package klfr.sof;

import java.io.Serializable;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.logging.Logger;
import java.util.regex.*;

import klfr.Tuple;

/**
 * The tokenizer is used by the parsing tools to quickly scan through SOF tokens. <br/>
 * <br/>
 * It wraps the terrible behavior of {@link Matcher}.
 * 
 * @author klfr
 */
public final class Tokenizer implements Iterator<String> {

	private Logger log = Logger.getLogger(this.getClass().getCanonicalName());

	/**
	 * Represents internal state of a tokenizer for transfering such states between objects and storing state as a file
	 * (hence the Serializable implementation).<br>
	 * <br>
	 * <strong>All of this classes properties are public to allow the SOF language internal utilities to modify them. It is
	 * strongly disregarded to make any modifications to a stored state object, as any modification might break the
	 * tokenizer; i.e. a tokenizer created from such a state will experience undocumented and indeterminate
	 * behavior.</strong>
	 * 
	 * @author klfr
	 */
	public static class TokenizerState implements Serializable {

		private static final long	serialVersionUID	= 1L;

		/** The start index in the code. */
		public int						start;
		/** The end index (exclusive) in the code. */
		public int						end;
		/** The start of the region that is set in the matcher. */
		public int						regionStart;
		/** The end of the region (exclusive) that is set in the matcher. */
		public int						regionEnd;
		/** The code that the tokenizer operates on. */
		public String					code;

		/**
		 * Create a new tokenizer state with the given parameters.
		 * 
		 * @param start       The start index.
		 * @param end         The end index.
		 * @param regionStart The region start index for the matcher.
		 * @param regionEnd   The region end index for the matcher.
		 * @param code        The code to operate on.
		 */
		public TokenizerState(int start, int end, int regionStart, int regionEnd, String code) {
			super();
			this.start = start;
			this.end = end;
			this.regionStart = regionStart;
			this.regionEnd = regionEnd;
			this.code = code;
		}

		@Override
		public TokenizerState clone() {
			return new TokenizerState(this.start, this.end, this.regionStart, this.regionEnd, this.code);
		}

		@Override
		public boolean equals(Object other) {
			return other instanceof TokenizerState ? equals((TokenizerState) other) : false;
		}

		/**
		 * Checks equality on this tokenizer and the other tokenizer.
		 * 
		 * @param other The other tokenizer to compare to.
		 * @return Whether the two tokenizers are equal.
		 */
		public final boolean equals(TokenizerState other) {
			return other.start == this.start && other.end == this.end && other.regionStart == this.regionStart && other.regionEnd == this.regionEnd && other.code.equals(this.code);
		}

		@Override
		public String toString() {
			return String.format("TokenizerState(%d->%d, r%d->%d [%s])", this.start, this.end, this.regionStart, this.regionEnd,
					this.code.replace("\n", "\\n").replace("\r", "\\r").replace("\b", "\\b").replace("\t", "\\t").replace("\f", "\\f"));
		}
	}

	private Matcher			m;
	/**
	 * stores the current state of the tokenizer
	 */
	private TokenizerState	currentState;
	/** Stores the last token that was found by the match methods */
	private String				lastMatchedToken;

	/**
	 * Returns the index of the last matched token.
	 * 
	 * @return The index of the last matched token.
	 */
	public final int start() {
		return this.currentState.start;
	}

	/**
	 * Constructor for factory methods, do not use externally.
	 */
	private Tokenizer(String code) {
		this.m = Patterns.tokenPattern.matcher(code);
		this.currentState = new TokenizerState(0, 0, 0, code.length(), code);
	}

	/**
	 * Returns the code that this tokenizer operates on.
	 * 
	 * @return The code that this tokenizer operates on.
	 */
	public final String getCode() {
		return this.currentState.code;
	}

	/**
	 * Creates a new tokenizer from a source code string. The tokenizer's state is set up to start scanning the code from
	 * the beginning.
	 * 
	 * @param code The SOF source code to be used with this Tokenizer.
	 * @return A new tokenizer.
	 */
	public static Tokenizer fromSourceCode(String code) {
		return new Tokenizer(code);
	}

