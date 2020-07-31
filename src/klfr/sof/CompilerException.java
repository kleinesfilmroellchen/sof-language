package klfr.sof;

import java.util.Formatter;
import java.util.ResourceBundle;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import klfr.sof.Tokenizer.TokenizerState;

/**
 * Compiler/interpreter exception class with fancy formatting.
 * 
 * @author klfr
 */
public class CompilerException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	private static final Logger log = Logger.getLogger(CompilerException.class.getCanonicalName());
	private static final ResourceBundle R = ResourceBundle.getBundle(Interpreter.MESSAGE_RESOURCE);
	private static final String EXCEPTION_FORMAT = "%s Error in line %d at index %d:%n %s%n %s%n    %s";

	public static final int EXPRESSION_OUTPUT_LEN = 30;

	/**
	 * A pseudo- compiler exception that does not have information about its
	 * occurance in SOF source code, because it was thrown from some subclass
	 * without interpreter access.
	 */
	public static class Incomplete extends RuntimeException {
		private static final long serialVersionUID = 1L;
		/**
		 * String that is used as a name for the 'sof.error.type.*' resource messages.
		 * This allows for localized error names.
		 */
		public final String nameKey;
		/**
		 * String that is used as a name for the 'sof.error.message.*' resource
		 * messages. This allows for localized error explanations.
		 */
		public final String explanationKey;

		public final Object[] formatArguments;

		/**
		 * Constructs a compiler exception that does not have all information for nice
		 * formatting.
		 * 
		 * @param nameKey         Name of the exception, as an accessor into the message
		 *                        resources 'sof.error.type'
		 * @param explanationKey  Why the exception occurred, as an accessor into the
		 *                        message resources 'sof.error.message'
		 * @param formatArguments Arguments for formatting the explanation message with.
		 */
		public Incomplete(String nameKey, String explanationKey, Object... formatArguments) {
			// null checks on name and explanation resource identifier
			nameKey = nameKey == null ? "generic" : nameKey;
			explanationKey = explanationKey == null ? nameKey : explanationKey;
			this.nameKey = nameKey;
			this.explanationKey = explanationKey;
			this.formatArguments = formatArguments;
		}

		public Incomplete(String string) {
			this(string, null);
		}
	}

	protected CompilerException(String message, Throwable cause) {
		super(message, cause);
	}

	private CompilerException(String message) {
		super(message);
	}

	/**
	 * Passes its arguments directly to
	 * {@link #formatMessage(String, int, int, String, String)}. The use of this
	 * method is discouraged as it often requires precise pre-processing of indices
	 * and strings. However, it might be useful for very specific error cases.
	 */
	public static CompilerException fromFormatMessage(String expression, int index, int linenum, String name,
			String reason) {
		return new CompilerException(formatMessage(expression, index, linenum, name, reason));
	}

	/**
	 * Makes a compiler exception that takes its positional information from a
	 * tokenizer.
	 * 
	 * @param expressionInfo A tokenizer pointing to the position where the
	 *                       exception occurred
	 * @param name           Name of the exception, as an accessor into the message
	 *                       resources 'sof.error.type'
	 * @param reason         Why the exception occurred, as an accessor into the
	 *                       message resources 'sof.error.message'
	 * @return nicely formatted multi-line string
	 */
	public static CompilerException fromCurrentPosition(Tokenizer expressionInfo, String name, String reason,
			final Object... formatArguments) {
		// null checks on name and explanation resource identifier
		name = name == null ? "generic" : name;
		reason = reason == null ? name : reason;

		// left = line, right = index in line
		final var codePosition = expressionInfo.getCurrentPosition();

		var allCode = expressionInfo.getCode();
		// line number-1 because is human-readable "one-based"
		var expressionLine = Pattern.compile("\\R")
				.matcher(Pattern.compile("$", Pattern.MULTILINE).split(allCode)[codePosition.getLeft() - 1]).replaceAll("");
		return CompilerException.fromFormatMessage(expressionLine, codePosition.getRight(), codePosition.getLeft(),
				R.getString("sof.error.type." + name), new Formatter(R.getLocale())
						.format(R.getString("sof.error.message." + reason), formatArguments).toString());
	}

	/**
	 * Makes a compiler exception that takes its positional information from a
	 * tokenizer state.
	 * 
	 * @param expressionInfo A tokenizer state pointing to the position where the
	 *                       exception occurred
	 * @param name           Name of the exception, as an accessor into the message
	 *                       resources 'sof.error.type'
	 * @param reason         Why the exception occurred, as an accessor into the
	 *                       message resources 'sof.error.message'
	 * @return nicely formatted multi-line string
	 */
	public static CompilerException fromCurrentPosition(TokenizerState expressionInfo, String name, String reason,
			final Object... formatArguments) {
		return fromCurrentPosition(Tokenizer.fromState(expressionInfo), name, reason);
	}

	/**
	 * Makes a compiler exception that takes its positional information from
	 * tokenizer-like data (all code, index inside code).
	 * 
	 * @param fullExpression All the code
	 * @param index          Index inside fullExpression where the exception
	 *                       occurred
	 * @param name           Name of the exception, as an accessor into the message
	 *                       resources 'sof.error.type'
	 * @param reason         Why the exception occurred, as an accessor into the
	 *                       message resources 'sof.error.message'
	 * @return nicely formatted multi-line string
	 */
	public static CompilerException fromCurrentPosition(String fullExpression, int index, String name, String reason,
			final Object... formatArguments) {
		var info = new TokenizerState(index, index + 1, 0, fullExpression.length(), fullExpression);
		return fromCurrentPosition(info, name, reason);
	}

	/**
	 * Makes a compiler exception that refers to a single line expression.
	 * 
	 * @param expression expression where error occurred
	 * @param index      index in expression where error occurred
	 * @param name       Name of the exception, as an accessor into the message
	 *                   resources 'sof.error.type'
	 * @param reason     Why the exception occurred, as an accessor into the message
	 *                   resources 'sof.error.message'
	 * @return nicely formatted multi-line string
	 */
	public static CompilerException fromSingleLineExpression(String expression, int index, String name, String reason) {
		// null checks on name and reason resource identifier
		name = name == null ? "generic" : name;
		reason = reason == null ? name : reason;

		var str = formatMessage(expression, index, 0, R.getString("sof.error.type." + name),
				R.getString("sof.error.message." + reason));
		return new CompilerException(str);
	}

	/**
	 * Constructs a compiler exception with the given base exception that points to
	 * the current place in code the tokenizer is looking at. This method is used
	 * with Incomplete compiler exceptions which were thrown by parts of the SOF
	 * system unaware of code positions.
	 * 
	 * @param expressionInfo The tokenizer that points to the location where the
	 *                       exception occured
	 * @param cause          The cause of this exception, an instance of the stub
	 *                       class {@link CompilerException.Incomplete}
	 * @return The newly constructed compiler exception.
	 */
	public static CompilerException fromIncomplete(Tokenizer expressionInfo, Incomplete cause) {
		final var exc = fromCurrentPosition(expressionInfo, cause.nameKey, cause.explanationKey, cause.formatArguments);
		exc.initCause(cause);
		return exc;
	}

	/**
	 * Constructs a compiler exception with the given base exception that points to
	 * the current place in code specified with index. This method is used
	 * with Incomplete compiler exceptions which were thrown by parts of the SOF
	 * system unaware of code positions.
	 * 
	 * @param cause          The cause of this exception, an instance of the stub
	 *                       class {@link CompilerException.Incomplete}
	 * @return The newly constructed compiler exception.
	 */
	public static CompilerException fromIncomplete(String code, int index, Incomplete cause) {
		final var exc = fromCurrentPosition(code, index, cause.nameKey, cause.explanationKey, cause.formatArguments);
		exc.initCause(cause);
		return exc;
	}

	/**
	 * Does the formatting for standard exceptions.
	 * 
	 * @param expression expression where error occurred
	 * @param index      index in expression where error occurred
	 * @param line       line where expression occurred, purely symbolic
	 * @param name       type of exception, e.g. Syntax, Value
	 * @param reason     explanation why the exception occurred
	 * @return nicely formatted multi-line string
	 */
	private static String formatMessage(String expression, int index, int line, String name, String reason) {
		// log.fine(() -> String.format(
		// "index %d, exprlen %d, significant %d, error line <%s>", index,
		// expression.length(),
		// significantAfterTrimmed(index, expression.length()),
		// expression.replace("\n", "\\n").replace("\t", "\\t").replace("\r", "\\r")));
		return String.format(EXCEPTION_FORMAT, name, line, index, trim(expression, index),
				" ".repeat(significantAfterTrimmed(index, expression.length())) + "^", reason);
	}

	/**
	 * trims string to exact length EXPRESSION_OUTPUT_LEN while respecting the
	 * significant index
	 */
	private static String trim(String original, int significantIndex) {
		int min = significantIndex - (EXPRESSION_OUTPUT_LEN / 2),
				max = significantIndex + (EXPRESSION_OUTPUT_LEN / 2) + 1, length = original.length();

		if (min < 0) {
			// case 1: from start on EXPRESSION_OUTPUT_LEN characters
			return padRight(original.substring(0, Math.min(EXPRESSION_OUTPUT_LEN, length)), EXPRESSION_OUTPUT_LEN);
		} else if (max > length) {
			// case 2: from end on EXPRESSION_OUTPUT_LEN characters (only happens if string
			// itself is more than EXPRESSION_OUTPUT_LEN chars)
			return padLeft(original.substring(Math.max(length - EXPRESSION_OUTPUT_LEN, 0)), EXPRESSION_OUTPUT_LEN);
		}
		// case 3: in the middle of string means that there is trim on both sides
		return original.substring(min, max);
	}

	/**
	 * Calculates position in trimmed string where the significant index lies.
	 * 
	 * @param significantIndex The index where the character is in the actual
	 *                         string.
	 * @param length           The actual string's length.
	 * @return An index (zero-based) that points to the position in the final
	 *         trimmed string where the indexed character lies
	 */
	private static int significantAfterTrimmed(int significantIndex, int length) {
		int min = significantIndex - (EXPRESSION_OUTPUT_LEN / 2),
				max = significantIndex + (EXPRESSION_OUTPUT_LEN / 2) + 1;
		if (min < 0)
			// case 1: there is no trim from the start, index valid as-is
			return significantIndex;
		if (max > length)
			// case 2: no trim from the end: compute end offset and subtract from the
			// displayed expression output string's length, but never go past the actual string length
			return EXPRESSION_OUTPUT_LEN - (length - significantIndex);
		// trim from the start and end means that the char is always at index
		// (EXPRESSION_OUTPUT_LEN/2)
		return (EXPRESSION_OUTPUT_LEN / 2);
	}

	private static String padLeft(final String s, final int len) {
		final int charsToAdd = len - s.length();
		if (charsToAdd <= 0)
			return s;
		return " ".repeat(charsToAdd) + s;
	}

	private static String padRight(final String s, final int len) {
		final int charsToAdd = len - s.length();
		if (charsToAdd <= 0)
			return s;
		return s + " ".repeat(charsToAdd);
	}

	private static String padCenter(final String s, final int len) {
		final int charsToAdd = len - s.length();
		if (charsToAdd <= 0)
			return s;
		// one additional char on the left side if number of padding chars is odd
		final int oneMoreLeft = (charsToAdd % 2) == 0 ? 0 : 1;
		return " ".repeat(charsToAdd / 2 + oneMoreLeft) + s + " ".repeat(charsToAdd / 2);
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
