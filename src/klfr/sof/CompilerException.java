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
		var expressionLine = Pattern.compile("$", Pattern.MULTILINE).split(allCode)[codePosition.getLeft()-1];
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
		final var formatStr = "%s Error in line %d at index %d:%n %s%n %"
				+ (significantAfterTrimmed(index, expression.length()) + 1) + "s%n    %s";
		// log.fine(String.format("index %d, exprlen %d, fstring %s", index, expression.length(), formatStr));
		return String.format(formatStr, name, line, index, trim(expression, index), "^", reason);
	}

	/** trims string to exact length 20 while respecting the significant index */
	private static String trim(String original, int significantIndex) {
		int min = significantIndex - 10, max = significantIndex + 10, length = original.length();

		if (min < 0) {
			// case 1: from start on 20 characters
			return String.format("%" + (length < 20 ? "-" : "") + "20s", original.substring(0, Math.min(20, length)));
		} else if (max >= length) {
			// case 2: from end on 20 characters (only happens if string itself is more than
			// 20 chars)
			return String.format("%20s", original.substring(Math.max(length - 20, 0), length));
		}
		// case 3: in the middle of string means that there is trim on both sides
		return String.format("%20s", original.substring(min, max));
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
		int min = significantIndex - 10, max = significantIndex + 10;
		if (min < 0)
			// case 1: there is no trim from the start, index valid as-is
			return significantIndex;
		if (max >= length)
			// case 2: no trim from the end: compute end offset and
			return 20 - (length - significantIndex);
		// trim from the start and end means that the char is always at index 10
		return 10;
	}

}
