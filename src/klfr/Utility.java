package klfr;

/**
 * Class containing general utility functions.
 */
public final class Utility {

	/**
	 * The default padding string for the padding functions: A space character.
	 */
	public static final char DEFAULT_PADDING = ' ';

	/**
	 * Ensure the given string's minimum length of len by padding it with padding characters on the left.
	 * 
	 * @param s       The string to be padded.
	 * @param len     The minimum length of the string. Longer strings are returned as-is.
	 * @param padding The padding character for extending the string's length.
	 * @return A string of minimum length len.
	 */
	public static String padLeft(final String s, final int len, final char padding) {
		final int charsToAdd = len - s.length();
		if (charsToAdd <= 0)
			return s;
		return Character.toString(padding).repeat(charsToAdd) + s;
	}

	/**
	 * Ensure the given string's minimum length of len by padding it with padding characters on the left. Uses space
	 * padding.
	 * 
	 * @param s   The string to be padded.
	 * @param len The minimum length of the string. Longer strings are returned as-is.
	 * @return A string of minimum length len.
	 */
	public static String padLeft(final String s, final int len) {
		return padLeft(s, len, DEFAULT_PADDING);
	}

	/**
	 * Ensure the given string's minimum length of len by padding it with padding characters on the right.
	 * 
	 * @param s       The string to be padded.
	 * @param len     The minimum length of the string. Longer strings are returned as-is.
	 * @param padding The padding character for extending the string's length.
	 * @return A string of minimum length len.
	 */
	public static String padRight(final String s, final int len, final char padding) {
		final int charsToAdd = len - s.length();
		if (charsToAdd <= 0)
			return s;
		return s + Character.toString(padding).repeat(charsToAdd);
	}

	/**
	 * Ensure the given string's minimum length of len by padding it with padding characters on the right. Uses space
	 * padding.
	 * 
	 * @param s   The string to be padded.
	 * @param len The minimum length of the string. Longer strings are returned as-is.
	 * @return A string of minimum length len.
	 */
	public static String padRight(final String s, final int len) {
		return padRight(s, len, DEFAULT_PADDING);
	}

	/**
	 * Ensure the given string's minimum length of len by padding it with padding characters on both sides, so that the
	 * original string is centered.
	 * 
	 * @param s       The string to be padded.
	 * @param len     The minimum length of the string. Longer strings are returned as-is.
	 * @param padding The padding character for extending the string's length.
	 * @return A string of minimum length len.
	 */
	public static String padCenter(final String s, final int len, final char padding) {
		final int charsToAdd = len - s.length();
		if (charsToAdd <= 0)
			return s;
		// one additional char on the left side if number of padding chars is odd
		final int oneMoreLeft = (charsToAdd % 2) == 0 ? 0 : 1;
		return Character.toString(padding).repeat(charsToAdd / 2 + oneMoreLeft) + s + Character.toString(padding).repeat(charsToAdd / 2);
	}

	/**
	 * Ensure the given string's minimum length of len by padding it with padding characters on both sides, so that the
	 * original string is centered. Uses space padding.
	 * 
	 * @param s   The string to be padded.
	 * @param len The minimum length of the string. Longer strings are returned as-is.
	 * @return A string of minimum length len.
	 */
	public static String padCenter(final String s, final int len) {
		return padCenter(s, len, DEFAULT_PADDING);
	}

	/**
	 * Convert the given double to a full string representation, i.e. no scientific notation and always twelve digits after
	 * the decimal point.
	 * 
	 * @param d The double to be converted
	 * @return A full string representation
	 */
	public static String fullDoubleToString(final double d) {
		// treat 0 separately, it will cause problems on the below algorithm
		if (d == 0) {
			return "0.000000000000";
		}
		// find the number of digits above the decimal point
		double testD = Math.abs(d);
		int digitsBeforePoint = 0;
		while (testD >= 1) {
			// doesn't matter that this loses precision on the lower end
			testD /= 10d;
			++digitsBeforePoint;
		}

		// create the decimal digits
		StringBuilder repr = new StringBuilder();
		// 10^ exponent to determine divisor and current decimal place
		int digitIndex = digitsBeforePoint;
		double dabs = Math.abs(d);
		while (digitIndex > 0) {
			// Recieves digit at current power of ten (= place in decimal number)
			long digit = (long) Math.floor(dabs / Math.pow(10, digitIndex - 1)) % 10;
			repr.append(digit);
			--digitIndex;
		}

		// insert decimal point
		if (digitIndex == 0) {
			repr.append(".");
		}

		// remove any parts above the decimal point, they create accuracy problems
		long digit = 0;
		dabs -= (long) Math.floor(dabs);
		// Because of inaccuracy, move to entirely new system of computing digits after decimal place.
		while (digitIndex > -12) {
			// Shift decimal point one step to the right
			dabs *= 10d;
			// final var oldDigit = digit;
			digit = (long) Math.floor(dabs) % 10;
			repr.append(digit);

			// This may avoid float inaccuracy at the very last decimal places.
			// However, in practice, inaccuracy is still as high as even Java itself reports.
			// dabs -= oldDigit * 10l;
			--digitIndex;
		}

		return repr.insert(0, d < 0 ? "-" : "").toString();
	}
}
