use gc_arena::lock::{GcRefLock, RefLock};
use gc_arena::{Arena, Collect, Mutation, Rootable};
use log::debug;
use miette::SourceSpan;

use self::nametable::Nametable;
use crate::error::Error;
use crate::identifier::Identifier;
use crate::runtime::nametable::NametableType;
use crate::runtime::stackable::CurriedArguments;
use crate::runtime::util::UtilityData;

pub mod interpreter;
pub mod nametable;
pub mod stackable;
mod util;

pub use stackable::Stackable;

/// Inner stack type that is the actual raw data structure of the stack.
pub type InnerStack<'gc> = Vec<Stackable<'gc>>;
/// Stack type and GC root.
#[derive(Debug, Collect)]
#[collect(no_drop)]
pub struct Stack<'gc> {
	main:        GcRefLock<'gc, InnerStack<'gc>>,
	/// Utility stack not visible for the program, currently only used by while loops.
	pub utility: GcRefLock<'gc, Vec<UtilityData<'gc>>>,

	top_nametable: usize,
}

impl<'gc> Stack<'gc> {
	pub fn new(mc: &Mutation<'gc>) -> Self {
		let mut me = Self {
			main:          GcRefLock::new(mc, RefLock::new(Vec::with_capacity(64))),
			utility:       GcRefLock::new(mc, RefLock::new(Vec::with_capacity(64))),
			top_nametable: 0,
		};

		me.push_nametable(mc, GcRefLock::new(mc, RefLock::new(Nametable::new(NametableType::Global))));
		me
	}

	pub fn top_nametable(&self) -> GcRefLock<'gc, Nametable<'gc>> {
		match self.main.borrow()[self.top_nametable] {
			Stackable::Nametable(nt) => nt,
			_ => unreachable!("missing nametable"),
		}
	}

	pub fn global_nametable(&self) -> GcRefLock<'gc, Nametable<'gc>> {
		match self.main.borrow()[0] {
			Stackable::Nametable(nt) => nt,
			_ => unreachable!("missing nametable"),
		}
	}

