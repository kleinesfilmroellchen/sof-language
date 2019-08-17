package klfr.sof.lang;

import java.io.Serializable;

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

	public Stackable clone();
}
