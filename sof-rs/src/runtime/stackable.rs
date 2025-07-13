use std::cmp::Ordering;
use std::fmt::Display;
use std::ops::{Add, BitAnd, BitOr, BitXor, Div, Mul, Rem, Sub};
use std::sync::Arc;

use flexstr::SharedStr;
use gc_arena::lock::{GcRefLock, RefLock};
use gc_arena::{Collect, Mutation};
use log::debug;
use miette::SourceSpan;
use smallvec::{SmallVec, smallvec};

use super::Stack;
use super::interpreter::{ActionVec, CallReturnBehavior, InterpreterAction};
use super::nametable::{Nametable, NametableType};
use crate::error::Error;
use crate::identifier::Identifier;
use crate::token::{Command, Token};

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
	#[allow(unused)]
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

impl CurriedFunction<'_> {
	pub fn remaining_arguments(&self) -> usize {
		self.function.borrow().arguments - self.curried_arguments.len()
	}
}

// Required as SmallVec does not implement Collect, but contains garbage-collected data so require_static is impossible.
// SAFETY: We trace both `function` as well as every value in `curried_arguments` (via iterator).
unsafe impl Collect for CurriedFunction<'_> {
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

macro_rules! numeric_op {
	($vis:vis $func_name:ident($command:ident, $func:ident)) => {
		$vis fn $func_name(
			&self,
			other: &Stackable<'gc>,
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

	pub fn divide(&self, other: &Stackable<'gc>, span: SourceSpan) -> Result<Stackable<'gc>, Error> {
		if matches!(other, Stackable::Integer(0) | Stackable::Decimal(0.0)) {
			Err(Error::DivideByZero { lhs: self.to_string().into(), rhs: other.to_string().into(), span })
		} else {
			self.divide_unchecked(other, span)
		}
	}

	pub fn modulus(&self, other: &Stackable<'gc>, span: SourceSpan) -> Result<Stackable<'gc>, Error> {
		if matches!(other, Stackable::Integer(0) | Stackable::Decimal(0.0)) {
			Err(Error::DivideByZero { lhs: self.to_string().into(), rhs: other.to_string().into(), span })
		} else {
			self.modulus_unchecked(other, span)
		}
	}

	pub fn shift_left(&self, other: &Stackable<'gc>, span: SourceSpan) -> Result<Stackable<'gc>, Error> {
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

	pub fn shift_right(&self, other: &Stackable<'gc>, span: SourceSpan) -> Result<Stackable<'gc>, Error> {
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

	pub fn compare(&self, other: &Stackable<'gc>, operation: Command, span: SourceSpan) -> Result<Ordering, Error> {
		if self.eq(other) {
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
				let value = stack.lookup(identifier, span)?;
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
						Stackable::Function(*function)
					);
					// function is being curried
					let function_copy = *function;
					let arguments = stack.pop_n(mc, curried_argument_count);
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
					let arguments = stack.pop_n(mc, curried_argument_count);
					let _ = stack.raw_pop(mc);
					let mut mut_function = curried_function.borrow_mut(mc);
					mut_function.curried_arguments.insert_many(0, arguments);
					stack.push(mc, Stackable::CurriedFunction(*curried_function));
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

impl Display for Stackable<'_> {
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

impl PartialEq for Stackable<'_> {
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
