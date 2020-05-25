package klfr.sof.lang;

import klfr.sof.CompilerException;

/**
 * Built-in Primitive Tokens is a collection for most PTs, like arithmetic
 * operations, comparisons, stack operations. This collection makes heavy use of
 * the {@link Callable#fromFunction(Function)} methods to define callables
 * directly from lambda functions.
 * 
 * @author klfr
 */
@SuppressWarnings("unchecked")
public final class BuiltinPTs {

	public static final Callable divide = Callable.fromFunction((a, b) -> {
		try {
			if (a instanceof IntPrimitive && b instanceof IntPrimitive) {
				return ((IntPrimitive) a).divide((IntPrimitive) b);
			}
			if (a instanceof FloatPrimitive && b instanceof FloatPrimitive) {
				return ((FloatPrimitive) a).divide((FloatPrimitive) b);
			}
			if (a instanceof FloatPrimitive && b instanceof IntPrimitive) {
				return FloatPrimitive.createFloatPrimitive(((FloatPrimitive) a).value() / ((IntPrimitive) b).value());
			}
			if (b instanceof FloatPrimitive && a instanceof IntPrimitive) {
				return FloatPrimitive.createFloatPrimitive(((IntPrimitive) a).value() / ((FloatPrimitive) b).value());
			}
			throw CompilerException.makeIncomplete("Type",
					String.format("Cannot divide types %s and %s.", a.typename(), b.typename()));
		} catch (ArithmeticException e) {
			throw CompilerException.makeIncomplete("Arithmetic", "Divide by zero.");
		}
	});

	public static final Callable add = Callable.fromFunction((a, b) -> {
		if (a instanceof IntPrimitive && b instanceof IntPrimitive) {
			return ((IntPrimitive) a).add((IntPrimitive) b);
		}
		if (a instanceof FloatPrimitive && b instanceof FloatPrimitive) {
			return ((FloatPrimitive) a).add((FloatPrimitive) b);
		}
		if (a instanceof FloatPrimitive && b instanceof IntPrimitive) {
			return FloatPrimitive.createFloatPrimitive(((FloatPrimitive) a).value() + ((IntPrimitive) b).value());
		}
		if (b instanceof FloatPrimitive && a instanceof IntPrimitive) {
			return FloatPrimitive.createFloatPrimitive(((FloatPrimitive) b).value() + ((IntPrimitive) a).value());
		}
		throw CompilerException.makeIncomplete("Type",
				String.format("Cannot add types %s and %s.", a.typename(), b.typename()));
	});

	public static final Callable multiply = Callable.fromFunction((a, b) -> {
		if (a instanceof IntPrimitive && b instanceof IntPrimitive) {
			return ((IntPrimitive) a).multiply((IntPrimitive) b);
		}
		if (a instanceof FloatPrimitive && b instanceof FloatPrimitive) {
			return ((FloatPrimitive) a).multiply((FloatPrimitive) b);
		}
		if (a instanceof FloatPrimitive && b instanceof IntPrimitive) {
			return FloatPrimitive.createFloatPrimitive(((FloatPrimitive) a).value() * ((IntPrimitive) b).value());
		}
		if (b instanceof FloatPrimitive && a instanceof IntPrimitive) {
			return FloatPrimitive.createFloatPrimitive(((FloatPrimitive) b).value() * ((IntPrimitive) a).value());
		}
		throw CompilerException.makeIncomplete("Type",
				String.format("Cannot multiply types %s and %s.", a.typename(), b.typename()));
	});

	public static final Callable subtract = Callable.fromFunction((a, b) -> {
		if (a instanceof IntPrimitive && b instanceof IntPrimitive) {
			return ((IntPrimitive) a).subtract((IntPrimitive) b);
		}
		if (a instanceof FloatPrimitive && b instanceof FloatPrimitive) {
			return ((FloatPrimitive) a).subtract((FloatPrimitive) b);
		}
		if (a instanceof FloatPrimitive && b instanceof IntPrimitive) {
			return FloatPrimitive.createFloatPrimitive(((FloatPrimitive) a).value() - ((IntPrimitive) b).value());
		}
		if (b instanceof FloatPrimitive && a instanceof IntPrimitive) {
			return FloatPrimitive.createFloatPrimitive(((IntPrimitive) a).value() - ((FloatPrimitive) b).value());
		}
		throw CompilerException.makeIncomplete("Type",
				String.format("Cannot subtract types %s and %s.", a.typename(), b.typename()));
	});

	public static final Callable lessThan = Callable.fromFunction((a, b) -> BoolPrimitive.createBoolPrimitive(a.compareTo(b) < 0));

	public static final Callable greaterThan = Callable.fromFunction((a, b) -> BoolPrimitive.createBoolPrimitive(a.compareTo(b) > 0));

	public static final Callable greaterEqualThan = Callable.fromFunction((a, b) -> BoolPrimitive.createBoolPrimitive(a.compareTo(b) >= 0));

	public static final Callable lessEqualThan = Callable.fromFunction((a, b) -> BoolPrimitive.createBoolPrimitive(a.compareTo(b) <= 0));
}
