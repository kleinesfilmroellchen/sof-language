package klfr.sof.lang.oop;

import klfr.sof.lang.*;

/**
 * An SOF object. This most importantly holds the nametable representing the
 * object's attributes.
 * 
 * @author klfr
 */
public class SObject implements Stackable {
	private static final long serialVersionUID = 1L;

	/**
	 * The attributes of the object.
	 */
	protected final MethodDelimiter attributes;

	/**
	 * Return the attributes of the object.
	 * @return the attributes of the object.
	 */
	public Nametable getAttributes() {
		return attributes;
	}

	public SObject() {
		this.attributes = new MethodDelimiter();
	}

	/** needed for copying */
	private SObject(MethodDelimiter nt) {
		this.attributes = nt;
	}

	public String toDebugString(DebugStringExtensiveness e) {
		return switch (e) {
			case Full -> "Object" + System.lineSeparator() + this.attributes.toDebugString(e);
			case Compact -> "Obj(" + this.attributes.toDebugString(e) + ")";
			default -> this.attributes.toDebugString(e);
		};
	}

	@Override
	public Stackable copy() {
		return new SObject(this.attributes);
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
