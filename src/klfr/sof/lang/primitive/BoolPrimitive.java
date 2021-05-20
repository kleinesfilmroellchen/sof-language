package klfr.sof.lang.primitive;

import klfr.sof.exceptions.IncompleteCompilerException;
import klfr.sof.lang.*;

/**
 * A boolean primitive of SOF.
 * 
 * @author klfr
 */
@StackableName("Boolean")
public class BoolPrimitive extends Primitive {
	private static final long serialVersionUID = 1L;
	/** The boolean that is represented by this primitive. */
	private final Boolean value;

	private static final BoolPrimitive FALSE = new BoolPrimitive(false);
	private static final BoolPrimitive TRUE = new BoolPrimitive(true);

	private BoolPrimitive(Boolean value) {
		this.value = value;
	}

	@Override
	public Object v() {
		return value;
	}

	/**
	 * Returns the boolean value represented by this primitive.
	 * @return The boolean value represented by this primitive.
	 */
	public Boolean value() {
		return value;
	}

	/**
	 * Create a new boolean primitive from the given boolean.
	 * @param value the boolean value to wrap in a primitive.
	 * @return a new boolean primitive with the value of the given boolean.
	 */
	public static BoolPrimitive createBoolPrimitive(Boolean value) {
		return value ? TRUE : FALSE;
	}

	/**
	 * Parse a new boolean primitive from the given boolean string.
	 * 
	 * @param booleanString The string to parse a boolean from.
	 * @return a new boolean primitive that has the value represented by the string.
	 * @throws IncompleteCompilerException If the string is neither "true" nor "false".
	 */
	public static BoolPrimitive createBoolFromString(String booleanString) throws IncompleteCompilerException {
		if (booleanString.toLowerCase().equals("true"))
			return TRUE;
		if (booleanString.toLowerCase().equals("false"))
			return FALSE;
		throw new IncompleteCompilerException("syntax", "boolean.syntax", booleanString);
	}

	@Override
	public boolean equals(Stackable other) {
		if (other instanceof BoolPrimitive otherBool) {
			return otherBool.value == this.value;
		}
		return false;
	}

	/**
	 * Returns whether the value represented by this boolean primitive is true.
	 * 
	 * @return whether the value represented by this boolean primitive is true.
	 */
	@Override
	public boolean isTrue() {
		return this.value;
	}

	/**
	 * Returns whether the value represented by this boolean primitive is false.
	 * 
	 * @return whether the value represented by this boolean primitive is false.
	 */
	@Override
	public boolean isFalse() {
		return !this.value;
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
