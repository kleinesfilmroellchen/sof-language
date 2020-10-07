package klfr.sof.lib;

import klfr.sof.Patterns;
import klfr.sof.lang.*;

import java.util.regex.Matcher;

import klfr.Tuple;
import klfr.Utility;

import static klfr.Utility.*;

/** Library class that handles SOF's string formatting. */
public final class Formatting {

	/**
	 * General version of the fmt functions that implements all the actual formatting.
	 * @param fstring The format string.
	 * @param fparams The format parameters.
	 */
	private static String internalFormat(String fstring, Stackable... fparams) {
		final var matcher = Patterns.formatSpecifierPattern.matcher(fstring);
		int currentIdx = 0;
		int lastMatchEnd = 0;
		StringBuilder finalString = new StringBuilder(fstring.length()*2);
		// for each format specifier in the string
		while (matcher.find()) {
			// run the actual formatting handler
			final var res = handleFormatter(matcher.group(), currentIdx, fparams);

			// update current format parameter index accordingly
			currentIdx = res.getLeft();
			// append sequence between last match and this match
			finalString.append(fstring.substring(lastMatchEnd, matcher.start()));
			// append the formatted string
			finalString.append(res.getRight());
			// set the new last match endpoint to the current match's end
			lastMatchEnd = matcher.end();
		}
		return finalString.toString();
	}

	private static class FormatSpecification {
		private static enum Justify { Left, Right, Center }
		private static enum FormatSpec { Decimal('d'), Octal('o'), Hex('x'), HexUpper('X'), Float('f'), Scientific('e'), ShortestFloat('g'), String('s'), Newline('n');
			private FormatSpec(char fc) { this.fc = fc; }
			public final char fc;
			public static FormatSpec make(final char fc) {
				for (var pt : values()) {
					if (pt.fc == fc)
						return pt;
				}
				return null;
			}
		}
		public FormatSpec fspec;
		public Justify justify = Justify.Right;
		public int precision = 0;
		public int width = 0;
		public int flags = 0;
		public char getPadChar() {
			return (flags & PAD_ZERO) > 0 ? '0' : ' ';
		}
		/** + */
		public static final int SIGN = 0b1;
		/** space */
		public static final int ALIGN_SIGN = 0b10;
		/** # */
		public static final int FULLFORM = 0b100;
		/** 0 */
		public static final int PAD_ZERO = 0b1000;
	}

