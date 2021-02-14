package klfr.sof.lib;

import klfr.sof.exceptions.*;
import klfr.sof.lang.*;
import klfr.sof.lang.functional.*;
import klfr.sof.lang.primitive.*;

/**
 * Container for most of the builtin functions, i.e. functions that are
 * available in the global namespace by default. These native operations are
 * bound to normal SOF functions in the preamble.sof file, the only SOF source
 * code file that is included in every execution of every program without
 * external control.
 */
@NativeFunctionCollection
public final class Builtins {

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
		 * A modified version of the int next(int) method that is suited for 64 random
		 * bits (long). It may return up to 64 bits of randomness. For this reason, it
		 * calls the next() method twice if more than 32 bits are requested.
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
	 * @param to The end value, exclusive.
	 * @return A random number between start (inclusive) and end (exclusive).
	 */
	public static IntPrimitive random(final IntPrimitive from, final IntPrimitive to) {
		final long start = from.value(), end = to.value(), range = end - start + 1;
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
	 * @return A random float between 0 (inclusive) and 1 (exclusive).
	 */
	public static FloatPrimitive random01() {
		return FloatPrimitive.createFloatPrimitive(sofRandom.nextDouble());
	}

	/**
	 * Implements SOF's random:float builtin function.
	 * 
	 * @param _from The start value, inclusive.
	 * @param _to The end value, exclusive.
	 * @return A random number between start (inclusive) and end (exclusive).
	 */
	public static FloatPrimitive random(final FloatPrimitive _from, final FloatPrimitive _to) {
		final double from = _from.value(), to = _to.value(),
						interval = to - from;
		return FloatPrimitive.createFloatPrimitive(
			from + sofRandom.nextDouble() * interval
		);
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
		if (toConvert instanceof FloatPrimitive) {
			return IntPrimitive.createIntPrimitive(Math.round(((FloatPrimitive) toConvert).value()));
		} else if (toConvert instanceof StringPrimitive) {
			return IntPrimitive.createIntegerFromString(((StringPrimitive) toConvert).value());
		} else if (toConvert instanceof IntPrimitive)
			return (IntPrimitive) toConvert;

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
		if (toConvert instanceof StringPrimitive) {
			return FloatPrimitive.createFloatFromString(((StringPrimitive) toConvert).value());
		} else if (toConvert instanceof IntPrimitive) {
			return FloatPrimitive.createFloatPrimitive(((IntPrimitive) toConvert).value().doubleValue());
		} else if (toConvert instanceof FloatPrimitive)
			return (FloatPrimitive) toConvert;
		
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
		if (toConvert instanceof CodeBlock || toConvert instanceof SOFunction) {
			return toConvert;
		} else {
			//TODO: Implement Church Numerals
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

	/**
	 * Implements SOF's abs function of the sof.math module.
	 * 
	 * @param a The number to compute the absolute value of.
	 * @return The absolute value of the argument.
	 * @throws IncompleteCompilerException If the value is not a number type.
	 */
	public static Stackable abs(Stackable a) throws IncompleteCompilerException {
		if (a instanceof FloatPrimitive)
			return FloatPrimitive.createFloatPrimitive(Math.abs(((FloatPrimitive) a).value()));
		else if (a instanceof IntPrimitive)
			return IntPrimitive.createIntPrimitive(Math.abs(((IntPrimitive) a).value()));

		throw new IncompleteCompilerException("type");
	}

	/**
	 * Implements SOF's sin function of the sof.math module.
	 * 
	 * @param a The number to compute the sine of.
	 * @return The sine of the argument.
	 * @throws IncompleteCompilerException If the value is not a number type.
	 */
	public static FloatPrimitive sin(Stackable a) throws IncompleteCompilerException {
		if (a instanceof FloatPrimitive)
			return FloatPrimitive.createFloatPrimitive(Math.sin(((FloatPrimitive)a).value()));
		if (a instanceof IntPrimitive)
			return FloatPrimitive.createFloatPrimitive(Math.sin(((IntPrimitive)a).value()));
		throw new IncompleteCompilerException("type");
	}

	/**
	 * Implements SOF's cos function of the sof.math module.
	 * 
	 * @param a The number to compute the cosine of.
	 * @return The cosine of the argument.
	 * @throws IncompleteCompilerException If the value is not a number type.
	 */
	public static FloatPrimitive cos(Stackable a) throws IncompleteCompilerException {
		if (a instanceof FloatPrimitive)
			return FloatPrimitive.createFloatPrimitive(Math.cos(((FloatPrimitive)a).value()));
		if (a instanceof IntPrimitive)
			return FloatPrimitive.createFloatPrimitive(Math.cos(((IntPrimitive)a).value()));
		throw new IncompleteCompilerException("type");
	}

	/**
	 * Implements SOF's tan function of the sof.math module.
	 * 
	 * @param a The number to compute the tangent of.
	 * @return The tangent of the argument.
	 * @throws IncompleteCompilerException If the value is not a number type.
	 */
	public static FloatPrimitive tan(Stackable a) throws IncompleteCompilerException {
		if (a instanceof FloatPrimitive)
			return FloatPrimitive.createFloatPrimitive(Math.tan(((FloatPrimitive)a).value()));
		if (a instanceof IntPrimitive)
			return FloatPrimitive.createFloatPrimitive(Math.tan(((IntPrimitive)a).value()));
		throw new IncompleteCompilerException("type");
	}

	/**
	 * Implements SOF's exp (e^x) function of the sof.math module.
	 * 
	 * @param a The number to compute the exponent of.
	 * @return The exponent of the argument.
	 * @throws IncompleteCompilerException If the value is not a number type.
	 */
	public static FloatPrimitive exp(Stackable a) throws IncompleteCompilerException {
		if (a instanceof FloatPrimitive)
			return FloatPrimitive.createFloatPrimitive(Math.exp(((FloatPrimitive)a).value()));
		if (a instanceof IntPrimitive)
			return FloatPrimitive.createFloatPrimitive(Math.exp(((IntPrimitive)a).value()));
		throw new IncompleteCompilerException("type");
	}

	/**
	 * Implements SOF's ln (natural logarithm) function of the sof.math module.
	 * 
	 * @param a The number to compute the natural logarithm of.
	 * @return The natural logarithm of the argument.
	 * @throws IncompleteCompilerException If the value is not a number type.
	 */
	public static FloatPrimitive ln(Stackable a) throws IncompleteCompilerException {
		if (a instanceof FloatPrimitive)
			return FloatPrimitive.createFloatPrimitive(Math.log(((FloatPrimitive)a).value()));
		if (a instanceof IntPrimitive)
			return FloatPrimitive.createFloatPrimitive(Math.log(((IntPrimitive)a).value()));
		throw new IncompleteCompilerException("type");
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
		var base = (b instanceof FloatPrimitive) ? ((FloatPrimitive)b).value() : (b instanceof IntPrimitive) ? ((IntPrimitive)b).value() : Double.NaN;
		if (Double.isNaN(base))
			throw new IncompleteCompilerException("type");

		var logb = Math.log(base);

		// log_b(a) = ln(a) / ln(b)
		if (a instanceof FloatPrimitive)
			return FloatPrimitive.createFloatPrimitive(Math.log(((FloatPrimitive)a).value()) / logb);
		if (a instanceof IntPrimitive)
			return FloatPrimitive.createFloatPrimitive(Math.log(((IntPrimitive)a).value()) / logb);
		throw new IncompleteCompilerException("type");
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
