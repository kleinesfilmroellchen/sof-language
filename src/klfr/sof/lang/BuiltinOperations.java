package klfr.sof.lang;

import klfr.sof.exceptions.IncompleteCompilerException;
import klfr.sof.lang.primitive.*;

/**
 * Built-in Operations is a collection for most PTs that implement operations, like arithmetic operations, comparisons,
 * stack operations.
 * 
 * @author klfr
 */
public final class BuiltinOperations {

	/**
	 * A simple functional interface that defines the structure of all multi-type capable binary operations.<br/>
	 * <br/>
	 * Multi-Type-Capable means that the operation must expect to recieve any SOF type (i.e. a generic Stackable for both
	 * parameters) and can return any SOF type, but it does not mean that the operation must be successful for any input
	 * type. The operation may always throw an {@link klfr.sof.exceptions.IncompleteCompilerException} if the types do not
	 * match up.
	 * 
	 * @see Stackable
	 * @see IncompleteCompilerException
	 */
	@FunctionalInterface
	public static interface BinaryOperation {

		/**
		 * Apply the binary operation to the given arguments.
		 * 
		 * @param a The left argument, lower on the stack.
		 * @param b The right argument, higher on the stack.
		 * @return The result of the binary operation with the arguments.
		 * @throws IncompleteCompilerException If the operation cannot be executed, e.g. the types are incorrect.
		 */
		public Stackable apply(Stackable a, Stackable b) throws IncompleteCompilerException;
	}

	/**
	 * Arbitrary-type arithmetic division in SOF. If at least one of the types is {@link FloatPrimitive}, the division is a
	 * floating-point division, if both are {@link IntPrimitive}, the division is an integer division.
	 * 
	 * @param a The dividend.
	 * @param b The divisor.
	 * @return {@code a / b}
	 * @throws IncompleteCompilerException If the types cannot be used in a division or if a divide by zero occurred.
	 */
	public static final Stackable divide(Stackable a, Stackable b) throws IncompleteCompilerException {
		try {
			if (a instanceof IntPrimitive ia && b instanceof IntPrimitive ib) {
				return ia.divide(ib);
			}
			if (a instanceof FloatPrimitive fa && b instanceof FloatPrimitive fb) {
				return fa.divide(fb);
			}
			if (a instanceof FloatPrimitive fa && b instanceof IntPrimitive ib) {
				return FloatPrimitive.createFloatPrimitive(fa.value() / ib.value());
			}
			if (b instanceof FloatPrimitive fb && a instanceof IntPrimitive ia) {
				return FloatPrimitive.createFloatPrimitive(ia.value() / fb.value());
			}
			throw new IncompleteCompilerException("type", "type.divide", a.typename(), b.typename());
		} catch (ArithmeticException e) {
			throw new IncompleteCompilerException("arithmetic", "div-by-zero");
		}
	}

	/**
	 * Arbitrary-type arithmetic modulus in SOF.<br/>
	 * <br/>
	 * The modulus is the remainder of the integer division between the arguments.<br/>
	 * For floating-point operands, the modulus is the remainder when determining how often the divisor exactly fits into
	 * the dividend. Or: The difference between the largest integer multiple of the divisor smaller than the dividend and
	 * the dividend itself.
	 * 
	 * @param a The dividend.
	 * @param b The divisor.
	 * @return {@code a mod b}, written in SOF with the {@code %} primitive token.
	 * @throws IncompleteCompilerException If the types cannot be used in a division or if a divide by zero occurred.
	 */
	public static final Stackable modulus(Stackable a, Stackable b) throws IncompleteCompilerException {
		try {
			if (a instanceof IntPrimitive ia && b instanceof IntPrimitive ib) {
				return ia.modulus(ib);
			}
			if (a instanceof FloatPrimitive fa && b instanceof FloatPrimitive fb) {
				return fa.modulus(fb);
			}
			if (a instanceof FloatPrimitive fa && b instanceof IntPrimitive ib) {
				return FloatPrimitive.createFloatPrimitive(fa.value() % ib.value());
			}
			if (b instanceof FloatPrimitive fb && a instanceof IntPrimitive ia) {
				return FloatPrimitive.createFloatPrimitive(ia.value() % fb.value());
			}
			throw new IncompleteCompilerException("type", "type.modulus", a.typename(), b.typename());
		} catch (ArithmeticException e) {
			throw new IncompleteCompilerException("arithmetic", "mod-by-zero");
		}
	}