	/**
	 * Handles actually creating the string for the specified formatting specifier sequence and parameters.
	 * This method supports an SOF-specific modified subset of C's printf() format specification syntax
	 * which is used (with modifications as well) widely by many programming languages.<br><br>
	 * 
	 * The following basic format is used for all format specifiers:
	 * <pre>
	 * %[flags][width][.precision]specifier
	 * </pre>
	 * The leading % is not passed to this method.
	 * 
	 * <ul>
	 * <li><strong>Specifier</strong> specifies the actual data type that is to be formatted.
	 * This is a subset of C's supported datatypes. SOF supports % (percent escape), d or i (integer),
	 * o (octal), x (hex), X (uppercase hex), f (float), e (scientific), g (float or scientific, whichever is shorter),
	 * s (string), n (system newline, this is different from C's n format specifier).</li>
	 * <li><strong>Flags</strong> specify various formatting flags. SOF supports < (left-justify), ^ (center-justify),
	 * + (force numbers to print sign), (space) (force blank space in sign location for positive numbers),
	 * # (force hex to print 0x / 0X, force decimal numbers to print decimal point), 0 (pad with zeroes instead of spaces).</li>
	 * <li><strong>Width</strong> is the minimum width of the formatted text.
	 * If the text is shorter, it is right-justified padded with spaces. Alignment and padding type is controlled by flags.</li>
	 * <li><strong>Precision</strong> depends on the format specifier. For integers, it is the minimum number of digits.
	 * If there are less digits, the number is padded with zeros. For floats, it is the number of digits after the decimal point.
	 * If there are more, it is rounded to this number, if there are less, it is padded with zeros.
	 * A precision of 0 will still only print 6 digits maximum.
	 * For strings, it is the maximum number of characters to print.</li>
	 * </ul>
	 * 
	 * @param fspecifier The format specifier, without a leading percent sign.
	 * @param currentIdx The current index in the parameter list that the format specifier applies to.
	 *                   This is ignored if the format specifier contains an absolute parameter index,
	 *                   or if the format specifier does not handle any parameters.
	 * @param fparams    The entire list of format parameters. Usually, only fparams[currentIdx] is considered.
	 * @return A tuple consisting of the string that should replace the format specifier in the final string,
	 *         and an index which specifies the index of the next to-be-handled format parameter.
	 *         This is usually currentIdx+1 for normal format specifiers, and currentIdx for format specifiers
	 *         which do not handle the specified current format parameter.
	 * @throws IllegalFormatException    If the format specifier is malformed.
	 * @throws IndexOutOfBoundsException If the current index or an additional argument index is out of bounds.
	 *                                   This is auto-thrown by the array indexing.
	 */
	private static Tuple<Integer, String> handleFormatter(String fspecifier, int currentIdx, Stackable[] fparams) throws IllegalArgumentException, ClassCastException, IndexOutOfBoundsException, NumberFormatException {
		// percent escape
		if (fspecifier.equals("%")) return Tuple.t(currentIdx, "%");

		final var fspec = parseFormatSpecifier(fspecifier);
		final var fparam = fparams[currentIdx];

		StringBuilder formatted = new StringBuilder();
		switch (fspec.fspec) {
			case Newline:
				return Tuple.t(currentIdx, System.lineSeparator());
			case Decimal:
				final long i = ((IntPrimitive)fparam).value();
				String fullInt = Long.toString(i, 10);
				if (fullInt.length() < fspec.precision) {
					fullInt = padLeft(fullInt, fspec.precision, '0');
				}
				formatted.append(fullInt);
				if (i >= 0) {
					if ((fspec.flags | FormatSpecification.SIGN) > 0)
						formatted.insert(0, '+');
					else if ((fspec.flags | FormatSpecification.ALIGN_SIGN) > 0)
						formatted.insert(0, ' ');
				}
				break;
			case Float:
				final double d = ((FloatPrimitive)fparam).value();
				String fullDouble = Utility.fullDoubleToString(d);
				// if there are at least two zeroes towards the end of the decimal line
				if (fullDouble.lastIndexOf("00") > fullDouble.length()-5) {
					fullDouble = fullDouble.substring(0, fullDouble.lastIndexOf("00"));
				}
			default:
				throw new RuntimeException("Unhandled format specifier");
		}
		// handle width, because its behavior is most general
		if (formatted.length() < fspec.width) {
			// Java Switch Expressions FTW
			formatted = switch(fspec.justify) {
				case Center -> new StringBuilder(padCenter(formatted.toString(), fspec.width, fspec.getPadChar()));
				case Left -> new StringBuilder(padLeft(formatted.toString(), fspec.width, fspec.getPadChar()));
				case Right -> new StringBuilder(padRight(formatted.toString(), fspec.width, fspec.getPadChar()));
			};
		}
		return Tuple.t(currentIdx+1, formatted.toString());
	}

