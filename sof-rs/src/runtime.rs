use std::fmt::Debug;

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
pub mod list;
mod module;
pub mod nametable;
pub mod native;
pub mod stackable;
mod util;

pub use stackable::Stackable;

/// Inner stack type that is the actual raw data structure of the stack.
pub type InnerStack<'gc> = Vec<Stackable<'gc>>;
/// Stack type and GC root.
#[derive(Collect)]
#[collect(no_drop)]
pub struct Stack<'gc> {
	main:        InnerStack<'gc>,
	/// Utility stack not visible for the program, currently only used by while loops.
	pub utility: Vec<UtilityData<'gc>>,

	/// Index into stack where the top nametable lives. This is largely an optimization.
	top_nametable:    usize,
	module_nametable: usize,
}

impl<'gc> Stack<'gc> {
	pub fn new(mc: &Mutation<'gc>) -> Self {
		let mut me = Self {
			main:             Vec::with_capacity(64),
			utility:          Vec::with_capacity(64),
			top_nametable:    0,
			module_nametable: 0,
		};

		me.push_nametable(GcRefLock::new(mc, RefLock::new(Nametable::new(NametableType::Global))));
		me
	}

	pub fn top_nametable(&self) -> GcRefLock<'gc, Nametable<'gc>> {
		match self.main[self.top_nametable] {
			Stackable::Nametable(nt) => nt,
			_ => unreachable!("missing nametable"),
		}
	}

	/// Returns the global nametable in the root module, and the module nametable otherwise.
	pub fn global_nametable(&self) -> GcRefLock<'gc, Nametable<'gc>> {
		match self.main[self.module_nametable] {
			Stackable::Nametable(nt) => nt,
			_ => unreachable!("missing nametable"),
		}
	}