	/**
	 * Arbitrary-type addition in SOF. If at least one operand is floating point, the result is promoted to floating point.
	 * 
	 * @param a The first summand.
	 * @param b The second summand.
	 * @return {@code a + b}
	 * @throws IncompleteCompilerException If the types cannot be used in an addition.
	 */
	public static final Stackable add(Stackable a, Stackable b) throws IncompleteCompilerException {
		if (a instanceof IntPrimitive ia && b instanceof IntPrimitive ib) {
			return ia.add(ib);
		}
		if (a instanceof FloatPrimitive fa && b instanceof FloatPrimitive fb) {
			return fa.add(fb);
		}
		if (a instanceof FloatPrimitive fa && b instanceof IntPrimitive ib) {
			return FloatPrimitive.createFloatPrimitive(fa.value() + ib.value());
		}
		if (b instanceof FloatPrimitive fb && a instanceof IntPrimitive ia) {
			return FloatPrimitive.createFloatPrimitive(fb.value() + ia.value());
		}
		throw new IncompleteCompilerException("type", "type.add", a.typename(), b.typename());
	}

	/**
	 * Arbitrary-type multiplication in SOF. If at least one operand is floating point, the result is promoted to floating
	 * point.
	 * 
	 * @param a The first factor.
	 * @param b The second factor.
	 * @return {@code a * b}
	 * @throws IncompleteCompilerException If the types cannot be used in a multiplication.
	 */
	public static final Stackable multiply(Stackable a, Stackable b) throws IncompleteCompilerException {
		if (a instanceof IntPrimitive ia && b instanceof IntPrimitive ib) {
			return ia.multiply(ib);
		}
		if (a instanceof FloatPrimitive fa && b instanceof FloatPrimitive fb) {
			return fa.multiply(fb);
		}
		if (a instanceof FloatPrimitive fa && b instanceof IntPrimitive ib) {
			return FloatPrimitive.createFloatPrimitive(fa.value() * ib.value());
		}
		if (b instanceof FloatPrimitive fb && a instanceof IntPrimitive ia) {
			return FloatPrimitive.createFloatPrimitive(fb.value() * ia.value());
		}
		throw new IncompleteCompilerException("type", "type.multiply", a.typename(), b.typename());
	}

	/**
	 * Arbitrary-type subtraction in SOF. If at least one operand is floating point, the result is promoted to floating
	 * point.
	 * 
	 * @param a The minuend.
	 * @param b The subtrahend.
	 * @return {@code a - b}
	 * @throws IncompleteCompilerException If the types cannot be used in a subtraction.
	 */
	public static final Stackable subtract(Stackable a, Stackable b) throws IncompleteCompilerException {
		if (a instanceof IntPrimitive ia && b instanceof IntPrimitive ib) {
			return ia.subtract(ib);
		}
		if (a instanceof FloatPrimitive fa && b instanceof FloatPrimitive fb) {
			return fa.subtract(fb);
		}
		if (a instanceof FloatPrimitive fa && b instanceof IntPrimitive ib) {
			return FloatPrimitive.createFloatPrimitive(fa.value() - ib.value());
		}
		if (b instanceof FloatPrimitive fb && a instanceof IntPrimitive ia) {
			return FloatPrimitive.createFloatPrimitive(ia.value() - fb.value());
		}
		throw new IncompleteCompilerException("type", "type.subtract", a.typename(), b.typename());
	}

	/**
	 * Arbitrary-type left bit shift in SOF. Both types are converted to integers beforehand.
	 * 
	 * @param a The number to be shifted.
	 * @param b The shift amount.
	 * @return {@code a << b}
	 * @throws IncompleteCompilerException If the types cannot be used in a bit shift.
	 */
	public static final Stackable bitShiftLeft(Stackable a, Stackable b) throws IncompleteCompilerException {
		Long ia = null;
		if (a instanceof IntPrimitive ia_) {
			ia = ia_.value();
		}
		Long ib = null;
		if (b instanceof IntPrimitive ib_) {
			ib = ib_.value();
		}
		if (ib == null || ia == null) {
			throw new IncompleteCompilerException("type", "type.bitshift", a.typename(), b.typename());
		}

		return IntPrimitive.createIntPrimitive(ia << ib);
	}

	/**
	 * Arbitrary-type right bit shift in SOF. Both types are converted to integers beforehand.
	 * 
	 * @param a The number to be shifted.
	 * @param b The shift amount.
	 * @return {@code a >> b}
	 * @throws IncompleteCompilerException If the types cannot be used in a bit shift.
	 */
	public static final Stackable bitShiftRight(Stackable a, Stackable b) throws IncompleteCompilerException {
		Long ia = null;
		if (a instanceof IntPrimitive ia_) {
			ia = ia_.value();
		}
		Long ib = null;
		if (b instanceof IntPrimitive ib_) {
			ib = ib_.value();
		}
		if (ib == null || ia == null) {
			throw new IncompleteCompilerException("type", "type.bitshift", a.typename(), b.typename());
		}

		return IntPrimitive.createIntPrimitive(ia >> ib);
	}

