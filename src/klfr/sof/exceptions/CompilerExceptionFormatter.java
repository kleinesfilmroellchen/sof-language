package klfr.sof.exceptions;

import static klfr.Utility.*;

import java.util.*;
import java.util.regex.*;

import klfr.sof.*;
import klfr.sof.Tokenizer;
import klfr.sof.Tokenizer.TokenizerState;

/**
 * A formatter for compiler exceptions.
 * This class is used by {@link CompilerException#getMessage()} and other users to create nicely formatted compiler exception infos.
 * 
 * @author klfr
 */
public class CompilerExceptionFormatter {
	private static final String EXCEPTION_FORMAT = "%s Error in file %s line %d at index %d:%n %s%n %s%n    %s";
	
	/**
	 * The maximum length of the outputted source code.
	 */
	public static final int EXPRESSION_OUTPUT_LEN = 70;
	
	/**
	 * Does the formatting for CLI messages.
	 * 
	 * @param expression      expression where error occurred
	 * @param index           index in expression where error occurred
	 * @param line            line where expression occurred, purely symbolic
	 * @param name            type of exception, e.g. Syntax, Value
	 * @param reason          explanation why the exception occurred
	 * @param locale          locale of the message strings
	 * @param formatArguments 
	 * @return nicely formatted multi-line string
	 */
	@SuppressWarnings("resource")
	private static String formatCLIMessage(String filename, String expression, int index, int line, String name,
			String reason, Locale locale, Object[] formatArguments) {
		// log.fine(() -> String.format(
		// "index %d, exprlen %d, significant %d, error line <%s>", index,
		// expression.length(),
		// significantAfterTrimmed(index, expression.length()),
		// expression.replace("\n", "\\n").replace("\t", "\\t").replace("\r", "\\r")));
		final ResourceBundle R = ResourceBundle.getBundle(Interpreter.MESSAGE_RESOURCE, locale);

		return String.format(EXCEPTION_FORMAT,
				// name: error type localized
				R.getString("sof.error.type." + name),
				// direct parameters file name, line number, index number
				filename, line, index,
				// erroneous code: trim the error line with the given index
				trim(expression, index),
				// error pointer character: appropriate padding
				" ".repeat(significantAfterTrimmed(index, expression.length())) + "^", 
				// reason: format with localized type and arguments
				new Formatter(locale)
					  .format(R.getString("sof.error.message." + reason), formatArguments).toString());
	}

	/**
	 * Trims string to exact length EXPRESSION_OUTPUT_LEN while respecting the
	 * significant index.
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

	/**
	 * The compiler exception that this formatter is using to produce formatted output.
	 */
	public final CompilerException exception;

	/**
	 * Creates a new formatter for the given exception.
	 * @param exception The exception that this formatter will format.
	 */
	public CompilerExceptionFormatter(CompilerException exception) {
		this.exception = exception;
	}

	/**
	 * Format the compiler exception of this formatter in the format suited for CLI output.
	 * This output is very detailed and includes the error location.
	 * @param locale The locale for the text message.
	 * @return A nicely formatted multiline string.
	 */
	public String formatCLI(final Locale locale) {
		final var filename = this.exception.getLocation();
		final var code = filename.code();
		final var index = this.exception.getIndex();
		final var name = this.exception.getNameKey();
		final var reason = this.exception.getReasonKey();
		final var formatArguments = this.exception.getFormatArguments();

		final var expressionTokenizer = Tokenizer.fromState(new TokenizerState(index, index + 1, 0, code.length(), code));
		// left = line, right = index in line
		final var codePosition = expressionTokenizer.getCurrentPosition();
		// line number-1 because is human-readable "one-based"
		var expressionLine = Pattern.compile("\\R")
				.matcher(Pattern.compile("$", Pattern.MULTILINE).split(code)[codePosition.getLeft() - 1]).replaceAll("");

		return formatCLIMessage(filename.sourceFile().getPath(), expressionLine, codePosition.getRight(), codePosition.getLeft(), name, reason, locale, formatArguments);
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
