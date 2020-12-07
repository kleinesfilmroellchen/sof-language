package klfr.sof;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The SOF preprocessor is a collection of static methods that normalize and
 * clean code before it can be executed. Unlike normal preprocessors, this
 * preprocessor does little to no logical work on the source code it is given
 * and only detects the most basic of syntax errors.
 */
public class Preprocessor {

	private static enum PreprocessorState {
		CODE, STRING, COMMENT_STARTED, MULTILINE_COMMENT, LINE_COMMENT, EXPECTING_MLCOMMENT_END;
	}

	private static final Logger log = Logger.getLogger(Preprocessor.class.getCanonicalName());

	// private static String replaceBySpace(MatchResult comment) {
	// 	return comment.group().replaceAll("[^\n]", " ");
	// }

	/**
	 * Preprocesses an SOF literal string token that has already been processed by
	 * {@link Preprocessor#preprocessCode(String)}. The given token must still
	 * contain the leading and trailing double quote, and the
	 * {@link klfr.sof.Patterns#stringPattern} is used to match the string
	 * contents and replace the escape sequences.
	 * 
	 * @param sofString The SOF literal string token to be processed.
	 * @return A string that can be used directly to construct a
	 *         {@link klfr.sof.lang.primitive.StringPrimitive}.
	 */
	public static String preprocessSofString(final String sofString) {
		final var strm = Patterns.stringPattern.matcher(sofString);
		if (!strm.matches()) {
			throw new CompilerException.Incomplete("syntax", "syntax.string", sofString);
		}
		final var str = strm.group(1);
		return Patterns.escapeSequencePattern.matcher(str).replaceAll(escape -> {
			if (escape.group(2) != null) {
				// unicode escape sequence
				final int codepoint = Integer.parseInt(escape.group(2), 16);
				return String.valueOf(Character.toChars(codepoint));
			}
			switch (escape.group(1)) {
				case "n":
					return "\n";
				case "t":
					return "\t";
				case "f":
					return "\f";
				default:
					return "\\" + escape.group(1);
			}
		}).replace("\\\"", "\"");
	}

	public static String preprocessCode(String dirtyCode) {
		var state = PreprocessorState.CODE;
		boolean acceptNextChar = false;
		StringBuilder clean = new StringBuilder(dirtyCode.length());
		for (char c : dirtyCode.toCharArray()) {
			// flag to auto-accept any character that occurs
			if (acceptNextChar) {
				clean.append(c);
				acceptNextChar = false;
				continue;
			}
			switch (state) {
				// normal code, check for transition into comment or string
				case CODE:
					if (c == '#') {
						state = PreprocessorState.COMMENT_STARTED;
						clean.append(" ");
					} else {
						if (c == '"')
							state = PreprocessorState.STRING;
						clean.append(c);
					}
					break;
				// a # was found last time, either single line or multiline comment
				case COMMENT_STARTED:
					if (c == '*') {
						state = PreprocessorState.MULTILINE_COMMENT;
						clean.append(" ");
					} else {
						state = PreprocessorState.LINE_COMMENT;
						clean.append(" ");
					}
					break;
				// inside a line comment, append a space if no line break occurs
				case LINE_COMMENT:
					if (c == '\n') {
						state = PreprocessorState.CODE;
						clean.append(c);
					} else
						clean.append(" ");
					break;
				// inside a multiline comment, check if end of comment may occur
				case MULTILINE_COMMENT:
					if (c == '*')
						state = PreprocessorState.EXPECTING_MLCOMMENT_END;
					else if (c == '\n')
						clean.append("\n");
					else
						clean.append(" ");
					break;
				// a * last time inside a multiline comment, expecting the mlcomment to end
				case EXPECTING_MLCOMMENT_END:
					if (c == '#') {
						state = PreprocessorState.CODE;
						// append two spaces b/c when finding the * nothing was appended
						clean.append("  ");
					} else {
						state = PreprocessorState.MULTILINE_COMMENT;
						// the mlcomment did not end, append the "ignored" *
						clean.append('*').append(c);
					}
					break;
				// inside a string
				case STRING:
					// ignore escaped chars, they are processed when the string is created
					if (c == '\\') {
						acceptNextChar = true;
						clean.append(c);
					} else // transition back to code
					if (c == '"') {
						state = PreprocessorState.CODE;
						clean.append(c);
					} else
						clean.append(c);
					break;
			}
		}
		// if (state == PreprocessorState.STRING)
		// 	throw CompilerException.fromCurrentPosition(dirtyCode, dirtyCode.length() - 1, "syntax",
		// 			"syntax.string.unclosed");
		return clean.toString();
	}

