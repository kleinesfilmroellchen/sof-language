package klfr.sof.lib;

import java.util.logging.Logger;

import klfr.sof.exceptions.*;
import klfr.sof.lang.*;
import klfr.sof.lang.Stackable.DebugStringExtensiveness;
import klfr.sof.lang.functional.*;
import klfr.sof.lang.primitive.*;

/**
 * Container for most of the builtin functions, i.e. functions that are available in the global namespace by default.
 * These native operations are bound to normal SOF functions in the preamble.sof file, the only SOF source code file
 * that is included in every execution of every program without external control.
 */
@NativeFunctionCollection
public final class Builtins {

	private static final Logger log = Logger.getLogger(Builtins.class.getCanonicalName());

	// static {
	// 	System.out.println("Registering builtins...");
	// 	NativeFunctionRegistry.registerNativeFunctions(Builtins.class);
	// }

	////////////////////////////////////////////////////////////////////////////////////////////////
	//#region Randomness

	/**
	 * A small java.util.Random wrapper which makes the next(int) method visible.
	 */
	private static class Random extends java.util.Random {

		private static final long serialVersionUID = 1L;

		@Override
		public int next(int bits) {
			return super.next(bits);
		}

		/**
		 * A modified version of the int next(int) method that is suited for 64 random bits (long). It may return up to 64 bits
		 * of randomness. For this reason, it calls the next() method twice if more than 32 bits are requested.
		 * 
		 * @param bits The number of bits of randomness requested.
		 * @return A random long number with the lower {@code bits} random bits set.
		 */
		public long nextL(int bits) {
			return bits <= 32 ? super.next(bits) : (((long) (super.next(bits - 32)) << 32) + super.next(32));
		}
	}

	/** The RNG used by SOF's builtin random functions. */
	private static final Random sofRandom = new Random();

	/**
	 * Find the most significant bit number of the number that is set.
	 * 
	 * @param n The number to check.
	 * @return The (binary) position of the most significant bit that is set (1).
	 */
	public static int mostSignificantSetBit(long n) {
		if (n == 0)
			return 0;
		int msb = 0;
		while ((n & (0xffffffffffffffffl << ++msb)) != 0 && msb < 64)
			;
		return msb;
	}

	/**
	 * Implements SOF's random:int builtin function.
	 * 
	 * @param from The start value, inclusive.
	 * @param to   The end value, exclusive.
	 * @return A random number between start (inclusive) and end (exclusive).
	 */
	public static IntPrimitive random(final IntPrimitive from, final IntPrimitive to) {
		long start = from.value(), end = to.value();
		if (start > end) {
			final long tmp = start;
			start = end;
			end = tmp;
		}

		final long range = end - start + 1;
		// to arrive at a suitable number with less RNG calls, determine the msb of the
		// range. This way, only rng values with these bits may be computed.
		final int rangeMsb = mostSignificantSetBit(range) + 1;
		long rnumber = 0;
		do {
			rnumber = sofRandom.nextL(rangeMsb);
		} while (rnumber < 0 || rnumber >= range);
		return IntPrimitive.createIntPrimitive(rnumber + start);
	}

	/**
	 * Implements SOF's random:01 builtin function.
	 * 
	 * @return A random float between 0 (inclusive) and 1 (exclusive).
	 */
	public static FloatPrimitive random01() {
		return FloatPrimitive.createFloatPrimitive(sofRandom.nextDouble());
	}

	/**
	 * Implements SOF's random:float builtin function.
	 * 
	 * @param _from The start value, inclusive.
	 * @param _to   The end value, exclusive.
	 * @return A random number between start (inclusive) and end (exclusive).
	 */
	public static FloatPrimitive random(final FloatPrimitive _from, final FloatPrimitive _to) {
		final double from = _from.value(), to = _to.value(), interval = to - from;
		return FloatPrimitive.createFloatPrimitive(from + sofRandom.nextDouble() * interval);
	}

	//#endregion Randomness

	////////////////////////////////////////////////////////////////////////////////////////////////
	//#region Conversion

	/**
	 * Implements SOF's convert:int builtin function.
	 * 
	 * @param toConvert The value to convert to integer.
	 * @return An integer that was converted from the given value.
	 * @throws IncompleteCompilerException If the value cannot be converted.
	 */
	public static IntPrimitive convertInt(Stackable toConvert) throws IncompleteCompilerException {
		if (toConvert instanceof FloatPrimitive flt) {
			return IntPrimitive.createIntPrimitive(Math.round(flt.value()));
		} else if (toConvert instanceof StringPrimitive string) {
			return IntPrimitive.createIntegerFromString(string.value());
		} else if (toConvert instanceof IntPrimitive integer)
			return integer;
		else if (toConvert instanceof BoolPrimitive bool)
			return IntPrimitive.createIntPrimitive((long) (bool.value() ? 1 : 0));

		throw new IncompleteCompilerException("type");
	}

