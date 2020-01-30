package klfr.sof.lang;

import klfr.sof.CompilerException;
import klfr.sof.Interpreter;

/**
 * Identifiers are a type of stackable (i.e. basic SOF value) that are used to
 * identify functions, values, namespaces etc.<br>
 * The most common use of an identifier is as nametable keys, i.e. identifier
 * are the method of referring to the contents of nametables.
 * 
 * @author klfr
 */
public class Identifier implements Stackable {

	private static final long serialVersionUID = 1L;

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
	 * @throws CompilerException If the given string value is not a valid SOF
	 *                           identifier.
	 */
	public Identifier(String value) throws CompilerException {
		value = value.trim();
		if (!isValidIdentifier(value))
			throw CompilerException.fromIncompleteInfo("Syntax", "\"" + value + "\" is not a valid identifier");
		this.value = value;
	}

	@Override
	public String getDebugDisplay() {
		// literally this is it
		return value;
	}

	public String toString() {
		return getDebugDisplay();
	}

	@Override
	public boolean equals(Object other) {
		return Identifier.class.isAssignableFrom(other.getClass()) ? ((Identifier) other).value.equals(value) : false;
	}

	@Override
	public Stackable clone() {
		return new Identifier(this.value);
	}

	public int hashCode() {
		// change this from the string hashcode to not get hashtable collisions when
		// strings are attempted to be used as keys
		return this.value.hashCode() ^ 0xFF00FF00;
	}

	public static boolean isValidIdentifier(String id) {
		return Interpreter.identifierPattern.matcher(id).matches();
	}

}
