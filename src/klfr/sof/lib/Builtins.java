package klfr.sof.lib;

import klfr.sof.CompilerException;
import klfr.sof.lang.*;

/**
 * Container for most of the builtin functions, i.e. functions that are
 * available in the global namespace by default. These native operations are
 * bound to normal SOF functions in the preamble.sof file, the only SOF source
 * code file that is included in every execution of every program without
 * external control.
 */
public final class Builtins {

	////////////////////////////////////////////////////////////////////////////////////////////////
	//#region Randomness

	/**
	 * A small java.util.Random wrapper which makes the next(int) method visible.
	 */
	private static class Random extends java.util.Random {
		private static final long serialVersionUID = 1L;

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

	/** Find the most significant bit number of the number that is set. */
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
	 */
	public static IntPrimitive random(IntPrimitive from, IntPrimitive to) {
		final long start = from.value(), end = to.value(), range = end - start + 1;
		// to arrive at a suitable number with less RNG calls, determine the msb of the
		// range. This way, only rng values with these bits may be computed.
		int rangeMsb = mostSignificantSetBit(range) + 1;
		long rnumber = 0;
		do {
			rnumber = sofRandom.nextL(rangeMsb);
		} while (rnumber < 0 || rnumber >= range);
		return IntPrimitive.createIntPrimitive(rnumber + start);
	}

	/**
	 * Implements SOF's random:01 builtin function.
	 */
	public static FloatPrimitive random01() {
		return FloatPrimitive.createFloatPrimitive(sofRandom.nextDouble());
	}

	/**
	 * Implements SOF's random:float builtin function.
	 */
	public static FloatPrimitive random(FloatPrimitive _from, FloatPrimitive _to) {
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
	 */
	public static IntPrimitive convertInt(Stackable toConvert) {
		if (toConvert instanceof FloatPrimitive) {
			return IntPrimitive.createIntPrimitive(Math.round(((FloatPrimitive) toConvert).value()));
		} else if (toConvert instanceof StringPrimitive) {
			return IntPrimitive.createIntegerFromString(((StringPrimitive) toConvert).value());
		} else if (toConvert instanceof IntPrimitive)
			return (IntPrimitive) toConvert;

		throw new CompilerException.Incomplete("type");
	}

	/**
	 * Implements SOF's convert:float builtin function.
	 */
	public static FloatPrimitive convertFloat(Stackable toConvert) {
		if (toConvert instanceof StringPrimitive) {
			return FloatPrimitive.createFloatFromString(((StringPrimitive) toConvert).value());
		} else if (toConvert instanceof IntPrimitive) {
			return FloatPrimitive.createFloatPrimitive(((IntPrimitive) toConvert).value().doubleValue());
		} else if (toConvert instanceof FloatPrimitive)
			return (FloatPrimitive) toConvert;
		
		throw new CompilerException.Incomplete("type");
	}

	/**
	 * Implements SOF's convert:callable builtin function.
	 */
	public static Stackable convertCallable(Stackable toConvert) {
		if (toConvert instanceof CodeBlock || toConvert instanceof SOFunction) {
			return toConvert;
		} else {
			//TODO: Implement Church Numerals
		}
		
		throw new CompilerException.Incomplete("type");
	}

	/**
	 * Implements SOF's convert:string builtin function.
	 */
	public static StringPrimitive convertString(Stackable toConvert) {
		return StringPrimitive.createStringPrimitive(toConvert.print());
	}

	//#endregion Conversion

	////////////////////////////////////////////////////////////////////////////////////////////////
	//#region Math

	/**
	 * Implements SOF's abs function of the sof.math module.
	 */
	public static Stackable abs(Stackable a) {
		if (a instanceof FloatPrimitive)
			return FloatPrimitive.createFloatPrimitive(Math.abs(((FloatPrimitive) a).value()));
		else if (a instanceof IntPrimitive)
			return IntPrimitive.createIntPrimitive(Math.abs(((IntPrimitive) a).value()));

		throw new CompilerException.Incomplete("type");
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