	/**
	 * Implements SOF's convert:float builtin function.
	 * 
	 * @param toConvert The value to convert to float.
	 * @return An float that was converted from the given value.
	 * @throws IncompleteCompilerException If the value cannot be converted.
	 */
	public static FloatPrimitive convertFloat(Stackable toConvert) throws IncompleteCompilerException {
		if (toConvert instanceof StringPrimitive string) {
			return FloatPrimitive.createFloatFromString(string.value());
		} else if (toConvert instanceof IntPrimitive integer) {
			return FloatPrimitive.createFloatPrimitive(integer.value().doubleValue());
		} else if (toConvert instanceof FloatPrimitive flt)
			return flt;
		else if (toConvert instanceof BoolPrimitive bool)
			return FloatPrimitive.createFloatPrimitive(bool.value() ? 1.0d : 0.0d);

		throw new IncompleteCompilerException("type");
	}

	/**
	 * Implements SOF's convert:callable builtin function.
	 * 
	 * @param toConvert The value to convert to callable.
	 * @return An callable that was converted from the given value.
	 * @throws IncompleteCompilerException If the value cannot be converted.
	 */
	public static Stackable convertCallable(Stackable toConvert) throws IncompleteCompilerException {
		if (toConvert instanceof CodeBlock || toConvert instanceof Function) {
			return toConvert;
		} else if (toConvert instanceof IntPrimitive integer) {
			return new ChurchNumeral(integer.value());
		}

		throw new IncompleteCompilerException("type");
	}

	/**
	 * Implements SOF's convert:string builtin function.
	 * 
	 * @param toConvert The value to convert to string.
	 * @return An string that was converted from the given value.
	 * @throws IncompleteCompilerException If the value cannot be converted.
	 */
	public static StringPrimitive convertString(Stackable toConvert) throws IncompleteCompilerException {
		return StringPrimitive.createStringPrimitive(toConvert.print());
	}

	//#endregion Conversion

	////////////////////////////////////////////////////////////////////////////////////////////////
	//#region Math

	@FunctionalInterface
	private static interface MathFunction1 {

		public Double calc(Double a) throws IncompleteCompilerException;
	}

	@FunctionalInterface
	private static interface MathFunction2 {

		public Double calc(Double a, Double b) throws IncompleteCompilerException;
	}

	/**
	 * Computes a single-argument math function and handles the surrounding type checks.
	 * 
	 * @param a         The argument to the math function, may be of any number type.
	 * @param func      The math function to be called.
	 * @param autoWiden Whether to automatically widen Integers to Floats (and not re-narrow). This depends on what sort of
	 *                     function is used. For real-valued functions such as the trigonometric sin, cos, tan, widening the
	 *                     type is expected and intended behavior. For other functions such as abs, the domain and range are
	 *                     identical, so the return type should be the same as the input type. If autoWiden is true, the
	 *                     return type is guaranteed to be FloatPrimitive.
	 * @return The result of computing the math function on the argument, with the specified type widening behavior.
	 * @throws IncompleteCompilerException If the math calculation failed, or if there is a type error.
	 */
	private static Stackable computeMathFunction1(Stackable a, MathFunction1 func, boolean autoWiden) throws IncompleteCompilerException {
		if (a instanceof FloatPrimitive aFloat)
			return FloatPrimitive.createFloatPrimitive(func.calc(aFloat.value()));
		else if (a instanceof IntPrimitive aInt) {
			final var doubleValue = func.calc(aInt.value().doubleValue());
			if (autoWiden) {
				return FloatPrimitive.createFloatPrimitive(doubleValue);
			}
			return IntPrimitive.createIntPrimitive(doubleValue.longValue());
		}
		throw new IncompleteCompilerException("type");
	}

	/**
	 * Computes a two-argument math function and handles the surrounding type checks.
	 * 
	 * @param a         The first argument to the math function, may be of any number type.
	 * @param a         The second argument to the math function, may be of any number type.
	 * @param func      The math function to be called.
	 * @param autoWiden Whether to automatically widen Integers to Floats (and not re-narrow). This depends on what sort of
	 *                     function is used. For real-valued functions such as the trigonometric sin, cos, tan, widening the
	 *                     type is expected and intended behavior. For other functions such as abs, the domain and range are
	 *                     identical, so the return type should be the same as the input type. If autoWiden is true, the
	 *                     return type is guaranteed to be FloatPrimitive.
	 * @return The result of computing the math function on the argument, with the specified type widening behavior.
	 * @throws IncompleteCompilerException If the math calculation failed, or if there is a type error.
	 */
	private static Stackable computeMathFunction2(Stackable a, Stackable b, MathFunction2 func, boolean autoWiden) throws IncompleteCompilerException {
		// use NaN as a signal here, as any sensible math function fails on NaN anyways
		var bFloat = Double.NaN;
		if (b instanceof FloatPrimitive b_) {
			bFloat = b_.value();
		} else if (b instanceof IntPrimitive b_) {
			bFloat = b_.value();
		}
		if (Double.isNaN(bFloat))
			return FloatPrimitive.NaN;

		if (a instanceof FloatPrimitive aFloat)
			return FloatPrimitive.createFloatPrimitive(func.calc(aFloat.value(), bFloat));
		else if (a instanceof IntPrimitive aInt) {
			final var doubleValue = func.calc(aInt.value().doubleValue(), bFloat);
			if (autoWiden || b instanceof FloatPrimitive) {
				return FloatPrimitive.createFloatPrimitive(doubleValue);
			}
			return IntPrimitive.createIntPrimitive(doubleValue.longValue());
		}
		throw new IncompleteCompilerException("type");
	}

