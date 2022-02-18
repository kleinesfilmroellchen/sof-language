package klfr.sof.lang.oop;

import klfr.sof.lang.*;

/**
 * An SOF object. This most importantly holds the nametable representing the object's attributes.
 * 
 * @author klfr
 */
@StackableName("Object")
public final class Object implements Stackable {

	private static final long			serialVersionUID	= 1L;

	/**
	 * The attributes of the object.
	 */
	protected final MethodDelimiter	attributes;

	/**
	 * Return the attributes of the object.
	 * 
	 * @return the attributes of the object.
	 */
	public final Nametable getAttributes() {
		return attributes;
	}

	/**
	 * Create a new SOF object.
	 */
	public Object() {
		this.attributes = new MethodDelimiter();
	}

	/** needed for copying */
	private Object(MethodDelimiter nt) {
		this.attributes = nt;
	}

	public final String toDebugString(DebugStringExtensiveness e) {
		return switch (e) {
		case Full -> "Object" + System.lineSeparator() + this.attributes.toDebugString(e);
		case Compact -> "Obj(" + this.attributes.toDebugString(e) + ")";
		default -> this.attributes.toDebugString(e);
		};
	}

	@Override
	public Stackable copy() {
		return new Object(this.attributes);
	}

	@Override
	public int hashCode() {
		return attributes.hashCode() ^ 0x9f7193da;
	}

	/**
	 * SOF object equality is structural equality. This means that objects with the same attributes and their values are
	 * considered equal.
	 * 
	 * @param other The other stackable that this one is compared against.
	 * @return Whether the objects are equal according to structural equality.
	 */
	@Override
	public boolean equals(Stackable other) {
		if (other instanceof Object otherObject) {
			return attributes.equals(otherObject.attributes);
		} else {
			return false;
		}
	}

	@Override
	public String print() {
		return "[Object]";
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
