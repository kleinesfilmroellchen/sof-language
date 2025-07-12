use std::cmp::Ordering;
use std::fmt::Display;
use std::ops::{Add, BitAnd, BitOr, BitXor, Div, Mul, Rem, Sub};
use std::sync::Arc;

use flexstr::SharedStr;
use gc_arena::lock::{GcRefLock, RefLock};
use gc_arena::{Arena, Collect, Mutation, Rootable};
use log::debug;
use miette::SourceSpan;
use smallvec::{SmallVec, smallvec};

use self::nametable::Nametable;
use crate::error::Error;
use crate::identifier::Identifier;
use crate::interpreter::{ActionVec, CallReturnBehavior, InterpreterAction};
use crate::parser::{Command, Token};
use crate::runtime::nametable::NametableType;

pub mod nametable;

#[derive(Debug, Collect, Clone)]
#[collect(no_drop)]
pub enum Stackable<'gc> {
	Integer(i64),
	Decimal(f64),
	Boolean(bool),
	// both pseudo-string-types are allocated off-GC-heap
	Identifier(#[collect(require_static)] Identifier),
	String(#[collect(require_static)] SharedStr),
	CodeBlock(GcRefLock<'gc, CodeBlock>),
	Function(GcRefLock<'gc, Function>),
	CurriedFunction(GcRefLock<'gc, CurriedFunction<'gc>>),
	Object(GcRefLock<'gc, Object<'gc>>),
	Nametable(GcRefLock<'gc, Nametable<'gc>>),
	ListStart,
	Curry,
}

pub type TokenVec = Arc<Vec<Token>>;

#[derive(Debug, Collect, PartialEq)]
#[collect(require_static)]
pub struct CodeBlock {
	pub(crate) code: TokenVec,
}

#[derive(Debug, Collect, PartialEq, Clone)]
#[collect(require_static)]
pub struct Function {
	arguments:      usize,
	is_constructor: bool,
	code:           TokenVec,
}

impl Function {
	pub fn new(arguments: usize, code: TokenVec) -> Self {
		Self { arguments, is_constructor: false, code }
	}
}

pub type CurriedArguments<'gc> = SmallVec<[Stackable<'gc>; 8]>;

#[derive(Debug, Clone)]
pub struct CurriedFunction<'gc> {
	curried_arguments: CurriedArguments<'gc>,
	function:          GcRefLock<'gc, Function>,
}

impl<'gc> CurriedFunction<'gc> {
	pub fn remaining_arguments(&self) -> usize {
		self.function.borrow().arguments - self.curried_arguments.len()
	}
}

// Required as SmallVec does not implement Collect, but contains garbage-collected data so require_static is impossible.
// SAFETY: We trace both `function` as well as every value in `curried_arguments` (via iterator).
unsafe impl<'gc> Collect for CurriedFunction<'gc> {
	fn needs_trace() -> bool
	where
		Self: Sized,
	{
		true
	}

	fn trace(&self, cc: &gc_arena::Collection) {
		self.function.trace(cc);
		for element in &self.curried_arguments {
			element.trace(cc);
		}
	}
}

#[derive(Debug, Collect, PartialEq)]
#[collect(no_drop)]
pub struct Object<'gc> {
	fields: Nametable<'gc>,
}

