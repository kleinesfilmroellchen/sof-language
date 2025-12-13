use std::cmp::Ordering;
use std::fmt::{Debug, Display};
use std::ops::{Add, BitAnd, BitOr, BitXor, Div, Mul, Rem, Sub};
use std::sync::Arc;

use ahash::HashMap;
use gc_arena::lock::{GcRefLock, RefLock};
use gc_arena::{Collect, Gc, Mutation};
use lean_string::LeanString;
use log::debug;
use miette::SourceSpan;
use smallvec::SmallVec;

use super::Stack;
use super::interpreter::{ActionVec, CallReturnBehavior, InterpreterAction};
use super::list::List;
use super::nametable::{Nametable, NametableType};
use crate::error::Error;
use crate::identifier::Identifier;
use crate::token::{Command, Token};

#[derive(Clone, Debug, Collect)]
#[collect(no_drop)]
pub enum Stackable<'gc> {
	Integer(#[collect(require_static)] i64),
	Decimal(#[collect(require_static)] f64),
	Boolean(#[collect(require_static)] bool),
	// both pseudo-string-types are allocated off-GC-heap
	Identifier(#[collect(require_static)] Identifier),
	String(#[collect(require_static)] LeanString),
	CodeBlock(Gc<'gc, CodeBlock>),
	Function(Gc<'gc, Function<'gc>>),
	CurriedFunction(GcRefLock<'gc, CurriedFunction<'gc>>),
	BuiltinFunction {
		#[collect(require_static)]
		target_type: BuiltinType,
		#[collect(require_static)]
		id:          Identifier,
	},
	#[allow(unused)]
	Object(GcRefLock<'gc, Object<'gc>>),
	Nametable(GcRefLock<'gc, Nametable<'gc>>),
	ListStart,
	/// lists are immutable, therefore no lock here
	List(Gc<'gc, List<'gc>>),
	Curry,
}

pub type TokenVec = Arc<Vec<Token>>;

#[derive(Debug, Collect, PartialEq)]
#[collect(require_static)]
pub struct CodeBlock {
	pub(crate) code: TokenVec,
}

#[derive(Debug, Collect, PartialEq, Clone)]
#[collect(no_drop)]
pub struct Function<'gc> {
	arguments:                      usize,
	is_constructor:                 bool,
	#[collect(require_static)]
	code:                           TokenVec,
	global_nametable_at_definition: GcRefLock<'gc, Nametable<'gc>>,
}

impl<'gc> Function<'gc> {
	pub fn new(arguments: usize, code: TokenVec, current_global_nametable: GcRefLock<'gc, Nametable<'gc>>) -> Self {
		Self { arguments, is_constructor: false, code, global_nametable_at_definition: current_global_nametable }
	}
}

pub type CurriedArguments<'gc> = SmallVec<[Stackable<'gc>; 8]>;

#[derive(Debug, Clone)]
pub struct CurriedFunction<'gc> {
	curried_arguments: CurriedArguments<'gc>,
	function:          Gc<'gc, Function<'gc>>,
}

impl CurriedFunction<'_> {
	pub fn remaining_arguments(&self) -> usize {
		self.function.arguments - self.curried_arguments.len()
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

/// Call a builtin function on a builtin type, using the syntax
///
/// ```rust,no-run
/// call_builtin_function!(SomeBuiltin(id, span, stack, mc))
/// ```
#[macro_export]
macro_rules! call_builtin_function {
	($builtin_type:ident($id:expr, $span:expr, $stack:expr, $mc:expr)) => {{
		// automatically adds the .borrow() no-op method to any trivial type
		#[allow(unused_imports)]
		use std::borrow::Borrow;

		use $crate::error::Error;
		#[allow(unused)]
		use $crate::runtime::stackable::builtins::{Boolean, Decimal, Integer, String};

		let method = $builtin_type::get_builtin_method(&$id, $span)?;
		let value = $stack.pop($span)?;
		let Stackable::$builtin_type(ref inner) = value else {
			return Err(Error::InvalidType {
				operation: Command::Call,
				value:     value.to_string().into(),
				span:      $span,
			});
		};
		let result = method(&(*inner).borrow(), $stack, $mc, $span)?;
		$stack.push(value);
		if let Some(result) = result {
			$stack.push(result);
		}
		$crate::runtime::interpreter::no_action()
	}};
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
				stack.main.push(value);
				Ok(heapless::Vec::new())
			},
			Stackable::CodeBlock(codeblock) => Ok(heapless::Vec::from_array([InterpreterAction::ExecuteCall {
				code:            codeblock.code.clone(),
				return_behavior: CallReturnBehavior::BlockCall,
			}])),
			Stackable::Function(function) => {
				if let Some(curried_argument_count) = stack.next_currying_marker(function.arguments) {
					debug!(
						"found curry marker during function call of {}, will curry {curried_argument_count} arguments",
						Stackable::Function(*function)
					);
					// function is being curried
					let function_copy = *function;
					let arguments = stack.pop_n(curried_argument_count);
					let _ = stack.raw_pop();
					let curried_function =
						CurriedFunction { curried_arguments: arguments, function: function_copy };
					stack.push(Stackable::CurriedFunction(GcRefLock::new(mc, RefLock::new(curried_function))));
					Ok(heapless::Vec::new())
				} else {
					// TODO: try block
					(|| {
						if function.is_constructor {
							todo!("constructor functions");
						}
						// insert nametable below arguments
						let function_nametable =
							GcRefLock::new(mc, RefLock::new(Nametable::new(NametableType::Function)));
						stack.insert_nametable_at(function.arguments, function_nametable)?;
						// additionally insert function’s global nametable temporarily.
						// TOOD: only do this if function is from a module; shouldn’t impact performance much tho.
						stack.insert_function_specific_global_nametable(
							function.arguments + 1,
							function.global_nametable_at_definition,
						);
						Ok(heapless::Vec::from_array([InterpreterAction::ExecuteCall {
							code:            function.code.clone(),
							return_behavior: CallReturnBehavior::FunctionCall,
						}]))
					})()
					.map_err(|()| Error::NotEnoughArguments { argument_count: function.arguments, span })
				}
			},
			Stackable::CurriedFunction(curried_function) => {
				if let Some(curried_argument_count) =
					stack.next_currying_marker(curried_function.borrow().remaining_arguments())
				{
					// function is being curried (again)
					let arguments = stack.pop_n(curried_argument_count);
					let _ = stack.raw_pop();
					let mut mut_function = curried_function.borrow_mut(mc);
					mut_function.curried_arguments.insert_many(0, arguments);
					stack.push(Stackable::CurriedFunction(*curried_function));
					Ok(heapless::Vec::new())
				} else {
					let curried_function = curried_function.borrow();
					let function = curried_function.function;
					assert!(!function.is_constructor, "constructor not implemented");
					// push arguments to the stack
					for argument in &curried_function.curried_arguments {
						stack.push(argument.clone());
					}
					// insert nametable below arguments
					let function_nametable = GcRefLock::new(mc, RefLock::new(Nametable::new(NametableType::Function)));
					stack
						.insert_nametable_at(function.arguments, function_nametable)
						.map_err(|()| Error::NotEnoughArguments { argument_count: function.arguments, span })?;
					// additionally insert function’s global nametable temporarily.
					stack.insert_function_specific_global_nametable(
						function.arguments + 1,
						function.global_nametable_at_definition,
					);
					Ok(heapless::Vec::from_array([InterpreterAction::ExecuteCall {
						code:            function.code.clone(),
						return_behavior: CallReturnBehavior::FunctionCall,
					}]))
				}
			},
			Stackable::BuiltinFunction { target_type, id } => match target_type {
				BuiltinType::List => call_builtin_function!(List(id, span, stack, mc)),
				BuiltinType::Integer => call_builtin_function!(Integer(id, span, stack, mc)),
				BuiltinType::Decimal => call_builtin_function!(Decimal(id, span, stack, mc)),
				BuiltinType::Boolean => call_builtin_function!(Boolean(id, span, stack, mc)),
				BuiltinType::String => call_builtin_function!(String(id, span, stack, mc)),
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
			Stackable::String(string) => write!(f, "{string}"),
			Stackable::CodeBlock(cb) => write!(f, "[CodeBlock {}n ]", cb.code.len()),
			Stackable::Function(func) => {
				write!(f, "[Function/{} {}n ]", func.arguments, func.code.len())
			},
			Stackable::CurriedFunction(func) => {
				write!(
					f,
					"[CurriedFunction({})/{} {}n ]",
					func.borrow().curried_arguments.len(),
					func.borrow().function.arguments,
					func.borrow().function.code.len()
				)
			},
			Stackable::Object(_) => write!(f, "[Object]"),
			Stackable::Nametable(nt) => Display::fmt(&nt.borrow(), f),
			Stackable::ListStart => write!(f, "["),
			Stackable::Curry => write!(f, "|"),
			Stackable::List(list) => {
				write!(f, "[ ")?;
				for element in &list.list {
					write!(f, "{element}, ")?;
				}
				write!(f, " ]")
			},
			Stackable::BuiltinFunction { id, target_type } => write!(f, "[{target_type:?}.{id}]"),
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
			(Self::List(left), Self::List(right)) => left == right,
			(Self::ListStart, Self::ListStart) => true,
			_ => false,
		}
	}
}

/// All builtin types that have methods defined on them.
#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum BuiltinType {
	List,
	Integer,
	Decimal,
	Boolean,
	String,
}

