use std::sync::LazyLock;

use crate::runtime::native::{NativeFunction0, NativeFunction1, NativeFunction2, NativeFunctionRegistry};

mod preamble;

pub static DEFAULT_REGISTRY: LazyLock<NativeFunctionRegistry> = LazyLock::new(|| {
	let mut registry = NativeFunctionRegistry::default();
	registry.register_function("klfr.sof.lib.Builtins#convertInt(Stackable)", preamble::to_integer as NativeFunction1);
	registry.register_function("klfr.sof.lib.Builtins#convertFloat(Stackable)", preamble::to_float as NativeFunction1);
	registry
		.register_function("klfr.sof.lib.Builtins#convertString(Stackable)", preamble::to_string as NativeFunction1);
	registry.register_function("klfr.sof.lib.Builtins#random01()", preamble::random_float_01 as NativeFunction0);
	registry.register_function(
		"klfr.sof.lib.Builtins#random(FloatPrimitive,FloatPrimitive)",
		preamble::random_float_range as NativeFunction2,
	);
	registry.register_function(
		"klfr.sof.lib.Builtins#random(IntPrimitive,IntPrimitive)",
		preamble::random_int_range as NativeFunction2,
	);
	registry
});