	/**
	 * Arbitrary-type logical AND (∧) in SOF.
	 * 
	 * @param a The first argument to the and.
	 * @param b The second argument to the and.
	 * @return The first argument, if both are truthy according to SOF logic, otherwise (SOF-) false.
	 */
	public static final Stackable logicalAnd(Stackable a, Stackable b) {
		if (a.isTrue() && b.isTrue())
			return a;
		return BoolPrimitive.createBoolPrimitive(false);
	}

	/**
	 * Arbitrary-type logical OR (∨) in SOF.
	 * 
	 * @param a The first argument to the or.
	 * @param b The second argument to the or.
	 * @return The first argument, if it is truthy according to SOF logic, otherwise the second argument.
	 */
	public static final Stackable logicalOr(Stackable a, Stackable b) {
		if (a.isTrue())
			return a;
		else
			return b;
	}

	/**
	 * Arbitrary-type logical exclusive OR (⨁) in SOF.
	 * 
	 * @param a The first argument to the xor.
	 * @param b The second argument to the xor.
	 * @return Whether the two arguments have a different truthiness.
	 */
	public static final Stackable logicalXor(Stackable a, Stackable b) {
		// use the convenient property of (a xor b) ≙ true only if they are different
		return BoolPrimitive.createBoolPrimitive(b.isTrue() != a.isTrue());
	}

	/**
	 * Compares the two arguments according to SOF logic and determines whether the left argument is strictly less than the
	 * right.
	 * 
	 * @param a The left argument.
	 * @param b The right argument.
	 * @return Whether the first, left argument is strictly less than the second, right.
	 * @throws IncompleteCompilerException If the two types are not comparable.
	 */
	public static final Stackable lessThan(Stackable a, Stackable b) throws IncompleteCompilerException {
		try {
			return BoolPrimitive.createBoolPrimitive(a.compareTo(b) < 0);
		} catch (RuntimeException e) {
			throw (IncompleteCompilerException) e.getCause();
		}
	}

	/**
	 * Compares the two arguments according to SOF logic and determines whether the left argument is strictly greater than
	 * the right.
	 * 
	 * @param a The left argument.
	 * @param b The right argument.
	 * @return Whether the first, left argument is strictly greater than the second, right.
	 * @throws IncompleteCompilerException If the two types are not comparable.
	 */
	public static final Stackable greaterThan(Stackable a, Stackable b) throws IncompleteCompilerException {
		try {
			return BoolPrimitive.createBoolPrimitive(a.compareTo(b) > 0);
		} catch (RuntimeException e) {
			throw (IncompleteCompilerException) e.getCause();
		}
	}

	/**
	 * Compares the two arguments according to SOF logic and determines whether the left argument is greater or equal than
	 * the right.
	 * 
	 * @param a The left argument.
	 * @param b The right argument.
	 * @return Whether the first, left argument is greater or equal than the second, right.
	 * @throws IncompleteCompilerException If the two types are not comparable.
	 */
	public static final Stackable greaterEqualThan(Stackable a, Stackable b) throws IncompleteCompilerException {
		try {
			return BoolPrimitive.createBoolPrimitive(a.compareTo(b) >= 0);
		} catch (RuntimeException e) {
			throw (IncompleteCompilerException) e.getCause();
		}
	}

	/**
	 * Compares the two arguments according to SOF logic and determines whether the left argument is less or equal than the
	 * right.
	 * 
	 * @param a The left argument.
	 * @param b The right argument.
	 * @return Whether the first, left argument is less or equal than the second, right.
	 * @throws IncompleteCompilerException If the two types are not comparable.
	 */
	public static final Stackable lessEqualThan(Stackable a, Stackable b) throws IncompleteCompilerException {
		try {
			return BoolPrimitive.createBoolPrimitive(a.compareTo(b) <= 0);
		} catch (RuntimeException e) {
			throw (IncompleteCompilerException) e.getCause();
		}
	}

	/**
	 * Determines whether the two arguments are equal according to SOF logic.
	 * 
	 * @param a The first argument.
	 * @param b The second argument.
	 * @return (SOF) {@code true} if the two arguments are equal as determined by SOF logic, otherwise (SOF) {@code false}.
	 */
	public static final Stackable equals(Stackable a, Stackable b) {
		return BoolPrimitive.createBoolPrimitive(a.equals(b));
	}

	/**
	 * Determines whether the two arguments are not equal according to SOF logic.
	 * 
	 * @param a The first argument.
	 * @param b The second argument.
	 * @return (SOF) {@code false} if the two arguments are equal as determined by SOF logic, otherwise (SOF) {@code true}.
	 */
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