/// Inner stack type that is the actual raw data structure of the stack.
pub type InnerStack<'gc> = Vec<Stackable<'gc>>;
/// Stack type and GC root.
#[derive(Debug, Collect)]
#[collect(no_drop)]
pub struct Stack<'gc> {
	main:        GcRefLock<'gc, InnerStack<'gc>>,
	/// Utility stack not visible for the program, currently only used by while loops.
	pub utility: GcRefLock<'gc, InnerStack<'gc>>,

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

	pub fn lookup(&self, name: Identifier, span: SourceSpan) -> Result<Stackable<'gc>, Error> {
		// fast path for top nametable
		match self.main.borrow()[self.top_nametable] {
			Stackable::Nametable(nt) => nt.borrow().lookup(name.clone(), span).ok(),
			_ => None,
		}
		.or_else(|| {
			self.main.borrow().iter().rev().find_map(|stackable| match stackable {
				Stackable::Nametable(nt) => nt.borrow().lookup(name.clone(), span).ok(),
				_ => None,
			})
		})
		.ok_or(Error::UndefinedValue { name, span })
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
	pub fn pop_n(&self, mc: &Mutation<'gc>, count: usize) -> Result<CurriedArguments<'gc>, Error> {
		let mut mut_stack = self.main.borrow_mut(mc);
		debug_assert!(mut_stack.len() >= count, "at least {count} elements must be available to pop");
		let values = Self::unchecked_pop_n(&mut mut_stack, count);
		Ok(values)
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
		debug_assert!(stack.len() > 0);
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

macro_rules! numeric_op {
    ($vis:vis $func_name:ident($command:ident, $func:ident)) => {
        $vis fn $func_name(
            &self,
            other: Stackable<'gc>,
            span: SourceSpan,
        ) -> Result<Stackable<'gc>, Error> {
            Ok(match (self, &other) {
                (Stackable::Integer(lhs), Stackable::Integer(rhs)) => Stackable::Integer(lhs.$func(rhs)),
                (Stackable::Integer(lhs), Stackable::Decimal(rhs)) => {
                    Stackable::Decimal((*lhs as f64).$func(rhs))
                }
                (Stackable::Decimal(lhs), Stackable::Integer(rhs)) => {
                    Stackable::Decimal(lhs.$func(*rhs as f64))
                }
                (Stackable::Decimal(lhs), Stackable::Decimal(rhs)) => Stackable::Decimal(lhs.$func(rhs)),
                _ => {
                    return Err(Error::InvalidTypes {
                        operation: Command::$command,
                        lhs: self.to_string().into(),
                        rhs: other.to_string().into(),
                        span,
                    });
                }
            })
        }
    };
}

macro_rules! logic_op {
    ($vis:vis $func_name:ident($command:ident, $func:ident)) => {
        $vis fn $func_name(
            &self,
            other: Stackable<'gc>,
            span: SourceSpan,
        ) -> Result<Stackable<'gc>, Error> {
            match (self, &other) {
                (Stackable::Boolean(lhs), Stackable::Boolean(rhs)) => Ok(Stackable::Boolean(lhs.$func(rhs))),
                _ => Err(Error::InvalidTypes {
                    operation: Command::$command,
                    lhs: self.to_string().into(),
                    rhs: other.to_string().into(),
                    span,
                })
            }
        }
    };
}

impl<'gc> Stackable<'gc> {
	numeric_op!(pub add(Plus, add));

	numeric_op!(pub subtract(Minus, sub));

	numeric_op!(pub multiply(Multiply, mul));

	numeric_op!(divide_unchecked(Divide, div));

	numeric_op!(modulus_unchecked(Modulus, rem));

	logic_op!(pub and(And, bitand));

	logic_op!(pub or(Or, bitor));

	logic_op!(pub xor(Xor, bitxor));