/// Builtin method for a primitive type.
///
/// This type is generic over the GC arena lifetime (early-bound), but must accept any lifetime for the borrows of the
/// receiver and stack (late-bound).
#[allow(type_alias_bounds)]
pub type BuiltinMethod<This> = for<'a, 'b, 'c, 'gc> fn(
	&'a This,
	&'b mut Stack<'gc>,
	&'c Mutation<'gc>,
	SourceSpan,
) -> Result<Option<Stackable<'gc>>, Error>;

/// Registry for builtin methods belonging to a primitive type.
#[allow(type_alias_bounds)]
pub type BuiltinMethodRegistry<This> = HashMap<Identifier, BuiltinMethod<This>>;

pub trait RegistryExt<This> {
	fn get_builtin_method(&self, id: &Identifier, span: SourceSpan) -> Result<BuiltinMethod<This>, Error>;
}

impl<This> RegistryExt<This> for BuiltinMethodRegistry<This> {
	fn get_builtin_method(&self, id: &Identifier, span: SourceSpan) -> Result<BuiltinMethod<This>, Error> {
		self.get(id).ok_or_else(|| Error::UndefinedValue { name: id.clone(), span }).cloned()
	}
}

#[allow(non_snake_case)]
pub(crate) mod builtins {

	pub mod Decimal {
		use ahash::HashMapExt;

