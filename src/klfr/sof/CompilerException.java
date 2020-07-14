package klfr.sof;

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

	public static final int EXPRESSION_OUTPUT_LEN = 30;

	/**
	 * Whether this element has information about execution location and mischievous
	 * code available.
	 */
	private boolean infoPresent = false;

	/**
	 * @return Whether this element has information about execution location and
	 *         mischievous code available.
	 */
	public boolean isInfoPresent() {
		return infoPresent;
	}

	protected CompilerException(String arg0, boolean infoPresent) {
		super(arg0);
		this.infoPresent = infoPresent;
	}

	protected CompilerException(Throwable arg0, boolean infoPresent) {
		super(arg0);
		this.infoPresent = infoPresent;
	}

	protected CompilerException(String arg0, Throwable arg1) {
		super(arg0, arg1);
		infoPresent = false;
	}

	protected CompilerException(String arg0, Throwable arg1, boolean arg2, boolean arg3) {
		super(arg0, arg1, arg2, arg3);
		infoPresent = false;
	}

	/**
	 * Makes a compiler exception that does not have all information for nice
	 * formatting.
	 * 
	 * @param name   Name of exception
	 * @param reason Why the exception occurred
	 */
	public static CompilerException makeIncomplete(String name, String reason) {
		return new CompilerException(name + " " + reason, false);
	}

	/**
	 * Passes its arguments directly to
	 * {@link #formatMessage(String, int, int, String, String)}. The use of this
	 * method is discouraged as it often requires precise pre-processing of indices
	 * and strings. However, it might be useful for very specific error cases.
	 */
	public static CompilerException fromFormatMessage(String expression, int index, int linenum, String name,
			String reason) {
		return new CompilerException(formatMessage(expression, index, linenum, name, reason), true);
	}

	/**
	 * Makes a compiler exception that takes its positional information from a
	 * tokenizer
	 * 
	 * @param expressionInfo A tokenizer pointing to the position where the
	 *                       exception occurred
	 * @param name           Name of the exception
	 * @param reason         Why the exception occurred
	 * @return nicely formatted multi-line string
	 */
	public static CompilerException fromCurrentPosition(Tokenizer expressionInfo, String name, String reason) {
		// left = line, right = index in line
		final var codePosition = expressionInfo.getCurrentPosition();
		var allCode = expressionInfo.getCode();
		// line number-1 because is human-readable "one-based"
		var expressionLine = Pattern.compile("\\R")
				.matcher(Pattern.compile("$", Pattern.MULTILINE).split(allCode)[codePosition.getLeft() - 1]).replaceAll("");
		return CompilerException.fromFormatMessage(expressionLine, codePosition.getRight(), codePosition.getLeft(),
				name == null ? "Interpreter" : name, reason);
	}

	/**
	 * Makes a compiler exception that takes its positional information from a
	 * tokenizer state
	 * 
	 * @param expressionInfo A tokenizer state pointing to the position where the
	 *                       exception occurred
	 * @param name           Name of the exception
	 * @param reason         Why the exception occurred
	 * @return nicely formatted multi-line string
	 */
	public static CompilerException fromCurrentPosition(TokenizerState expressionInfo, String name, String reason) {
		return fromCurrentPosition(Tokenizer.fromState(expressionInfo), name, reason);
	}

	/**
	 * Makes a compiler exception that takes its positional information from
	 * tokenizer-like data (all code, index inside code)
	 * 
	 * @param fullExpression All the code
	 * @param index          Index inside fullExpression where the exception
	 *                       occurred
	 * @param name           Name of the exception
	 * @param reason         Why the exception occurred
	 * @return nicely formatted multi-line string
	 */
	public static CompilerException fromCurrentPosition(String fullExpression, int index, String name, String reason) {
		var info = new TokenizerState(index, index + 1, 0, fullExpression.length(), fullExpression);
		return fromCurrentPosition(info, name, reason);
	}

	/**
	 * Makes a compiler exception that refers to a single line expression.
	 * 
	 * @param expression expression where error occurred
	 * @param index      index in expression where error occurred
	 * @param name       type of exception, e.g. Syntax, Value
	 * @param reason     explanation why the exception occurred
	 * @return nicely formatted multi-line string
	 */
	public static CompilerException fromSingleLineExpression(String expression, int index, String name, String reason) {
		var str = formatMessage(expression, index, 0, name, reason);
		return new CompilerException(str, true);
	}

	/**
	 * Constructs a compiler exception with the given base exception that points to
	 * the current place in code the tokenizer is looking at. <br>
	 * <br>
	 * This method is intended to be used with exceptions thrown by other classes
	 * unaware of the interpreter state. These classes can use the simple format "<
	 * Name > < Description >" for their exception message to achieve suitable
	 * formatting of the exception. As only one line of the exception message is
	 * extracted, further lines can provide debug information to be used otherwise.
	 * 
	 * @param expressionInfo The tokenizer that points to the location where the
	 *                       exception occured
	 * @param cause          The cause of this exception. The first word of the
	 *                       exception message is used as the name (such as
	 *                       "Syntax") and the rest as the long reason.
	 * @return The newly constructed compiler error.
	 */
	public static CompilerException fromIncomplete(Tokenizer expressionInfo, CompilerException cause) {
		Scanner helper = new Scanner(cause.getLocalizedMessage());
		String name = helper.next();
		String reason = helper.nextLine();
		helper.close();
		final var exc = fromCurrentPosition(expressionInfo, name, reason);
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
		final var formatStr = "%s Error in line %d at index %d:%n %s%n %s%n    %s";
		// log.fine(() -> String.format(
		// "index %d, exprlen %d, significant %d, error line <%s>", index,
		// expression.length(),
		// significantAfterTrimmed(index, expression.length()),
		// expression.replace("\n", "\\n").replace("\t", "\\t").replace("\r", "\\r")));
		return String.format(formatStr, name, line, index, trim(expression, index),
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
			return original.substring(length - EXPRESSION_OUTPUT_LEN);
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
			// displayed string's length
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
