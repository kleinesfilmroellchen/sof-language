package klfr.sof.lib;

import klfr.sof.Patterns;
import klfr.sof.lang.*;
import klfr.sof.lang.primitive.FloatPrimitive;
import klfr.sof.lang.primitive.IntPrimitive;
import klfr.sof.lang.primitive.StringPrimitive;

import java.util.logging.Logger;
import java.util.regex.Matcher;

import klfr.Tuple;
import klfr.Utility;

import static klfr.Utility.*;

/** Library class that handles SOF's string formatting. */
@NativeFunctionCollection
public final class Formatting {

	private static final Logger log = Logger.getLogger(Formatting.class.getCanonicalName());

	/**
	 * General version of the fmt functions that implements all the actual formatting.
	 * 
	 * @param fstring The format string.
	 * @param fparams The format parameters.
	 * @return The formatted string; use SOF string formatting syntax.
	 */
	public static String internalFormat(String fstring, Stackable... fparams) {
		final var matcher = Patterns.formatSpecifierPattern.matcher(fstring);
		int currentIdx = 0;
		int lastMatchEnd = 0;
		StringBuilder finalString = new StringBuilder(fstring.length() * 2);
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
		// append the section of the string after the last format specifier
		finalString.append(fstring.substring(lastMatchEnd));
		return finalString.toString();
	}

	private static class FormatSpecification {

		private static enum Justify {
			Left, Right, Center
		}

		private static enum FormatSpec {

			Decimal('d'), Binary('b'), Octal('o'), Hex('x'), HexUpper('X'), Float('f'), Scientific('e'), ShortestFloat('g'), String('s'), Newline('n');

			private FormatSpec(char fc) {
				this.fc = fc;
			}

			public final char fc;

			public static FormatSpec make(final char fc) {
				for (var pt : values()) {
					if (pt.fc == fc)
						return pt;
				}
				return null;
			}
		}

		public FormatSpec	fspec;
		public Justify		justify		= Justify.Right;
		public int			precision	= 0;
		public int			width			= 0;
		public int			flags			= 0;

		public char getPadChar() {
			return ((flags & PAD_ZERO) > 0) ? '0' : ' ';
		}

		/** + */
		public static final int	SIGN			= 0b1;
		/** space */
		public static final int	ALIGN_SIGN	= 0b10;
		/** # */
		public static final int	FULLFORM		= 0b100;
		/** 0 */
		public static final int	PAD_ZERO		= 0b1000;
	}

