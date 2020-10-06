package klfr.sof.lang;

import klfr.sof.CompilerException;

/**
 * Built-in Primitive Tokens is a collection for most PTs, like arithmetic
 * operations, comparisons, stack operations.
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
		if (a.isTrue() && b.isTrue())
			return a;
		return BoolPrimitive.createBoolPrimitive(false);
	}

	public static final Stackable logicalOr(Stackable a, Stackable b) {
		// doesn't care about types, this one time run-time binding/virtual methods come
		// into play thanks java
		if (a.isTrue())
			return a;
		else if (b.isTrue())
			return b;
		return BoolPrimitive.createBoolPrimitive(false);
	}

	public static final Stackable logicalXor(Stackable a, Stackable b) {
		// use the convenient property of (a xor b) == true only if they are different
		return BoolPrimitive.createBoolPrimitive(b.isTrue() != a.isTrue());
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

/*  
The SOF programming language interpreter.
Copyright (C) 2019-2020  kleinesfilmröllchen

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
