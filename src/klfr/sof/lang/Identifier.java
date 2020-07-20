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
@StackableName("Identifier")
public class Identifier implements Callable {

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
			throw new CompilerException.Incomplete("syntax", "syntax.identifier", value);
		this.value = value;
	}

	@Override
	public String toDebugString(DebugStringExtensiveness e) {
		switch (e) {
			case Full:
				return "Identifier(" + value + ")";
			case Compact:
				return value;
			case Type:
			default:
				return Stackable.toDebugString(this, e);
		}
	}

	@Override
	public String print() {
		return value;
	}

	@Override
	public boolean equals(Stackable other) {
		return other instanceof Identifier ? ((Identifier) other).value.equals(value) : false;
	}

	@Override
	public Stackable copy() {
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

	@Override
	@SuppressWarnings("deprecation")
	public CallProvider getCallProvider() {
		return interpreter -> {
			// look up this identifier in the innermost nametable
			final var value = interpreter.internal.stack().lookup(this);
			// if no mapping, throw error
			if (value == null) {
				throw CompilerException.fromCurrentPosition(interpreter.internal.tokenizer(), "name", null, this);
			}
			return value;
		};
	}

	@Override
	public int compareTo(Stackable other) {
		if (other instanceof Identifier)
			return this.getValue().compareTo(((Identifier) other).getValue());
		throw new ClassCastException(
				"Cannot compare Identifier " + this.toString() + " to " + other.getClass().toString());
	}

}
