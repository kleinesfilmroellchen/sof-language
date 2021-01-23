package klfr.sof.lang.primitive;

import klfr.sof.exceptions.IncompleteCompilerException;
import klfr.sof.lang.*;

@StackableName("Boolean")
public class BoolPrimitive extends Primitive {
	private static final long serialVersionUID = 1L;
	private final Boolean value;

	private BoolPrimitive(Boolean value) {
		this.value = value;
	}

	@Override
	public Object v() {
		return value;
	}

	public Boolean value() {
		return value;
	}

	public static BoolPrimitive createBoolPrimitive(Boolean value) {
		return new BoolPrimitive(value);
	}

	public static BoolPrimitive createBoolFromString(String booleanString) throws IncompleteCompilerException {
		if (booleanString.toLowerCase().equals("true"))
			return new BoolPrimitive(true);
		if (booleanString.toLowerCase().equals("false"))
			return new BoolPrimitive(false);
		throw new IncompleteCompilerException("syntax", "boolean.syntax", booleanString);
	}

	@Override
	public boolean equals(Stackable other) {
		if (other instanceof BoolPrimitive) {
			return ((BoolPrimitive) other).value == this.value;
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