	private static FormatSpecification parseFormatSpecifier(String fspecifier) throws IllegalArgumentException, ClassCastException, NumberFormatException {
		final var fspec = new FormatSpecification();
		Matcher fmatcher = Patterns.formatSpecifierPattern.matcher(fspecifier);
		if (!fmatcher.matches()) throw new IllegalArgumentException("Format specifier " + fspecifier + " malformed.");

		String newline = fmatcher.group(1), flags = fmatcher.group(2), width = fmatcher.group(3),
				 precision = fmatcher.group(4);
		char specifier = fmatcher.group(5).charAt(0);
		if (newline != null) {
			fspec.fspec = FormatSpecification.FormatSpec.Newline;
			return fspec;
		}

		// parse format specifier
		// decimal has two specifiers, handle 'i' specially
		if (specifier == 'i') fspec.fspec = FormatSpecification.FormatSpec.Decimal;
		fspec.fspec = FormatSpecification.FormatSpec.make(specifier);
		if (fspec.fspec == null) throw new IllegalArgumentException("Format specifier " + specifier + " unknown");

		// parse width and precision
		if (width != null) {
			fspec.width = Integer.parseInt(width, 10);
		}
		if (precision != null) {
			fspec.precision = Integer.parseInt(precision, 10);
		}

		// parse flags
		if (flags != null) {
			for (var flag : flags.toCharArray()) {
				if (flag == '<') fspec.justify = FormatSpecification.Justify.Left;
				else if (flag == '^') fspec.justify = FormatSpecification.Justify.Center;
				else if (flag == '+') fspec.flags |= FormatSpecification.SIGN;
				else if (flag == ' ') fspec.flags |= FormatSpecification.ALIGN_SIGN;
				else if (flag == '#') fspec.flags |= FormatSpecification.FULLFORM;
				else if (flag == '0') fspec.flags |= FormatSpecification.PAD_ZERO;
			}
		}

		return fspec;

		// int fidx = 0; var currentChar = fspecifier.charAt(fidx);
		// // handle flags
		// while (currentChar == '<' || currentChar == '^' || currentChar == '+' || currentChar == ' ' || currentChar == '#' || currentChar == '0') {
		// 	if (currentChar == '<') fspec.justify = FormatSpecification.Justify.Left;
		// 	else if (currentChar == '^') fspec.justify = FormatSpecification.Justify.Center;
		// 	else if (currentChar == '+') fspec.flags |= FormatSpecification.SIGN;
		// 	else if (currentChar == ' ') fspec.flags |= FormatSpecification.ALIGN_SIGN;
		// 	else if (currentChar == '#') fspec.flags |= FormatSpecification.FULLFORM;
		// 	else if (currentChar == '0') fspec.flags |= FormatSpecification.PAD_ZERO;
		// 	fidx++; currentChar = fspecifier.charAt(fidx);
		// }

		// // parse width
		// StringBuilder widthString = new StringBuilder();
		// while ('\u0030' <= currentChar && currentChar <= '\u0039') {
		// 	widthString.append(currentChar);
		// 	fidx++; currentChar = fspecifier.charAt(fidx);
		// }
		// if (!widthString.isEmpty()) fspec.width = Integer.parseInt(widthString, 0, widthString.length(), 10);

		// // parse precision
		// if (currentChar == '.') {
		// 	StringBuilder precString = new StringBuilder();
		// 	fidx++; currentChar = fspecifier.charAt(fidx);
		// 	while ('\u0030' <= currentChar && currentChar <= '\u0039') {
		// 		precString.append(currentChar);
		// 		fidx++; currentChar = fspecifier.charAt(fidx);
		// 	}
		// 	if (!precString.isEmpty()) fspec.precision = Integer.parseInt(precString, 0, precString.length(), 10);
		// }

		// // parse format specifier
		// // decimal has two specifiers, handle 'i' specially
		// if (currentChar == 'i') fspec.fspec = FormatSpecification.FormatSpec.Decimal;
		// fspec.fspec = FormatSpecification.FormatSpec.make(currentChar);
		// if (fspec.fspec == null) throw new IllegalArgumentException("Format specifier " + currentChar + " unknown");

		// return fspec;
	}

	public static StringPrimitive fmt(StringPrimitive fstring, Stackable farg0) {
		return StringPrimitive.createStringPrimitive(internalFormat(fstring.value(), farg0));
	}
	public static StringPrimitive fmt(StringPrimitive fstring, Stackable farg0, Stackable farg1) {
		return StringPrimitive.createStringPrimitive(internalFormat(fstring.value(), farg0, farg1));
	}
	public static StringPrimitive fmt(StringPrimitive fstring, Stackable farg0, Stackable farg1, Stackable farg2) {
		return StringPrimitive.createStringPrimitive(internalFormat(fstring.value(), farg0, farg1, farg2));
	}
	public static StringPrimitive fmt(StringPrimitive fstring, Stackable farg0, Stackable farg1, Stackable farg2, Stackable farg3) {
		return StringPrimitive.createStringPrimitive(internalFormat(fstring.value(), farg0, farg1, farg2, farg3));
	}
	public static StringPrimitive fmt(StringPrimitive fstring, Stackable farg0, Stackable farg1, Stackable farg2, Stackable farg3, Stackable farg4) {
		return StringPrimitive.createStringPrimitive(internalFormat(fstring.value(), farg0, farg1, farg2, farg3, farg4));
	}
	public static StringPrimitive fmt(StringPrimitive fstring, Stackable farg0, Stackable farg1, Stackable farg2, Stackable farg3, Stackable farg4, Stackable farg5) {
		return StringPrimitive.createStringPrimitive(internalFormat(fstring.value(), farg0, farg1, farg2, farg3, farg4, farg5));
	}
	public static StringPrimitive fmt(StringPrimitive fstring, Stackable farg0, Stackable farg1, Stackable farg2, Stackable farg3, Stackable farg4, Stackable farg5, Stackable farg6) {
		return StringPrimitive.createStringPrimitive(internalFormat(fstring.value(), farg0, farg1, farg2, farg3, farg4, farg5, farg6));
	}
	public static StringPrimitive fmt(StringPrimitive fstring, Stackable farg0, Stackable farg1, Stackable farg2, Stackable farg3, Stackable farg4, Stackable farg5, Stackable farg6, Stackable farg7) {
		return StringPrimitive.createStringPrimitive(internalFormat(fstring.value(), farg0, farg1, farg2, farg3, farg4, farg5, farg6, farg7));
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
