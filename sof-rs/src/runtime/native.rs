use ahash::HashMap;
use flexstr::SharedStr;
use log::debug;
use miette::SourceSpan;

use super::Stackable;
use crate::error::Error;
use crate::runtime::Stack;

pub type NativeFunction1 = for<'gc> fn(Stackable<'gc>) -> Result<Option<Stackable<'gc>>, Error>;
pub type NativeFunction2 = for<'gc> fn(Stackable<'gc>, Stackable<'gc>) -> Result<Option<Stackable<'gc>>, Error>;
pub type NativeFunction3 =
	for<'gc> fn(Stackable<'gc>, Stackable<'gc>, Stackable<'gc>) -> Result<Option<Stackable<'gc>>, Error>;
pub type NativeFunction4 = for<'gc> fn(
	Stackable<'gc>,
	Stackable<'gc>,
	Stackable<'gc>,
	Stackable<'gc>,
) -> Result<Option<Stackable<'gc>>, Error>;
pub type NativeFunction5 = for<'gc> fn(
	Stackable<'gc>,
	Stackable<'gc>,
	Stackable<'gc>,
	Stackable<'gc>,
	Stackable<'gc>,
) -> Result<Option<Stackable<'gc>>, Error>;

#[derive(Debug, Copy, Clone)]
pub enum NativeFunction {
	Args1(NativeFunction1),
	Args2(NativeFunction2),
	Args3(NativeFunction3),
	Args4(NativeFunction4),
	Args5(NativeFunction5),
}

impl From<NativeFunction1> for NativeFunction {
	fn from(value: NativeFunction1) -> Self {
		Self::Args1(value)
	}
}
impl From<NativeFunction2> for NativeFunction {
	fn from(value: NativeFunction2) -> Self {
		Self::Args2(value)
	}
}
impl From<NativeFunction3> for NativeFunction {
	fn from(value: NativeFunction3) -> Self {
		Self::Args3(value)
	}
}
impl From<NativeFunction4> for NativeFunction {
	fn from(value: NativeFunction4) -> Self {
		Self::Args4(value)
	}
}
impl From<NativeFunction5> for NativeFunction {
	fn from(value: NativeFunction5) -> Self {
		Self::Args5(value)
	}
}

impl NativeFunction {
	pub fn call(&self, stack: &mut Stack<'_>, span: SourceSpan) -> Result<(), Error> {
		if let Some(result) = match self {
			NativeFunction::Args1(function) => {
				let argument = stack.pop(span)?;
				function(argument)
			},
			NativeFunction::Args2(function) => {
				let argument2 = stack.pop(span)?;
				let argument1 = stack.pop(span)?;
				function(argument1, argument2)
			},
			NativeFunction::Args3(function) => {
				let argument3 = stack.pop(span)?;
				let argument2 = stack.pop(span)?;
				let argument1 = stack.pop(span)?;
				function(argument1, argument2, argument3)
			},
			NativeFunction::Args4(function) => {
				let argument4 = stack.pop(span)?;
				let argument3 = stack.pop(span)?;
				let argument2 = stack.pop(span)?;
				let argument1 = stack.pop(span)?;
				function(argument1, argument2, argument3, argument4)
			},
			NativeFunction::Args5(function) => {
				let argument5 = stack.pop(span)?;
				let argument4 = stack.pop(span)?;
				let argument3 = stack.pop(span)?;
				let argument2 = stack.pop(span)?;
				let argument1 = stack.pop(span)?;
				function(argument1, argument2, argument3, argument4, argument5)
			},
		}? {
			stack.push(result);
		}
		Ok(())
	}
}

#[derive(Clone, Default)]
pub struct NativeFunctionRegistry {
	pub functions: HashMap<SharedStr, NativeFunction>,
}

impl NativeFunctionRegistry {
	pub fn register_function(&mut self, name: &str, function: impl Into<NativeFunction>) {
		self.functions.insert(SharedStr::from_ref(name), function.into());
	}

	#[inline]
	pub fn call_function(&self, name: &str, stack: &mut Stack<'_>, span: SourceSpan) -> Result<(), Error> {
		if let Some(function) = self.functions.get(name) {
			debug!("calling native function {name}: {function:?}");
			function.call(stack, span)
		} else {
			Err(Error::UnknownNativeFunction { name: name.into(), span })
		}
	}
}
