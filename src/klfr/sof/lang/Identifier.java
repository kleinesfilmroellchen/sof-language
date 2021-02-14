package klfr.sof.lang;

import klfr.sof.*;
import klfr.sof.exceptions.CompilerException;
import klfr.sof.exceptions.IncompleteCompilerException;

/**
 * Identifiers are a type of stackable (i.e. basic SOF value) that are used to
 * identify functions, values, namespaces etc.<br>
 * The most common use of an identifier is as nametable keys, i.e. identifier
 * are the method of referring to the contents of nametables.
 * 
 * @author klfr
 */
@StackableName("Identifier")
public class Identifier implements Stackable {

	private static final long serialVersionUID = 1L;

	/** The identifier text symbol. */
	private String value;

	/**
	 * Returns the value string represented by this identifier.
	 * 
	 * @return the value string represented by this identifier.
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Constructs an identifier with the string value.
	 * 
	 * @param value The string to be used for the identifier.
	 * @throws IncompleteCompilerException If the given string value is not a valid SOF
	 *                                      identifier.
	 */
	public Identifier(String value) throws IncompleteCompilerException {
		value = value.trim();
		if (!isValidIdentifier(value))
			throw new IncompleteCompilerException("syntax", "syntax.identifier", value);
		this.value = value;
	}

	@Override
	public String toDebugString(DebugStringExtensiveness e) {
		return switch (e) {
			case Full -> "Identifier(" + value + ")";
			case Compact -> value;
			case Type -> Stackable.toDebugString(this, e);
		};
	}

	@Override
	public String print() {
		return value;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof Identifier ? ((Identifier) other).value.equals(value) : false;
	}

	@Override
	public boolean equals(Stackable other) {
		return other instanceof Identifier ? ((Identifier) other).value.equals(value) : false;
	}

	@Override
	public Stackable copy() {
		// this cannot throw b/c otherwise this identifer couldn't have been created
		try {
			return new Identifier(this.value);
		} catch (IncompleteCompilerException e) { throw new RuntimeException(); }
	}

	public int hashCode() {
		// change this from the string hashcode to not get hashtable collisions when
		// strings are attempted to be used as keys
		return this.value.hashCode() ^ 0xFF00FF00;
	}

	/**
	 * Checks whether the given string would be a valid SOF identifier character sequence.
	 * <br/><br/>
	 * The actual check is done with the identifier pattern.
	 * @param id The string to check.
	 * @return Whether the given string would be a valid SOF identifier character sequence.
	 * @see klfr.sof.Patterns#identifierPattern
	 */
	public static boolean isValidIdentifier(String id) {
		return Patterns.identifierPattern.matcher(id).matches();
	}

	@Override
	public int compareTo(Stackable other) {
		if (other instanceof Identifier)
			return this.getValue().compareTo(((Identifier) other).getValue());
		throw new ClassCastException(
				"Cannot compare Identifier " + this.toString() + " to " + other.getClass().toString());
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