	pub fn lookup(&self, name: &Identifier, span: SourceSpan) -> Result<Stackable<'gc>, Error> {
		// fast path for top nametable
		match self.main[self.top_nametable] {
			Stackable::Nametable(nt) => nt.borrow().lookup(name, span).ok(),
			_ => None,
		}
		.or_else(|| {
			self.main.iter().rev().find_map(|stackable| match stackable {
				Stackable::Nametable(nt) => nt.borrow().lookup(name, span).ok(),
				_ => None,
			})
		})
		.ok_or_else(|| Error::UndefinedValue { name: name.clone(), span })
	}

	/// Value must not be a nametable.
	pub fn push(&mut self, value: Stackable<'gc>) {
		self.main.push(value);
	}

	pub fn push_nametable(&mut self, nametable: GcRefLock<'gc, Nametable<'gc>>) {
		self.push(Stackable::Nametable(nametable));
		self.top_nametable = self.main.len() - 1;
	}

	pub fn push_module_nametable(&mut self, nametable: GcRefLock<'gc, Nametable<'gc>>) {
		debug_assert!(nametable.borrow().kind == NametableType::Module);
		self.push_nametable(nametable);
		self.module_nametable = self.top_nametable;
	}

	/// Inserts the nametable below a certain number of elements on top of the stack, especially to set a function
	/// nametable before a call.
	///
	/// `allow_below_other_nametable` optionally allows to insert the new nametable below any other nametable (i.e. at
	/// any position in the stack), which is not the normal function-call behavior.
	pub fn insert_nametable_at(
		&mut self,
		position: usize,
		nametable: GcRefLock<'gc, Nametable<'gc>>,
	) -> Result<(), ()> {
		if position > self.main.len() {
			return Err(());
		}
		let insert_position = self.main.len() - position;
		if self.main[insert_position ..].iter().any(|v| matches!(v, Stackable::Nametable(_))) {
			return Err(());
		}

		self.main.insert(insert_position, Stackable::Nametable(nametable));
		self.top_nametable = insert_position;
		Ok(())
	}

	pub fn insert_function_specific_global_nametable(
		&mut self,
		position: usize,
		global_nametable: GcRefLock<'gc, Nametable<'gc>>,
	) {
		if position > self.main.len() {
			panic!("invalid position for function global nt");
		}
		debug_assert!([NametableType::Module, NametableType::Global].contains(&global_nametable.borrow().kind));

		let insert_position = self.main.len() - position;
		// shift top nametable pointer if necessary
		if self.top_nametable >= insert_position {
			self.top_nametable += 1;
		}

		self.main.insert(insert_position, Stackable::Nametable(global_nametable));
		self.module_nametable = insert_position;
	}

	/// Returns the next currying marker, if the marker is up to `max_arguments` places from the stack top. In other
	/// words, if a function with `max_arguments` arguments would be called on the stack currently, this function
	/// reports whether the function would be curried thanks to a currying marker (returning how many arguments are
	/// above the currying marker), or not (`None`).
	pub fn next_currying_marker(&self, max_arguments: usize) -> Option<usize> {
		if self.main.len() < max_arguments {
			return None;
		}

		// Limit search to either max_arguments from the back, or the position of the top nametable.
		self.main[(self.main.len() - max_arguments).max(self.top_nametable + 1) ..]
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
	pub fn pop(&mut self, span: SourceSpan) -> Result<Stackable<'gc>, Error> {
		// SAFETY: We have one value at the start, and that is a nametable.
		//         Below, we check that we never pop nametables, so this one value will remain.
		let value = Self::unchecked_pop(&mut self.main);
		match value {
			Stackable::Curry => {
				debug!("ignoring curry marker on stack");
				// some recursion for this slow path, might be tail-call optimized even
				self.pop(span)
			},
			Stackable::Nametable(_) => {
				self.main.push(value);
				Err(Error::MissingValue { span })
			},
			_ if self.main.len() >= self.top_nametable => {
				// fast path: did not reach top nametable, therefore cannot be a nametable
				Ok(value)
			},
			_ => Ok(value),
		}
	}

	/// Peeks the topmost value on the stack for debugging purposes. This does not follow language rules.
	pub fn raw_peek(&self) -> Option<Stackable<'gc>> {
		self.main.last().cloned()
	}

	/// Pops a value from the stack, disregarding language rules. This means that this function can pop nametables and
	/// curry markers. This function does not update the top nametable pointer and will leave the stack in an
	/// inconsistent state if used to pop a nametable. Use [`Self::pop_nametable`] instead.
	pub fn raw_pop(&mut self) -> Stackable<'gc> {
		self.main.pop().expect("missing nametable")
	}

	/// Caller must guarantee that `count` elements are available to pop, and that all of them are no nametables or
	/// currying markers.
	#[inline]
	pub fn pop_n(&mut self, count: usize) -> CurriedArguments<'gc> {
		debug_assert!(self.main.len() >= count, "at least {count} elements must be available to pop");

		Self::unchecked_pop_n(&mut self.main, count)
	}

	/// Pops everything above and including the topmost nametable. Never pops the global nametable.
	pub fn pop_nametable(&mut self, span: SourceSpan) -> Result<GcRefLock<'gc, Nametable<'gc>>, Error> {
		loop {
			let top_value = Self::unchecked_pop(&mut self.main);
			if self.main.is_empty() {
				self.main.push(top_value);
				return Err(Error::MissingNametable { span });
			} else if let Stackable::Nametable(nt) = top_value {
				self.top_nametable = self
					.main
					.iter()
					.enumerate()
					.rev()
					.find_map(|(i, el)| match el {
						Stackable::Nametable(_) => Some(i),
						_ => None,
					})
					.unwrap_or(0);
				// Only relevant for restarting after some failures: make sure the module nametable index is not OOB
				self.module_nametable = self.top_nametable.min(self.module_nametable);
				return Ok(nt);
			}
		}
	}

	/// Pops everything above and including the module nametable. Never pops the global nametable.
	pub fn pop_module_nametable(&mut self, span: SourceSpan) -> Result<GcRefLock<'gc, Nametable<'gc>>, Error> {
		loop {
			let next_nametable = self.pop_nametable(span)?;
			if next_nametable.borrow().kind == NametableType::Module {
				self.module_nametable = self
					.main
					.iter()
					.enumerate()
					.rev()
					.find_map(|(i, el)| match el {
						Stackable::Nametable(nt) if nt.borrow().kind == NametableType::Module => Some(i),
						_ => None,
					})
					.unwrap_or(0);

				return Ok(next_nametable);
			}
		}
	}

	/// Caller must guarantee that there is at least one element on the stack.
	#[inline]
	fn unchecked_pop(stack: &mut InnerStack<'gc>) -> Stackable<'gc> {
		debug_assert!(!stack.is_empty());
		// Drop correctness is guaranteed as the pointer read takes ownership without moving and set_len discards the
		// value without Drop. SAFETY: Caller guarantees len() >= 1
		let value = unsafe { stack.as_mut_ptr().add(stack.len() - 1).read() };
		// SAFETY: Decreasing the length is always safe as we had >= 1 element.
		unsafe { stack.set_len(stack.len() - 1) };
		value
	}

	/// Caller must guarantee that there is at least `count` elements on the stack.
	#[inline]
	fn unchecked_pop_n(stack: &mut InnerStack<'gc>, count: usize) -> CurriedArguments<'gc> {
		debug_assert!(stack.len() >= count);
		// SAFETY: Caller guarantees len() >= count.
		let values = unsafe { stack.get_unchecked(stack.len() - count ..) }.iter().cloned().collect();
		// SAFETY: Decreasing the length is always safe as we had >= count element.
		unsafe { stack.set_len(stack.len() - count) };
		values
	}
}

impl<'gc> Debug for Stack<'gc> {
	fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
		write!(f, "Stack {{ ")?;
		if f.alternate() {
			write!(f, "\n")?;
		}
		for (i, s) in self.main.iter().enumerate().rev() {
			write!(
				f,
				"\n\t{}{}{}",
				s,
				if i == self.top_nametable { " (top)" } else { "" },
				if i == self.module_nametable { " (module)" } else { "" }
			)?;
		}
		write!(f, "\n\t---\n")?;
		if f.alternate() {
			write!(f, "util: {:#?}\n", self.utility)?;
		} else {
			write!(f, "util: {:?}", self.utility)?;
		}
		write!(f, "}}")
	}
}

/// GC arena type for the SOF runtime, based on the stack root.
pub type StackArena = Arena<Rootable!(Stack<'_>)>;
