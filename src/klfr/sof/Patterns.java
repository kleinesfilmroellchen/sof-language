package klfr.sof;

import java.util.regex.Pattern;

/**
 * Storage class for all the regular expressions that assist in finding tokens
 * and processing them.
 */
public final class Patterns {

	/** Pattern for integer number literals. */
	public static final Pattern intPattern = Pattern.compile("(\\+|\\-)?(?:((?:0d)?[0-9]+)|(0[xh][0-9a-fA-F]+)|(0o[0-7]+)|(0b[01]+)|0)");
	/**
	 * Pattern for floating point number literals (represented with 64-bit "double"
	 * values internally). Group 1 matches the sign, if present, group 2 matches the
	 * integer section of the number, group 3 matches the fractional section of the
	 * number. Group 4 matches the scientific exponent, if present.
	 */
	public static final Pattern doublePattern = Pattern
			.compile("(\\+|\\-)?(?:([0-9]+)\\.([0-9]+)([eE][\\-\\+][0-9]+)?)");
	/**
	 * String pattern. Matches a starting quote character followed by any number of
	 * arbitrary characters followed by an ending quote character. The quote escape
	 * works in the way that a {@code \"} sequence is accepted as an "arbitrary
	 * character" (while single quote is, of course, not) and the ending quote
	 * character cannot be preceded by a {@code \} (may otherwise cause issues on
	 * end of string). This pattern does not match other escape sequences, see
	 * {@link Patterns#escapeSequencePattern}.
	 */
	public static final Pattern stringPattern = Pattern.compile("\"((?:[^\"]|(\\\\\"))*?(?<!\\\\))\"");
	/**
	 * String escape sequence pattern. Matches the entire escape sequence except the
	 * leading backslash as group 1 and the unicode code point for {@code \ u}
	 * escapes as group 2. Does not match the escaped quote, which is not treated by
	 * the "preprocessor".
	 */
	public static final Pattern escapeSequencePattern = Pattern.compile("\\\\(f|t|n|(?:u([0-9a-fA-F]{4})))");
	/**
	 * Boolean literal pattern. Matches "true" and "false", capitalized as well.
	 */
	public static final Pattern boolPattern = Pattern.compile("True|False|true|false");
	/**
	 * (Basic) Token pattern. Matches all contiguous non-space text as well as the
	 * pattern for string literals.
	 */
	public static final Pattern tokenPattern = Pattern.compile("(" + stringPattern.pattern() + ")|(\\S+)");// \\b{g}
	/**
	 * Identifier pattern. An identifier is any unicode letter possibly followed by
	 * more unicode letters, numbers or the punctuation {@code : _ '} Note that
	 * using <code>\p{L}</code> allows for inter-language identifiers; one could
	 * write variable names completely with Han logographs ("Chinese characters"), for example.
	 */
	public static final Pattern identifierPattern = Pattern.compile("\\p{L}[\\p{L}0-9_'\\:]*");
	/**
	 * The start of a code block; the single character <code>{</code>. Positive
	 * lookbehinds and lookaheads are used to ensure that the character is either
	 * before the end of input or any number of whitespace; and either after the
	 * beginning of input or any number of whitespace.
	 */
	public static final Pattern codeBlockStartPattern = Pattern.compile("(?<=^|\\s+)\\{(?=$|\\s+)");
	/**
	 * The end of a code block; the single character <code>}</code>. Positive
	 * lookbehinds and lookaheads are used to ensure that the character is either
	 * before the end of input or any number of whitespace; and either after the
	 * beginning of input or any number of whitespace.
	 */
	public static final Pattern codeBlockEndPattern = Pattern.compile("(?<=^|\\s+)\\}(?=$|\\s+)");
	/**
	 * Single-line comment; starting with a # and ending with a newline.
	 */
	public static final Pattern commentOneLinePattern = Pattern.compile("\\#.*?$", Pattern.MULTILINE);
	/**
	 * Multi-line comment; starting with a #* and ending with *#
	 */
	public static final Pattern commentMultilinePattern = Pattern.compile("\\#\\*.*?\\*\\#", Pattern.DOTALL);
	/**
	 * The start of a line; pattern created by compiling the start of string flag
	 * "^" in MULTILINE mode.
	 */
	public static final Pattern nlPat = Pattern.compile("^", Pattern.MULTILINE);
	/**
	 * Pattern for a line break.
	 */
	public static final Pattern lineBreakPattern = Pattern.compile("\\R");

	/**
	 * Format of the format specifiers in SOF format strings.
	 */
	public static final Pattern formatSpecifierPattern = Pattern.compile("(\\%n)|\\%([\\<\\^\\+ \\#0]+)?([1-9][0-9]*)?(?:\\.([1-9][0-9]*))?([diboxXefgs])");

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