	/**
	 * Creates a tokenizer from the given saved tokenizer state. This method does not guarantee to return a functioning
	 * tokenizer as the tokenizer state might be corrupted.
	 * 
	 * @param state the state to create a tokenizer from
	 * @return a new tokenizer without a state stack
	 */
	public static Tokenizer fromState(TokenizerState state) {
		Tokenizer t = new Tokenizer(state.code);
		t.currentState = state;
		return t;
	}

	/**
	 * Create a state object that represents the current full internal state of the Tokenizer excluding its saved state
	 * stack. The new state object is fully independed from this tokenizer.<br>
	 * <br>
	 * This method is to be used together with {@code Tokenizer.fromState()} to recreate the tokenizer later on or clone it.
	 * 
	 * @return a new, independed TokenizerState
	 */
	public final TokenizerState getState() {
		return this.currentState.clone();
	}

	/**
	 * Sets the state's parameters on this tokenizer.
	 * 
	 * @param state The state to be set.
	 * @deprecated This method will easily break the tokenizer's proper behavior.
	 */
	@Deprecated
	public final void setState(TokenizerState state) {
		this.currentState = state;
		this.m = Patterns.tokenPattern.matcher(this.currentState.code);
		this.m.region(state.regionStart, state.regionEnd);
	}

	/**
	 * Returns the current execution position of the Tokenizer, as a (Line, Index) number tuple. While the line number is
	 * one-based (as in text editors), the index (inside the line) is zero-based (as in strings).
	 * 
	 * @return A tuple with two integers that represent the line position and index inside the line; see above notes.
	 */
	public final Tuple<Integer, Integer> getCurrentPosition() {
		Matcher linefinder = Patterns.nlPat.matcher(getCode());
		int realIndex = this.start(), linenum = 0, lineStart = 0;
		// increment line number while the text index is still after the searched line
		// beginning
		while (linefinder.find() && realIndex > linefinder.start() - 1) {
			++linenum;
			lineStart = linefinder.start() + 1;
		}
		// linenum -1 because we advanced past the actual line
		log.fine(String.format("tuple current index %d computed to line %d starting at %d line-inside-index %d", realIndex, linenum, lineStart, realIndex - (lineStart - 1)));
		return new Tuple<>(linenum, realIndex - (lineStart - 1));
	}

	@Override
	public Tokenizer clone() {
		return Tokenizer.fromState(this.getState());
	}

	/**
	 * Returns whether the tokenizer has exceeded its searching region.
	 * 
	 * @return whether the tokenizer has exceeded its searching region.
	 */
	private final boolean regionExceeded() {
		return this.currentState.regionEnd < Math.max(this.currentState.start, this.currentState.end) || this.currentState.regionStart > Math.min(this.currentState.start, this.currentState.end);
	}

	/**
	 * Returns whether the tokenizer can provide more tokens.
	 * 
	 * @return whether the tokenizer can provide more tokens.
	 */
	public final boolean hasNext() {
		return findNextToken(false);
	}

	/**
	 * Performs region- and state-safe find on the matcher from the given index.
	 * 
	 * @param advance Whether to actually store the new findings. hasNext(), for example, will set this to false to not
	 *                   change the state on repeated invocations.
	 */
	private final boolean findNextToken(boolean advance) {
		if (this.regionExceeded())
			return false;
		// whether there are more tokens to be found: perform one additional match
		var hasMore = this.m.find(this.currentState.end);
		final int prevEnd = this.currentState.end, prevStart = this.currentState.start;
		if (hasMore) {
			// in this case, use the matcher's finding bounds
			this.currentState.end = this.m.end();
			this.currentState.start = this.m.start();
			// check if any of the new finds are outside of the region
			if (this.regionExceeded()) {
				if (!advance) {
					this.currentState.end = prevEnd;
					this.currentState.start = prevStart;
				}
				return false;
			}
			// store the matched token for the other methods to use
			if (advance)
				this.lastMatchedToken = this.m.group();
		}
		// otherwise, we hit the end, position the last match at the end of the code
		else
			this.currentState.end = this.currentState.code.length();

		if (!advance) {
			this.currentState.end = prevEnd;
			this.currentState.start = prevStart;
		}
		return hasMore;
	}

	/**
	 * Finds and returns the next token, or an empty string if there is no next token.
	 * 
	 * @return the next token, or an empty string if there is no next token.
	 */
	@Override
	public String next() {
		if (!this.findNextToken(true))
			throw new NoSuchElementException("No more tokens");
		return this.lastMatchedToken;
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
