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
public final class BuiltinPTs {

	public static final Stackable divide(Stackable a, Stackable b) {
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
			throw new CompilerException.Incomplete("type", "type.divide", a.typename(), b.typename());
		} catch (ArithmeticException e) {
			throw new CompilerException.Incomplete("arithmetic", "div-by-zero");
		}
	}

	public static final Stackable add(Stackable a, Stackable b) {
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
		throw new CompilerException.Incomplete("type", "type.add", a.typename(), b.typename());
	}

	public static final Stackable multiply(Stackable a, Stackable b) {
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
		throw new CompilerException.Incomplete("type", "type.multiply", a.typename(), b.typename());
	}

	public static final Stackable subtract(Stackable a, Stackable b) {
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
		throw new CompilerException.Incomplete("type", "type.subtract", a.typename(), b.typename());
	}

	public static final Stackable logicalAnd(Stackable a, Stackable b) {
		// make 'and' more efficient by only checking type of first operand, and
		// returning second operand if it is true
		if (a instanceof BoolPrimitive && ((BoolPrimitive) a).isTrue()) {
			return b;
		}
		throw new CompilerException.Incomplete("type", "type.and", a.typename(), b.typename());
	}

	public static final Stackable logicalOr(Stackable a, Stackable b) {
		// make 'or' more efficient by only checking a if possible, and also return the
		// value that was considered true
		if (a instanceof BoolPrimitive && ((BoolPrimitive) a).isTrue()) {
			return a;
		} else if (b instanceof BoolPrimitive && ((BoolPrimitive) b).isTrue()) {
			return b;
		}
		throw new CompilerException.Incomplete("type", "type.or", a.typename(), b.typename());
	}

	public static final Stackable logicalXor(Stackable a, Stackable b) {
		// two typechecks necessary, use the convenient property of (a xor b) == true
		// only if they are different
		if (a instanceof BoolPrimitive && b instanceof BoolPrimitive) {
			return BoolPrimitive.createBoolPrimitive(((BoolPrimitive) b).value() != ((BoolPrimitive) a).value());
		}
		throw new CompilerException.Incomplete("type", "type.xor", a.typename(), b.typename());
	}

	public static final Stackable lessThan(Stackable a, Stackable b) {
		return BoolPrimitive.createBoolPrimitive(a.compareTo(b) < 0);
	}

	public static final Stackable greaterThan(Stackable a, Stackable b) {
		return BoolPrimitive.createBoolPrimitive(a.compareTo(b) > 0);
	}

	public static final Stackable greaterEqualThan(Stackable a, Stackable b) {
		return BoolPrimitive.createBoolPrimitive(a.compareTo(b) >= 0);
	}

	public static final Stackable lessEqualThan(Stackable a, Stackable b) {
		return BoolPrimitive.createBoolPrimitive(a.compareTo(b) <= 0);
	}

	public static final Stackable equals(Stackable a, Stackable b) {
		return BoolPrimitive.createBoolPrimitive(a.equals(b));
	}

	public static final Stackable notEquals(Stackable a, Stackable b) {
		return BoolPrimitive.createBoolPrimitive(!a.equals(b));
	}

}
