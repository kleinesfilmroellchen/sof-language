package klfr.sof.lang;

import klfr.sof.exceptions.IncompleteCompilerException;

/**
 * Marker objects on the stack that are "transparent" to most operations.
 * This means that they just get discarded under most circumstances, but are used for
 * very specific operations, like currying behavior or list comprehension.
 * 
 * @author klfr
 */
public class TransparentData implements Stackable {

	/**
	 * The underlying type of transparent object.
	 * 
	 * @implNote This cannot be the Stackable itself (although it should be) because
	 *           the automatic Comparable implementation of enums clashes with the
	 *           Comparable implementation of Stackable.
	 */
	public enum TransparentType {
		/** Currently unused. */
		// ListStart("["),
		/**
		 * Function currying marker. A function cannot consume arguments past this
		 * marker on the stack.
		 */
		CurryPipe("|");

		private final String symbol;

		private TransparentType(final String symbol) {
			this.symbol = symbol;
		}

		public final String getSymbol() {
			return symbol;
		}

		public static final TransparentType fromSymbol(final String symbol) throws IncompleteCompilerException {
			return switch (symbol) {
				case "|" -> TransparentType.CurryPipe;
				default -> throw new IncompleteCompilerException("syntax");
			};
		}
	}

	private final TransparentType type;

	/**
	 * Returns the proper internal type of the transparent object.
	 * @return the proper internal type of the transparent object.
	 */
	public TransparentType getType() {
		return type;
	}

	/**
	 * Creates a new transparent object.
	 * @param type The type of transparent marker data this object is.
	 */
	public TransparentData(final TransparentType type) {
		this.type = type;
	}

	@Override
	public Stackable copy() {
		return new TransparentData(type);
	}

	@Override
	public boolean equals(Stackable other) {
		return other instanceof TransparentData ? ((TransparentData) other).type.equals(this.type) : false;
	}
}

/*  
The SOF programming language interpreter.
Copyright (C) 2019-2022  kleinesfilmröllchen

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

