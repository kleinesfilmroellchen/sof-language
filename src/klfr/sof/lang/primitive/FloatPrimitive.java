package klfr.sof.lang.primitive;

import java.util.logging.Logger;

import klfr.sof.Patterns;
import klfr.sof.exceptions.IncompleteCompilerException;
import klfr.sof.lang.*;

/**
 * floating point decimal primitive type
 */
@StackableName("Float")
public final class FloatPrimitive extends Primitive {

	private static final long		serialVersionUID		= 1L;
	private static final Logger	log						= Logger.getLogger(FloatPrimitive.class.getCanonicalName());
	/**
	 * Because this float implementation uses double as its basis, equality comparisons for many common ratios will be
	 * imprecise. For this reason, equality testing between two sof floats is done after rounding to this specified number
	 * of decimal places. As doubles have about 12 significant decimal places, a number closely below this may be ideal to
	 * preserve accuracy of comparisons.
	 */
	public static final int			EQUALITY_PRECISION	= 10;
	/**
	 * The Not A Number (NaN) constant for float primitives.
	 */
	public static final Stackable	NaN						= FloatPrimitive.createFloatPrimitive(Double.NaN);

	/** The double that is represented by this primitive. */
	private final Double				v;

	private FloatPrimitive(double d) {
		this.v = d;
	}

	@Override
	public Object v() {
		return v;
	}

	/**
	 * Returns the value represented by this primitive.
	 * 
	 * @return the value represented by this primitive.
	 */
	public final Double value() {
		return v;
	}

	/**
	 * Creates a new float primitive from the given double.
	 * 
	 * @param d The double to create a primitive from.
	 * @return A new float primitive with the value of the given double.
	 */
	public static FloatPrimitive createFloatPrimitive(Double d) {
		return new FloatPrimitive(d);
	}

	/**
	 * Returns a float primitive from the given stackable.
	 * <ul>
	 * <li>If the stackable is already a float primitive, it is returned.</li>
	 * <li>If the stackable is an integer primitive, the integer is trivially converted.</li>
	 * <li>If the stackable is any other data type, a runtime error is thrown.</li>
	 * </ul>
	 * This method is intended to only be used if the caller knows that the data is of a number type.
	 * 
	 * @param s The stackable to be converted.
	 * @return The converted stackable.
	 */
	public static FloatPrimitive fromStackable(Stackable s) {
		if (s instanceof FloatPrimitive f) {
			return f;
		}
		if (s instanceof IntPrimitive i) {
			return FloatPrimitive.createFloatPrimitive(i.value().doubleValue());
		}
		throw new RuntimeException("SOF Type " + s.typename() + " is not trivially convertable to Float.");
	}

	/**
	 * Execute optimized arithmetic add.
	 * 
	 * @param other The float to add.
	 * @return The sum of this float and the other.
	 * @see BuiltinOperations#add(Stackable, Stackable)
	 */
	public final FloatPrimitive add(FloatPrimitive other) {
		if (this.v == 0)
			return other;
		if (other.v == 0)
			return this;
		return createFloatPrimitive(this.v + other.v);
	}

	/**
	 * Execute optimized arithmetic divide.
	 * 
	 * @param other The float to divide.
	 * @return {@code this / other}
	 * @see BuiltinOperations#divide(Stackable, Stackable)
	 */
	public final FloatPrimitive divide(FloatPrimitive other) throws ArithmeticException {
		return other.v == 0 ? this : createFloatPrimitive(this.v / other.v);
	}

	/**
	 * Execute optimized arithmetic modulus.
	 * 
	 * @param other The float to modulus with.
	 * @return {@code this % other}
	 * @see BuiltinOperations#modulus(Stackable, Stackable)
	 */
	public final FloatPrimitive modulus(FloatPrimitive other) throws ArithmeticException {
		if (other.v == 0) {
			throw new ArithmeticException(String.format("Modulus by zero: %f mod %f", this.v, other.v));
		}
		return this.v == 0 ? this : createFloatPrimitive(this.v % other.v);
	}