	pub fn lookup(&self, name: &Identifier, span: SourceSpan) -> Result<Stackable<'gc>, Error> {
		// fast path for top nametable
		match self.main.borrow()[self.top_nametable] {
			Stackable::Nametable(nt) => nt.borrow().lookup(name, span).ok(),
			_ => None,
		}
		.or_else(|| {
			self.main.borrow().iter().rev().find_map(|stackable| match stackable {
				Stackable::Nametable(nt) => nt.borrow().lookup(name, span).ok(),
				_ => None,
			})
		})
		.ok_or_else(|| Error::UndefinedValue { name: name.clone(), span })
	}

	/// Value must not be a nametable.
	pub fn push(&self, mc: &Mutation<'gc>, value: Stackable<'gc>) {
		self.main.borrow_mut(mc).push(value);
	}

	pub fn push_nametable(&mut self, mc: &Mutation<'gc>, nametable: GcRefLock<'gc, Nametable<'gc>>) {
		self.push(mc, Stackable::Nametable(nametable));
		self.top_nametable = self.main.borrow().len() - 1;
	}

	pub fn insert_nametable_at(
		&mut self,
		mc: &Mutation<'gc>,
		position: usize,
		nametable: GcRefLock<'gc, Nametable<'gc>>,
		span: SourceSpan,
	) -> Result<(), Error> {
		let stack = self.main.borrow();
		if position > stack.len() {
			return Err(Error::NotEnoughArguments { span, argument_count: position });
		}
		let insert_position = stack.len() - position;
		if stack[insert_position ..].iter().any(|v| matches!(v, Stackable::Nametable(_))) {
			return Err(Error::NotEnoughArguments { span, argument_count: position });
		}
		drop(stack);
		self.main.borrow_mut(mc).insert(insert_position, Stackable::Nametable(nametable));
		self.top_nametable = insert_position;
		Ok(())
	}

	/// Returns the next currying marker, if the marker is up to `max_arguments` places from the stack top. In other
	/// words, if a function with `max_arguments` arguments would be called on the stack currently, this function
	/// reports whether the function would be curried thanks to a currying marker (returning how many arguments are
	/// above the currying marker), or not (`None`).
	pub fn next_currying_marker(&self, max_arguments: usize) -> Option<usize> {
		let stack = self.main.borrow();
		if stack.len() < max_arguments {
			return None;
		}

		// Limit search to either max_arguments from the back, or the position of the top nametable.
		stack[(stack.len() - max_arguments).max(self.top_nametable + 1) ..]
			.iter()
			.rev()
			.enumerate()
			.find_map(|(i, v)| if matches!(v, Stackable::Curry) { Some(i) } else { None })
	}

	/// Performs a pop according to language rules.
	///
	/// - Nametables cannot be popped with this function and will return an error.
	/// - Currying markers are ignored and silently dropped.
	///
	/// If you need to pop nametables, use [`Self::pop_nametable`] instead.
	#[inline]
	pub fn pop(&self, mc: &Mutation<'gc>, span: SourceSpan) -> Result<Stackable<'gc>, Error> {
		let mut mut_stack = self.main.borrow_mut(mc);
		// SAFETY: We have one value at the start, and that is a nametable.
		//         Below, we check that we never pop nametables, so this one value will remain.
		let value = Self::unchecked_pop(&mut mut_stack);
		// TODO: tell the compiler that this is unlikely
		if matches!(value, Stackable::Curry) {
			debug!("ignoring curry marker on stack");
			drop(mut_stack);
			// some recursion for this slow path, might be tail-call optimized even
			self.pop(mc, span)
		} else if mut_stack.len() >= self.top_nametable {
			// fast path: did not reach top nametable, therefore cannot be a nametable
			Ok(value)
		} else if matches!(value, Stackable::Nametable(_)) {
			mut_stack.push(value);
			Err(Error::MissingValue { span })
		} else {
			Ok(value)
		}
	}

	/// Peeks the topmost value on the stack for debugging purposes. This does not follow language rules.
	pub fn raw_peek(&self) -> Option<Stackable<'gc>> {
		self.main.borrow().last().cloned()
	}

	/// Pops a value from the stack, disregarding language rules. This means that this function can pop nametables and
	/// curry markers. This function does not update the top nametable pointer and will leave the stack in an
	/// inconsistent state if used to pop a nametable. Use [`Self::pop_nametable`] instead.
	pub fn raw_pop(&mut self, mc: &Mutation<'gc>) -> Stackable<'gc> {
		self.main.borrow_mut(mc).pop().expect("missing nametable")
	}

	/// Caller must guarantee that `count` elements are available to pop, and that all of them are no nametables or
	/// currying markers.
	#[inline]
	pub fn pop_n(&self, mc: &Mutation<'gc>, count: usize) -> CurriedArguments<'gc> {
		let mut mut_stack = self.main.borrow_mut(mc);
		debug_assert!(mut_stack.len() >= count, "at least {count} elements must be available to pop");
		
		Self::unchecked_pop_n(&mut mut_stack, count)
	}

	/// Pops everything above and including the topmost nametable. Never pops the global nametable.
	pub fn pop_nametable(
		&mut self,
		mc: &Mutation<'gc>,
		span: SourceSpan,
	) -> Result<GcRefLock<'gc, Nametable<'gc>>, Error> {
		let mut mut_stack = self.main.borrow_mut(mc);
		loop {
			let top_value = Self::unchecked_pop(&mut mut_stack);
			if mut_stack.is_empty() {
				mut_stack.push(top_value);
				return Err(Error::MissingNametable { span });
			} else if let Stackable::Nametable(nt) = top_value {
				self.top_nametable = mut_stack
					.iter()
					.enumerate()
					.rev()
					.find_map(|(i, el)| match el {
						Stackable::Nametable(_) => Some(i),
						_ => None,
					})
					.unwrap_or(0);
				return Ok(nt);
			}
		}
	}

	/// Caller must guarantee that there is at least one element on the stack.
	#[inline(always)]
	fn unchecked_pop(stack: &mut InnerStack<'gc>) -> Stackable<'gc> {
		debug_assert!(!stack.is_empty());
		// SAFETY: Caller guarantees len() >= 1
		let value = unsafe { stack.get_unchecked(stack.len() - 1) }.clone();
		// SAFETY: Decreasing the length is always safe.
		unsafe { stack.set_len(stack.len() - 1) };
		value
	}

	/// Caller must guarantee that there is at least `count` elements on the stack.
	#[inline(always)]
	fn unchecked_pop_n(stack: &mut InnerStack<'gc>, count: usize) -> CurriedArguments<'gc> {
		debug_assert!(stack.len() >= count);
		// SAFETY: Caller guarantees len() >= count.
		let values = unsafe { stack.get_unchecked(stack.len() - count ..) }.iter().cloned().collect();
		// SAFETY: Decreasing the length is always safe.
		unsafe { stack.set_len(stack.len() - count) };
		values
	}
}

/// GC arena type for the SOF runtime, based on the stack root.
pub type StackArena = Arena<Rootable!(Stack<'_>)>;