	/**
	 * Searches the String for two matching (open&amp;close, like parenthesis) patterns
	 * and returns the index after the closing character. Also keeps track of
	 * nesting levels.
	 * 
	 * @param toSearch     The String through which is searched.
	 * @param indexOfFirst The index where the opening pattern starts. The method
	 *                     will search for the closing character combination that
	 *                     matches with this combination.
	 * @param toMatchOpen  The pattern that denotes the opening or introduction of a
	 *                     new nesting level.
	 * @param toMatchClose The pattern that denotes the closing or finalization of a
	 *                     nesting level.
	 * @return The index directly after the closing pattern that matches the given
	 *         opening pattern at the given index. If an error occurs, such as not
	 *         finding matching characters or nesting level errors, the index
	 *         returned is -1.
	 */
	public static int indexOfMatching(final String toSearch, final int indexOfFirst, final Pattern toMatchOpen,
			final Pattern toMatchClose) {
		final Matcher openingMatcher = toMatchOpen.matcher(toSearch);
		final Matcher closingMatcher = toMatchClose.matcher(toSearch);
		boolean openingAvailable = openingMatcher.find(indexOfFirst),
				closingAvailable = closingMatcher.find(indexOfFirst);
		if (!openingAvailable || !closingAvailable)
			return -1;
		int openingStart = openingMatcher.start(), closingStart = closingMatcher.start();
		int indentationLevel = 0;
		int lastValidClosing;

		do {
			lastValidClosing = closingMatcher.end();
			// only do this if there was an opening available in the last search.
			// if not, then it is useless to try further.
			if (openingStart < closingStart && openingAvailable) {
				// the opening occurs first, so advance it
				++indentationLevel;
				openingAvailable = openingMatcher.find();
				if (openingAvailable)
					openingStart = openingMatcher.start();
				// set the start of the next opening to a high value so the second clause is
				// definitely triggered next time
				else
					openingStart = Integer.MAX_VALUE;
			} else
			// only do this if there was a closing available in the last search.
			// if not, then it is useless to try further.
			if (closingAvailable) {
				// the closing occurs first, so advance it
				--indentationLevel;
				closingAvailable = closingMatcher.find();
				if (closingAvailable)
					closingStart = closingMatcher.start();
				// set the start of the next closing to a low value so the first clause is
				// definitely triggered next time
				else
					closingStart = Integer.MIN_VALUE;
			}
		} while ((openingAvailable || closingAvailable) && indentationLevel > 0);
		if (indentationLevel != 0)
			return -1;
		return lastValidClosing;
	}

	/**
	 * Searches the String for two matching (open&amp;close, like parenthesis) character
	 * pairs and returns the index after the closing character. Also keeps track of
	 * nesting levels.
	 * 
	 * @param toSearch     The String through which is searched.
	 * @param indexOfFirst The index where the opening character combination starts.
	 *                     The method will search for the closing character
	 *                     combination that matches with this combination.
	 * @param toMatchOpen  The character combination that denotes the opening or
	 *                     introduction of a new nesting level.
	 * @param toMatchClose The character combination that denotes the closing or
	 *                     finalization of a nesting level.
	 * @return The index directly after the closing character combination that
	 *         matches the given opening character combination at the given index.
	 *         If an error occurs, such as not finding matching characters or
	 *         nesting level errors, the index returned is -1.
	 */
	public static Integer indexOfMatching(String toSearch, int indexOfFirst, String toMatchOpen, String toMatchClose) {
		return indexOfMatching(toSearch, indexOfFirst, Pattern.compile(Pattern.quote(toMatchOpen)),
				Pattern.compile(Pattern.quote(toMatchClose)));
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