	/**
	 * Implements SOF's abs function of the sof.math module.
	 * 
	 * @param a The number to compute the absolute value of.
	 * @return The absolute value of the argument.
	 * @throws IncompleteCompilerException If the value is not a number type.
	 */
	public static Stackable abs(Stackable a) throws IncompleteCompilerException {
		return computeMathFunction1(a, Math::abs, false);
	}

	/**
	 * Implements SOF's hypot function of the sof.math module. This calculates the hypotenuse of a right-angled triangle
	 * without overflow or underflow.
	 * 
	 * @param a The first value
	 * @param b The second value
	 * @return {@code sqrt(a^2 + b^2)} without underflow or overflow
	 * @throws IncompleteCompilerException If the types are not number types.
	 */
	public static Stackable hypot(Stackable a, Stackable b) throws IncompleteCompilerException {
		log.info(String.format("hypot types %s, %s", a.toDebugString(DebugStringExtensiveness.Compact), b.toDebugString(DebugStringExtensiveness.Compact)));
		return (FloatPrimitive) computeMathFunction2(a, b, Math::hypot, true);
	}

	/**
	 * Implements SOF's sin function of the sof.math module.
	 * 
	 * @param a The number to compute the sine of.
	 * @return The sine of the argument.
	 * @throws IncompleteCompilerException If the value is not a number type.
	 */
	public static FloatPrimitive sin(Stackable a) throws IncompleteCompilerException {
		return (FloatPrimitive) computeMathFunction1(a, Math::sin, true);
	}

	/**
	 * Implements SOF's cos function of the sof.math module.
	 * 
	 * @param a The number to compute the cosine of.
	 * @return The cosine of the argument.
	 * @throws IncompleteCompilerException If the value is not a number type.
	 */
	public static FloatPrimitive cos(Stackable a) throws IncompleteCompilerException {
		return (FloatPrimitive) computeMathFunction1(a, Math::cos, true);
	}

	/**
	 * Implements SOF's tan function of the sof.math module.
	 * 
	 * @param a The number to compute the tangent of.
	 * @return The tangent of the argument.
	 * @throws IncompleteCompilerException If the value is not a number type.
	 */
	public static FloatPrimitive tan(Stackable a) throws IncompleteCompilerException {
		return (FloatPrimitive) computeMathFunction1(a, Math::tan, true);
	}

	/**
	 * Implements SOF's exp (e^x) function of the sof.math module.
	 * 
	 * @param a The number to compute the exponent of.
	 * @return The exponent of the argument.
	 * @throws IncompleteCompilerException If the value is not a number type.
	 */
	public static FloatPrimitive exp(Stackable a) throws IncompleteCompilerException {
		return (FloatPrimitive) computeMathFunction1(a, Math::exp, true);
	}

	/**
	 * Implements SOF's ln (natural logarithm) function of the sof.math module.
	 * 
	 * @param a The number to compute the natural logarithm of.
	 * @return The natural logarithm of the argument.
	 * @throws IncompleteCompilerException If the value is not a number type.
	 */
	public static FloatPrimitive ln(Stackable a) throws IncompleteCompilerException {
		return (FloatPrimitive) computeMathFunction1(a, Math::log, true);
	}

	/**
	 * Implements SOF's log function of the sof.math module.
	 * 
	 * @param b The base of the logarithm.
	 * @param a The number to compute the logarithm of.
	 * @return The logarithm of a to the base b.
	 * @throws IncompleteCompilerException If the values are not a number type.
	 */
	public static FloatPrimitive log(Stackable b, Stackable a) throws IncompleteCompilerException {
		return (FloatPrimitive) computeMathFunction2(b, a, Builtins::log, true);
	}

	/**
	 * Computes the logarithm of a with the base b.<br>
	 * <br>
	 * The implementation makes use of the logarithmic law that {@code log_b(a) = log(a) / log(b)} where {@code log} can
	 * have any base. Here, the very precise {@code ln} (log base e) is used.
	 * 
	 * @param a The number of which to calculate the logarithm
	 * @param b The base of the logarithm
	 * @return mathematically {@code log_b(a)}.
	 */
	private static Double log(Double b, Double a) {
		return Math.log(a) / Math.log(b);
	}

	/**
	 * Implements SOF's sqrt function of the sof.math module.
	 * 
	 * @param a The element to be square rooted.
	 * @return The square root of the argument.
	 * @throws IncompleteCompilerException If the value is not a number type.
	 */
	public static Stackable sqrt(Stackable a) throws IncompleteCompilerException {
		return (FloatPrimitive) computeMathFunction1(a, Math::sqrt, true);
	}

	//#endregion Math
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
