use std::cell::LazyCell;

use crate::runtime::native::{NativeFunction1, NativeFunctionRegistry};

mod preamble;

pub const DEFAULT_REGISTRY: LazyCell<NativeFunctionRegistry> = LazyCell::new(|| {
	let mut registry = NativeFunctionRegistry::default();
	registry.register_function("klfr.sof.lib.Builtins#convertInt(Stackable)", preamble::to_integer as NativeFunction1);
	registry.register_function("klfr.sof.lib.Builtins#convertFloat(Stackable)", preamble::to_float as NativeFunction1);
	registry
		.register_function("klfr.sof.lib.Builtins#convertString(Stackable)", preamble::to_string as NativeFunction1);
	registry
});