	/**
	 * Handles actually creating the string for the specified formatting specifier sequence and parameters. This method
	 * supports an SOF-specific modified subset of C's printf() format specification syntax which is used (with
	 * modifications as well) widely by many programming languages.<br>
	 * <br>
	 * 
	 * The following basic format is used for all format specifiers:
	 * 
	 * <pre>
	 * %[flags][width][.precision]specifier
	 * </pre>
	 * 
	 * The leading % IS passed to this method.
	 * 
	 * <ul>
	 * <li><strong>Specifier</strong> specifies the actual data type that is to be formatted. This is a subset of C's
	 * supported datatypes. SOF supports % (percent escape), d or i (integer), o (octal), x (hex), X (uppercase hex), f
	 * (float), e (scientific), g (float or scientific, whichever is shorter), s (string), n (system newline, this is
	 * different from C's n format specifier).</li>
	 * <li><strong>Flags</strong> specify various formatting flags. SOF supports &lt; (left-justify), ^ (center-justify), +
	 * (force numbers to print sign), (space) (force blank space in sign location for positive numbers), # (force hex to
	 * print 0x / 0X, force octal to print leading 0, force decimal numbers to print decimal point), 0 (pad with zeroes
	 * instead of spaces).</li>
	 * <li><strong>Width</strong> is the minimum width of the formatted text. If the text is shorter, it is right-justified
	 * padded with spaces. Alignment and padding type is controlled by flags.</li>
	 * <li><strong>Precision</strong> depends on the format specifier. For integers, it is the minimum number of digits. If
	 * there are less digits, the number is padded with zeros. For floats, it is the number of digits after the decimal
	 * point. If there are more, it is rounded to this number, if there are less, it is padded with zeros. A precision of 0
	 * will still only print 6 digits maximum. For strings, it is the maximum number of characters to print.</li>
	 * </ul>
	 * 
	 * @param fspecifier The format specifier, without a leading percent sign.
	 * @param currentIdx The current index in the parameter list that the format specifier applies to. This is ignored if
	 *                      the format specifier contains an absolute parameter index, or if the format specifier does not
	 *                      handle any parameters.
	 * @param fparams    The entire list of format parameters. Usually, only fparams[currentIdx] is considered.
	 * @return A tuple consisting of the string that should replace the format specifier in the final string, and an index
	 *         which specifies the index of the next to-be-handled format parameter. This is usually currentIdx+1 for normal
	 *         format specifiers, and currentIdx for format specifiers which do not handle the specified current format
	 *         parameter.
	 * @throws IllegalArgumentException  If the format specifier is malformed.
	 * @throws IndexOutOfBoundsException If the current index or an additional argument index is out of bounds. This is
	 *                                      auto-thrown by the array indexing.
	 * @throws NumberFormatException     If number formatting fails, this is auto-thrown by the number formatters.
	 */
	public static Tuple<Integer, String> handleFormatter(String fspecifier, int currentIdx, Stackable[] fparams) throws IllegalArgumentException, IndexOutOfBoundsException, NumberFormatException {
		// percent escape
		if (fspecifier.equals("%%"))
			return Tuple.t(currentIdx, "%");

		final var fspec = parseFormatSpecifier(fspecifier);

		// treat newline first because it may even be used if there are no formatting parameters at all
		if (fspec.fspec == FormatSpecification.FormatSpec.Newline)
			return Tuple.t(currentIdx, System.lineSeparator());

		final var fparam = fparams[currentIdx];

		StringBuilder formatted = new StringBuilder();
		switch (fspec.fspec) {
		// share code between all integer radices
		case Decimal:
		case Hex:
		case HexUpper:
		case Octal:
		case Binary:
			// determine radix for toString()
			final int radix = (fspec.fspec == FormatSpecification.FormatSpec.Hex || fspec.fspec == FormatSpecification.FormatSpec.HexUpper) ? 16
					: (fspec.fspec == FormatSpecification.FormatSpec.Octal) ? 8 : (fspec.fspec == FormatSpecification.FormatSpec.Binary) ? 2 : 10;
			final long i = ((IntPrimitive) fparam).value();

			String fullInt = Long.toString(i, radix);
			if (fullInt.length() < fspec.precision)
				fullInt = padLeft(fullInt, fspec.precision, '0');
			if (fspec.fspec == FormatSpecification.FormatSpec.HexUpper)
				fullInt = fullInt.toUpperCase();

			formatted.append(fullInt);
			if ((fspec.flags & FormatSpecification.FULLFORM) > 0) {
				// handle the two/three types of full form
				if (fspec.fspec == FormatSpecification.FormatSpec.Octal)
					formatted.insert(0, '0');
				else if (fspec.fspec == FormatSpecification.FormatSpec.Hex)
					formatted.insert(0, "0x");
				else if (fspec.fspec == FormatSpecification.FormatSpec.HexUpper)
					formatted.insert(0, "0X");
			}
			if (i >= 0) {
				if ((fspec.flags & FormatSpecification.SIGN) > 0)
					formatted.insert(0, '+');
				else if ((fspec.flags & FormatSpecification.ALIGN_SIGN) > 0)
					formatted.insert(0, ' ');
			} else {
				// delete any negative signs and add one to the front
				formatted.deleteCharAt(formatted.indexOf("-"));
				formatted.insert(0, '-');
			}
			break;
		case Float:
			// Using the rounding trick here already to make the limited decimal representation rounding-accurate
			final double d = fspec.precision <= 0 ? ((FloatPrimitive) fparam).value() : Math.round(((FloatPrimitive) fparam).value() * Math.pow(10, fspec.precision)) / Math.pow(10, fspec.precision);
			StringBuilder fullDouble = new StringBuilder(Utility.fullDoubleToString(d));
			log.fine(fullDouble.toString());
			// Maximum precision is demanded, pad the number out with zeroes for precision over 12
			if (fspec.precision >= 12) {
				fullDouble = new StringBuilder(Utility.padRight(fullDouble.toString(), fspec.precision, '0'));
			} else if (fspec.precision <= 0) {
				// If precision is zero (or less, shouldn't happen), use the optimal digit length
				int idx = fullDouble.length();
				// advance to the last zero-containing digit
				while ((--idx) > fullDouble.lastIndexOf(".") && (fullDouble.charAt(idx) == '0' || fullDouble.charAt(idx) == '9')) {

				}
				// more rectification for long "99999" chains
				if (fullDouble.charAt(idx + 1) == '9')
					fullDouble.insert(idx, String.valueOf((char) (fullDouble.charAt(idx) + 1)));
				// special case for integers that have all zeroes behind the decimal point
				if (idx == fullDouble.lastIndexOf(".")) {
					// full form flag specifies that even for the integers, a decimal point and a 0 needs to be printed.
					if ((fspec.flags & FormatSpecification.FULLFORM) > 0)
						++idx;
					// otherwise remove it
					else
						--idx;
				}

				fullDouble.setLength(idx + 1);
			} else {
				// one character BEHIND the actual last character to be printed, see setLength below
				var lastRelevantChar = fullDouble.length() - (12 - fspec.precision);
				// HACK: Floating-point imprecisions are revealed by the super-accurate toString converter.
				// To prevent 999999... chains (or similar) messing up converted rounded numbers, increment the last printed digit
				// if another digit after it is greater or equal to 5. (Char comparison works because the ASCII is nicely in order)
				if (lastRelevantChar < fullDouble.length() && fullDouble.charAt(lastRelevantChar) >= '5') {
					// insertion is fine because the later sections will be thrown away anyways
					fullDouble.insert(lastRelevantChar - 1, String.valueOf((char) (fullDouble.charAt(lastRelevantChar - 1) + 1)));
				}
				// Remove however many digits necessary from the end
				fullDouble.setLength(lastRelevantChar);
			}
			formatted.append(fullDouble);
			if (d >= 0) {
				if ((fspec.flags & FormatSpecification.SIGN) > 0)
					formatted.insert(0, '+');
				else if ((fspec.flags & FormatSpecification.ALIGN_SIGN) > 0)
					formatted.insert(0, ' ');
			}
			break;
		case String:
			formatted.append(fparam.print());
			break;
		default:
			throw new RuntimeException("Unhandled format specifier");
		}
		// handle width, because its behavior is most general
		if (formatted.length() < fspec.width) {
			// Java Switch Expressions FTW
			formatted = switch (fspec.justify) {
			case Center -> new StringBuilder(padCenter(formatted.toString(), fspec.width, fspec.getPadChar()));
			case Left -> new StringBuilder(padRight(formatted.toString(), fspec.width, fspec.getPadChar()));
			case Right -> new StringBuilder(padLeft(formatted.toString(), fspec.width, fspec.getPadChar()));
			};
		}
		return Tuple.t(currentIdx + 1, formatted.toString());
	}