		use crate::runtime::stackable::{BuiltinMethod, BuiltinMethodRegistry, RegistryExt};
		use crate::{error, identifier};
		pub fn get_builtin_method(
			id: &identifier::Identifier,
			span: miette::SourceSpan,
		) -> Result<BuiltinMethod<f64>, error::Error> {
			static REGISTRY: std::sync::LazyLock<BuiltinMethodRegistry<f64>> =
				std::sync::LazyLock::new(|| BuiltinMethodRegistry::new());
			REGISTRY.get_builtin_method(id, span)
		}
	}
	pub mod Integer {
		use ahash::HashMapExt;

		use crate::identifier;
		use crate::runtime::stackable::{BuiltinMethod, BuiltinMethodRegistry, Error, RegistryExt};
		pub fn get_builtin_method(
			id: &identifier::Identifier,
			span: miette::SourceSpan,
		) -> Result<BuiltinMethod<i64>, Error> {
			static REGISTRY: std::sync::LazyLock<BuiltinMethodRegistry<i64>> =
				std::sync::LazyLock::new(|| BuiltinMethodRegistry::new());
			REGISTRY.get_builtin_method(id, span)
		}
	}
	pub mod Boolean {
		use ahash::HashMapExt;

		use crate::identifier;
		use crate::runtime::stackable::{BuiltinMethod, BuiltinMethodRegistry, Error, RegistryExt};
		pub fn get_builtin_method(
			id: &identifier::Identifier,
			span: miette::SourceSpan,
		) -> Result<BuiltinMethod<bool>, Error> {
			static REGISTRY: std::sync::LazyLock<BuiltinMethodRegistry<bool>> =
				std::sync::LazyLock::new(|| BuiltinMethodRegistry::new());
			REGISTRY.get_builtin_method(id, span)
		}
	}

	pub mod String {
		use ahash::HashMapExt;

		use crate::identifier;
		use crate::runtime::stackable::{BuiltinMethod, BuiltinMethodRegistry, Error, RegistryExt};
		pub fn get_builtin_method(
			id: &identifier::Identifier,
			span: miette::SourceSpan,
		) -> Result<BuiltinMethod<lean_string::LeanString>, Error> {
			static REGISTRY: std::sync::LazyLock<BuiltinMethodRegistry<lean_string::LeanString>> =
				std::sync::LazyLock::new(|| BuiltinMethodRegistry::new());
			REGISTRY.get_builtin_method(id, span)
		}
	}
}
