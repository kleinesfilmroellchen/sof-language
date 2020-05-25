package klfr.sof.lang;

import java.util.logging.Logger;

import klfr.sof.CompilerException;
import klfr.sof.Interpreter;

/**
 * floating point decimal primitive type
 */
@StackableName("Float")
public class FloatPrimitive extends Primitive {
	private static final long serialVersionUID = 1L;
	private static final Logger log = Logger.getLogger(FloatPrimitive.class.getCanonicalName());

	private final Double v;

	private FloatPrimitive(double d) {
		this.v = d;
	}

	@Override
	public Object v() {
		return v;
	}

	public Double value() {
		return v;
	}

	public static FloatPrimitive createFloatPrimitive(Double d) {
		return new FloatPrimitive(d);
	}

	/**
	 * Execute optimized arithmetic add.
	 */
	public FloatPrimitive add(FloatPrimitive other) {
		if (this.v == 0)
			return other;
		if (other.v == 0)
			return this;
		return createFloatPrimitive(this.v + other.v);
	}

	public FloatPrimitive divide(FloatPrimitive other) throws ArithmeticException {
		return other.v == 0 ? this : createFloatPrimitive(this.v / other.v);
	}

	public FloatPrimitive multiply(FloatPrimitive other) {
		return this.v == 1 ? other : (other.v == 1 ? this : createFloatPrimitive(this.v * other.v));
	}

	public FloatPrimitive subtract(FloatPrimitive other) {
		if (other.v == 0)
			return this;
		return createFloatPrimitive(this.v - other.v);
	}

	public static FloatPrimitive createFloatFromString(String doubleString) throws CompilerException {
		final var m = Interpreter.doublePattern.matcher(doubleString);
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
				decimalPart += numberChars.get(decimalPartStr.charAt(i)) * Math.pow(10, -(i + 1));
			}
			log.finest(String.format("%d * ( %d + %f ) * 10 ^ %d", sign, integerPart, decimalPart, exponent));
			return new FloatPrimitive(sign * Math.pow(10, exponent) * (integerPart + decimalPart));
		} else {
			throw CompilerException.makeIncomplete("Syntax",
					String.format("No float literal found in\"%s\".", doubleString));
		}
	}

	@Override
	public int compareTo(Stackable x) {
		if (x instanceof FloatPrimitive) {
			final var o = (FloatPrimitive) x;
			return (this.v - o.v > 0 ? 1 : (this.v - o.v < 0 ? -1 : 0));
		} else if (x instanceof IntPrimitive) {
			final var o = (IntPrimitive) x;
			return (this.v - o.value() > 0 ? 1 : (this.v - o.value() < 0 ? -1 : 0));
		}
		throw CompilerException.makeIncomplete("Type",
				String.format("%s and %s cannot be compared.", this.typename(), x.typename()));
	}
}