	/**
	 * Execute optimized arithmetic multiply.
	 * 
	 * @param other The float to multiply.
	 * @return {@code this * other}
	 * @see BuiltinOperations#multiply(Stackable, Stackable)
	 */
	public final FloatPrimitive multiply(FloatPrimitive other) {
		return this.v == 1 ? other : (other.v == 1 ? this : createFloatPrimitive(this.v * other.v));
	}

	/**
	 * Execute optimized arithmetic subtract.
	 * 
	 * @param other The float to subtract.
	 * @return {@code this - other}
	 * @see BuiltinOperations#subtract(Stackable, Stackable)
	 */
	public final FloatPrimitive subtract(FloatPrimitive other) {
		if (other.v == 0)
			return this;
		return createFloatPrimitive(this.v - other.v);
	}

	/**
	 * Parses an SOF-syntax float in a string into an actual SOF float.
	 * 
	 * @param doubleString The string that is contains a single SOF float according to SOF float syntax. There may be
	 *                        leading or trailing whitespace.
	 * @return An SOF float primitive that represents the value of the float contained in the string.
	 * @throws IncompleteCompilerException If the string does not conform to SOF float syntax.
	 */
	public static FloatPrimitive createFloatFromString(String doubleString) throws IncompleteCompilerException {
		doubleString = doubleString.strip();
		final var m = Patterns.doublePattern.matcher(doubleString);
		if (m.matches()) {
			log.finest(() -> String.format("%s %s %s %s", m.group(1), m.group(2), m.group(3), m.group(4)));
			// split the parts of the double up
			final byte sign = (byte) ((m.group(1) == null ? "+" : m.group(1)).contentEquals("-") ? -1 : 1);
			final long integerPart = IntPrimitive.createIntegerFromString(m.group(2)).value();
			String decimalPartStr = m.group(3), exponentStr = m.group(4) == null ? "" : m.group(4);
			double decimalPart = 0d;
			long exponent = 0L;

			exponentStr = exponentStr.length() > 0 ? exponentStr.substring(1) : "";
			if (exponentStr.length() > 0) {
				exponent = IntPrimitive.createIntegerFromString(exponentStr).value();
			}

			for (var i = 0; i < decimalPartStr.length(); ++i) {
				// add in a decimal digit at its respective power of ten
				decimalPart += numberChars.get(decimalPartStr.charAt(i)) * Math.pow(10, -(i + 1));
			}
			log.finest(String.format("%d * ( %d + %f ) * 10 ^ %d", sign, integerPart, decimalPart, exponent));
			return new FloatPrimitive(sign * Math.pow(10, exponent) * (integerPart + decimalPart));
		} else {
			throw new IncompleteCompilerException("syntax", "syntax.float", doubleString);
		}
	}

	/**
	 * Round to specified number of decimal points.
	 * 
	 * @param d        number to round.
	 * @param decimals number of decimals to round to. 0 = round to integer, negative = round to tens, 100's etc.
	 * @return rounded number.
	 */
	public static double round(double d, int decimals) {
		return Math.round(d * Math.pow(10, decimals)) / Math.pow(10, decimals);
	}

	@Override
	public boolean equals(Stackable other) {
		if (other instanceof FloatPrimitive otherFloat) {
			return round(otherFloat.v, EQUALITY_PRECISION) == round(this.v, EQUALITY_PRECISION);
		}
		return false;
	}

	@Override
	public int compareTo(Stackable x) {
		if (x instanceof FloatPrimitive o) {
			return (this.v - o.v > 0 ? 1 : (this.v - o.v < 0 ? -1 : 0));
		} else if (x instanceof IntPrimitive o) {
			return (this.v - o.value() > 0 ? 1 : (this.v - o.value() < 0 ? -1 : 0));
		}
		throw new RuntimeException(new IncompleteCompilerException("type", "type.compare", this.typename(), x.typename()));
	}

	@Override
	public String print() {
		// Java float tostring is almost fine, except for allowing exponents with simple
		// "E###" format, where ### is the actual positive exponent. Using this simple
		// regex replacement puts the tostring in an acceptable format re-parseable by
		// the above custom SOF float parser.
		return Double.toString(this.v).replaceAll("[eE](?!\\-)", "e+");
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
