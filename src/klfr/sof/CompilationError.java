package klfr.sof;

/**
 * Compiler/interpreter exception class with fancy formatting.
 * 
 * @author klfr
 */
public class CompilationError extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private boolean infoPresent = false;

	/**
	 * @return Whether this compilation error has a nice error message attached to
	 *         it.
	 */
	public boolean isInfoPresent() {
		return infoPresent;
	}

	/**
	 * Nicely formatted compiler exception.
	 * 
	 * @param expression Full string (only include relevant line plz).
	 * @param line       Line where exception occured
	 * @param index      Location where exception occurred
	 * @param reason     Short exception reason (without 'exception' string).
	 */
	public CompilationError(String expression, int index, int line, String name, String reason, Throwable t) {
		super(formatMessage(expression, index, line, name, reason), t);
		infoPresent = true;
	}

	public CompilationError(String expression, int index, int linenum, String name, String reason) {
		super(formatMessage(expression, index, linenum, name, reason));
		infoPresent = true;
	}

	public CompilationError(String arg0) {
		super(arg0);
		infoPresent = false;
	}

	public CompilationError(Throwable arg0) {
		super(arg0);
		infoPresent = false;
	}

	public CompilationError(String arg0, Throwable arg1) {
		super(arg0, arg1);
		infoPresent = false;
	}

	public CompilationError(String arg0, Throwable arg1, boolean arg2, boolean arg3) {
		super(arg0, arg1, arg2, arg3);
		infoPresent = false;
	}

	/** does the formatting for standard exceptions */
	private static String formatMessage(String expression, int index, int line, String name, String reason) {
		System.out.println(expression.length());
		return String.format("%s Error in line %d at index %d:%n" + "%s" + "%"
				+ significantAfterTrimmed(index, expression.length()) + "s%n" + "    %s", name, line, index,
				trim(expression, index), "^", reason);
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
	 * @param significantIndex The index where the important bit is in the actual
	 *                         string.
	 * @param length           The actual string's length.
	 */
	private static int significantAfterTrimmed(int significantIndex, int length) {
		int min = significantIndex - 10, max = significantIndex + 10;
		// there is no trim from the start
		if (min <= 0)
			return significantIndex + 1;
		// no trim from the end: distance to end is the new location
		if (max >= length)
			return 20 - (length - significantIndex);
		// trim from the start and end means that the char is always at index 10
		return 11;
	}

}