	private static FormatSpecification parseFormatSpecifier(String fspecifier) throws IllegalArgumentException, NumberFormatException {
		final var fspec = new FormatSpecification();
		Matcher fmatcher = Patterns.formatSpecifierPattern.matcher(fspecifier);
		if (!fmatcher.matches())
			throw new IllegalArgumentException("Format specifier " + fspecifier + " malformed.");

		String newline = fmatcher.group(1), flags = fmatcher.group(2), width = fmatcher.group(3), precision = fmatcher.group(4);
		if (newline != null) {
			fspec.fspec = FormatSpecification.FormatSpec.Newline;
			return fspec;
		}
		char specifier = fmatcher.group(5).charAt(0);

		// parse format specifier
		fspec.fspec = FormatSpecification.FormatSpec.make(specifier);
		// decimal has two specifiers, handle 'i' specially
		if (specifier == 'i')
			fspec.fspec = FormatSpecification.FormatSpec.Decimal;
		if (fspec.fspec == null)
			throw new IllegalArgumentException("Format specifier " + specifier + " unknown");

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
				if (flag == '<')
					fspec.justify = FormatSpecification.Justify.Left;
				else if (flag == '^')
					fspec.justify = FormatSpecification.Justify.Center;
				else if (flag == '+')
					fspec.flags |= FormatSpecification.SIGN;
				else if (flag == ' ')
					fspec.flags |= FormatSpecification.ALIGN_SIGN;
				else if (flag == '#')
					fspec.flags |= FormatSpecification.FULLFORM;
				else if (flag == '0')
					fspec.flags |= FormatSpecification.PAD_ZERO;
			}
		}

		return fspec;
	}

	/**
	 * Format with 0 arguments.
	 * 
	 * @param fstring The string to format.
	 * @return The formatted string.
	 */
	public static StringPrimitive fmt(StringPrimitive fstring) {
		return StringPrimitive.createStringPrimitive(internalFormat(fstring.value()));
	}

	/**
	 * Format with 1 arguments.
	 * 
	 * @param fstring The string to format.
	 * @param farg0   The first formatting argument.
	 * @return The formatted string.
	 */
	public static StringPrimitive fmt(StringPrimitive fstring, Stackable farg0) {
		return StringPrimitive.createStringPrimitive(internalFormat(fstring.value(), farg0));
	}

	/**
	 * Format with 2 arguments.
	 * 
	 * @param fstring The string to format.
	 * @param farg0   The first formatting argument.
	 * @param farg1   The second formatting argument.
	 * @return The formatted string.
	 */
	public static StringPrimitive fmt(StringPrimitive fstring, Stackable farg0, Stackable farg1) {
		return StringPrimitive.createStringPrimitive(internalFormat(fstring.value(), farg0, farg1));
	}

	/**
	 * Format with 3 arguments.
	 * 
	 * @param fstring The string to format.
	 * @param farg0   The first formatting argument.
	 * @param farg1   The second formatting argument.
	 * @param farg2   The third formatting argument.
	 * @return The formatted string.
	 */
	public static StringPrimitive fmt(StringPrimitive fstring, Stackable farg0, Stackable farg1, Stackable farg2) {
		return StringPrimitive.createStringPrimitive(internalFormat(fstring.value(), farg0, farg1, farg2));
	}

	/**
	 * Format with 4 arguments.
	 * 
	 * @param fstring The string to format.
	 * @param farg0   The first formatting argument.
	 * @param farg1   The second formatting argument.
	 * @param farg2   The third formatting argument.
	 * @param farg3   The fourth formatting argument.
	 * @return The formatted string.
	 */
	public static StringPrimitive fmt(StringPrimitive fstring, Stackable farg0, Stackable farg1, Stackable farg2, Stackable farg3) {
		return StringPrimitive.createStringPrimitive(internalFormat(fstring.value(), farg0, farg1, farg2, farg3));
	}

	/**
	 * Format with 5 arguments.
	 * 
	 * @param fstring The string to format.
	 * @param farg0   The first formatting argument.
	 * @param farg1   The second formatting argument.
	 * @param farg2   The third formatting argument.
	 * @param farg3   The fourth formatting argument.
	 * @param farg4   The fifth formatting argument.
	 * @return The formatted string.
	 */
	public static StringPrimitive fmt(StringPrimitive fstring, Stackable farg0, Stackable farg1, Stackable farg2, Stackable farg3, Stackable farg4) {
		return StringPrimitive.createStringPrimitive(internalFormat(fstring.value(), farg0, farg1, farg2, farg3, farg4));
	}

	/**
	 * Format with 6 arguments.
	 * 
	 * @param fstring The string to format.
	 * @param farg0   The first formatting argument.
	 * @param farg1   The second formatting argument.
	 * @param farg2   The third formatting argument.
	 * @param farg3   The fourth formatting argument.
	 * @param farg4   The fifth formatting argument.
	 * @param farg5   The sixth formatting argument.
	 * @return The formatted string.
	 */
	public static StringPrimitive fmt(StringPrimitive fstring, Stackable farg0, Stackable farg1, Stackable farg2, Stackable farg3, Stackable farg4, Stackable farg5) {
		return StringPrimitive.createStringPrimitive(internalFormat(fstring.value(), farg0, farg1, farg2, farg3, farg4, farg5));
	}

	/**
	 * Format with 7 arguments.
	 * 
	 * @param fstring The string to format.
	 * @param farg0   The first formatting argument.
	 * @param farg1   The second formatting argument.
	 * @param farg2   The third formatting argument.
	 * @param farg3   The fourth formatting argument.
	 * @param farg4   The fifth formatting argument.
	 * @param farg5   The sixth formatting argument.
	 * @param farg6   The seventh formatting argument.
	 * @return The formatted string.
	 */
	public static StringPrimitive fmt(StringPrimitive fstring, Stackable farg0, Stackable farg1, Stackable farg2, Stackable farg3, Stackable farg4, Stackable farg5, Stackable farg6) {
		return StringPrimitive.createStringPrimitive(internalFormat(fstring.value(), farg0, farg1, farg2, farg3, farg4, farg5, farg6));
	}

	/**
	 * Format with 8 arguments.
	 * 
	 * @param fstring The string to format.
	 * @param farg0   The first formatting argument.
	 * @param farg1   The second formatting argument.
	 * @param farg2   The third formatting argument.
	 * @param farg3   The fourth formatting argument.
	 * @param farg4   The fifth formatting argument.
	 * @param farg5   The sixth formatting argument.
	 * @param farg6   The seventh formatting argument.
	 * @param farg7   The eighth formatting argument.
	 * @return The formatted string.
	 */
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
