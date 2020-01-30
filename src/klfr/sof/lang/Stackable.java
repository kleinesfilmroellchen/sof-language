package klfr.sof.lang;

import java.io.Serializable;

/**
 * Stackable is the name for all elements that can be put onto the SOF stack,
 * meaning that every valid value in SOF is a Stackable. Stackables only have a
 * couple of basic methods by default, as Stackable is the root of the SOF type
 * hierarchy and therefore very general.
 */
public interface Stackable extends Serializable {
	/**
	 * Returns a string that represents this stackable in a debug view, in the most
	 * extended form.
	 */
	public String getDebugDisplay();

	/**
	 * Returns a string that represents this stackable in a concise manner, but
	 * still in a 'debuggy' way, so with more information than for normal output.
	 */
	public String toString();

	/**
	 * Returns a string that represents this stackable for output with 'write' etc.
	 * By default this method will simply return the same result as
	 * {@code toString()}, but implementors are encouraged to change this behavior.
	 */
	public default String toOutputString() {
		return this.toString();
	}

	/**
	 * Every Stackable must be able to clone itself.
	 * 
	 * @return An exact copy of this Stackable.
	 */
	public Stackable clone();
}