	pub fn divide(&self, other: Stackable<'gc>, span: SourceSpan) -> Result<Stackable<'gc>, Error> {
		if matches!(other, Stackable::Integer(0) | Stackable::Decimal(0.0)) {
			Err(Error::DivideByZero { lhs: self.to_string().into(), rhs: other.to_string().into(), span })
		} else {
			self.divide_unchecked(other, span)
		}
	}

	pub fn modulus(&self, other: Stackable<'gc>, span: SourceSpan) -> Result<Stackable<'gc>, Error> {
		if matches!(other, Stackable::Integer(0) | Stackable::Decimal(0.0)) {
			Err(Error::DivideByZero { lhs: self.to_string().into(), rhs: other.to_string().into(), span })
		} else {
			self.modulus_unchecked(other, span)
		}
	}

	pub fn shift_left(&self, other: Stackable<'gc>, span: SourceSpan) -> Result<Stackable<'gc>, Error> {
		match (self, &other) {
			(Stackable::Integer(lhs), Stackable::Integer(rhs)) =>
				Ok(Stackable::Integer(lhs.unbounded_shl((*rhs & 0xffff_ffff) as u32))),
			_ => Err(Error::InvalidTypes {
				operation: Command::LeftShift,
				lhs: self.to_string().into(),
				rhs: other.to_string().into(),
				span,
			}),
		}
	}

	pub fn shift_right(&self, other: Stackable<'gc>, span: SourceSpan) -> Result<Stackable<'gc>, Error> {
		match (self, &other) {
			(Stackable::Integer(lhs), Stackable::Integer(rhs)) =>
				Ok(Stackable::Integer(lhs.unbounded_shr((*rhs & 0xffff_ffff) as u32))),
			_ => Err(Error::InvalidTypes {
				operation: Command::RightShift,
				lhs: self.to_string().into(),
				rhs: other.to_string().into(),
				span,
			}),
		}
	}

	pub fn negate(&self, span: SourceSpan) -> Result<Stackable<'gc>, Error> {
		match self {
			Stackable::Boolean(value) => Ok(Stackable::Boolean(!value)),
			_ => Err(Error::InvalidType { operation: Command::Not, value: self.to_string().into(), span }),
		}
	}

	pub fn compare(&self, other: Stackable<'gc>, operation: Command, span: SourceSpan) -> Result<Ordering, Error> {
		if self.eq(&other) {
			Ok(Ordering::Equal)
		} else {
			match (self, &other) {
				(Stackable::Integer(lhs), Stackable::Integer(rhs)) => Ok(lhs.cmp(rhs)),
				(Stackable::Integer(lhs), Stackable::Decimal(rhs)) => (*lhs as f64).partial_cmp(rhs).ok_or_else(|| {
					Error::Incomparable { lhs: self.to_string().into(), rhs: other.to_string().into(), span }
				}),
				(Stackable::Decimal(lhs), Stackable::Integer(rhs)) =>
					lhs.partial_cmp(&(*rhs as f64)).ok_or_else(|| Error::Incomparable {
						lhs: self.to_string().into(),
						rhs: other.to_string().into(),
						span,
					}),
				(Stackable::Decimal(lhs), Stackable::Decimal(rhs)) => lhs.partial_cmp(rhs).ok_or_else(|| {
					Error::Incomparable { lhs: self.to_string().into(), rhs: other.to_string().into(), span }
				}),
				_ => Err(Error::InvalidTypes {
					operation,
					lhs: self.to_string().into(),
					rhs: other.to_string().into(),
					span,
				}),
			}
		}
	}

	/// Starts a call sequence of a Callable.
	/// Returns the next interpreter action, since calling usually involves nonstandard actions.
	pub fn enter_call(&self, mc: &Mutation<'gc>, stack: &mut Stack<'gc>, span: SourceSpan) -> Result<ActionVec, Error> {
		match self {
			Stackable::Identifier(identifier) => {
				let value = stack.lookup(identifier.clone(), span)?;
				stack.main.borrow_mut(mc).push(value);
				Ok(smallvec![])
			},
			Stackable::CodeBlock(codeblock) => Ok(smallvec![InterpreterAction::ExecuteCall {
				code:            codeblock.borrow().code.clone(),
				return_behavior: CallReturnBehavior::BlockCall,
			}]),
			Stackable::Function(function) => {
				if let Some(curried_argument_count) = stack.next_currying_marker(function.borrow().arguments) {
					debug!(
						"found curry marker during function call of {}, will curry {curried_argument_count} arguments",
						Stackable::Function(function.clone())
					);
					// function is being curried
					let function_copy = function.clone();
					let arguments = stack.pop_n(mc, curried_argument_count)?;
					let _ = stack.raw_pop(mc);
					let curried_function =
						CurriedFunction { curried_arguments: arguments, function: function_copy };
					stack.push(mc, Stackable::CurriedFunction(GcRefLock::new(mc, RefLock::new(curried_function))));
					Ok(smallvec![])
				} else {
					let function = function.borrow();
					assert!(!function.is_constructor, "constructor not implemented");
					// insert nametable below arguments
					let function_nametable = GcRefLock::new(mc, RefLock::new(Nametable::new(NametableType::Function)));
					stack.insert_nametable_at(mc, function.arguments, function_nametable, span)?;
					Ok(smallvec![InterpreterAction::ExecuteCall {
						code:            function.code.clone(),
						return_behavior: CallReturnBehavior::FunctionCall,
					}])
				}
			},
			Stackable::CurriedFunction(curried_function) => {
				if let Some(curried_argument_count) =
					stack.next_currying_marker(curried_function.borrow().remaining_arguments())
				{
					// function is being curried (again)
					let arguments = stack.pop_n(mc, curried_argument_count)?;
					let _ = stack.raw_pop(mc);
					let mut mut_function = curried_function.borrow_mut(mc);
					mut_function.curried_arguments.insert_many(0, arguments);
					stack.push(mc, Stackable::CurriedFunction(curried_function.clone()));
					Ok(smallvec![])
				} else {
					let curried_function = curried_function.borrow();
					let function = curried_function.function.borrow();
					assert!(!function.is_constructor, "constructor not implemented");
					// push arguments to the stack
					for argument in &curried_function.curried_arguments {
						stack.push(mc, argument.clone());
					}
					// insert nametable below arguments
					let function_nametable = GcRefLock::new(mc, RefLock::new(Nametable::new(NametableType::Function)));
					stack.insert_nametable_at(mc, function.arguments, function_nametable, span)?;
					Ok(smallvec![InterpreterAction::ExecuteCall {
						code:            function.code.clone(),
						return_behavior: CallReturnBehavior::FunctionCall,
					}])
				}
			},
			_ => Err(Error::InvalidType { operation: Command::Call, value: self.to_string().into(), span }),
		}
	}
}

impl<'gc> Display for Stackable<'gc> {
	fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
		match self {
			Stackable::Integer(int) => write!(f, "{int}"),
			Stackable::Decimal(dec) => write!(f, "{dec}"),
			Stackable::Boolean(boolean) => write!(f, "{boolean}"),
			Stackable::Identifier(identifier) => write!(f, "{identifier}"),
			Stackable::String(string) => write!(f, "\"{string}\""),
			Stackable::CodeBlock(cb) => write!(f, "[CodeBlock {}n ]", cb.borrow().code.len()),
			Stackable::Function(func) => {
				write!(f, "[Function/{} {}n ]", func.borrow().arguments, func.borrow().code.len())
			},
			Stackable::CurriedFunction(func) => {
				write!(
					f,
					"[CurriedFunction({})/{} {}n ]",
					func.borrow().curried_arguments.len(),
					func.borrow().function.borrow().arguments,
					func.borrow().function.borrow().code.len()
				)
			},
			Stackable::Object(_) => write!(f, "[Object]"),
			Stackable::Nametable(nt) => write!(f, "NT[{}]", nt.borrow().entries.len()),
			Stackable::ListStart => write!(f, "["),
			Stackable::Curry => write!(f, "|"),
		}
	}
}

impl<'gc> PartialEq for Stackable<'gc> {
	fn eq(&self, other: &Self) -> bool {
		match (self, other) {
			(Self::Integer(l0), Self::Integer(r0)) => l0 == r0,
			(Self::Integer(l0), Self::Decimal(r0)) => *l0 as f64 == *r0,
			(Self::Decimal(l0), Self::Decimal(r0)) => l0 == r0,
			(Self::Decimal(l0), Self::Integer(r0)) => *l0 == *r0 as f64,
			(Self::Boolean(l0), Self::Boolean(r0)) => l0 == r0,
			(Self::Identifier(l0), Self::Identifier(r0)) => l0 == r0,
			(Self::String(l0), Self::String(r0)) => l0 == r0,
			(Self::CodeBlock(l0), Self::CodeBlock(r0)) => l0 == r0,
			(Self::Function(l0), Self::Function(r0)) => l0 == r0,
			(Self::Object(l0), Self::Object(r0)) => l0 == r0,
			(Self::Nametable(l0), Self::Nametable(r0)) => l0 == r0,
			(Self::ListStart, Self::ListStart) => true,
			_ => false,
		}
	}
}
