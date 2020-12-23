package klfr.sof.lang;

import java.io.Serializable;
import java.util.Formattable;
import java.util.FormattableFlags;
import java.util.Formatter;

import klfr.sof.exceptions.IncompleteCompilerException;

/**
 * Stackable is the name for all elements that can be put onto the SOF stack,
 * meaning that every valid value in SOF is a Stackable. Stackables only have a
 * couple of basic methods by default, as Stackable is the root of the SOF type
 * hierarchy and therefore very general.
 */
@StackableName("Value")
public interface Stackable extends Serializable, Cloneable, Comparable<Stackable>, Formattable {

	/**
	 * An enum which represents the different extensivenesses for debug strings of
	 * Stackables. Each extensiveness has a character limit and Stackables should
	 * only return that many characters for their debug string in that particular
	 * extensiveness. The main use of this enum is in
	 * {@link Stackable#toDebugString(DebugStringExtensiveness)}.
	 */
	public static enum DebugStringExtensiveness {
		/**
		 * Most extensive debug string, to be used when viewing individual values. No
		 * length restriction.
		 */
		Full("Most extensive debug string, to be used when viewing individual values. No length restriction.",
				Integer.MAX_VALUE),
		/**
		 * Compact debug string with important information, to be used in value lists
		 * like Stack and NTs.
		 */
		Compact("Compact debug string with important information, to be used in value lists like Stack and NTs.", 30),
		/** Only show internal type descriptor; shortest debug string. */
		Type("Only show internal type descriptor; shortest debug string.", 10);

		public final String description;
		public final int maxlength;

		private DebugStringExtensiveness(String description, int maxlength) {
			this.description = description;
			this.maxlength = maxlength;
		}

		/**
		 * Enforces this extensiveness' length restriction onto the given string by
		 * trimming from the end.
		 */
		public String ensureLength(String s) {
			return s.substring(0, Math.min(s.length(), maxlength));
		}
	}

	/**
	 * Returns the debug string for this Stackable. The extensiveness of the string
	 * is determined by the enum parameter.
	 * 
	 * @param e The debug string extensiveness. Refer to the enum documentation for
	 *          information about the different extensivenesses.
	 * @return A debug string for displayal to the SOF interpreter system developer,
	 *         as in logger output, describe(s) PT calls etc.
	 */
	public default String toDebugString(DebugStringExtensiveness e) {
		return toDebugString(this, e);
	}

	/**
	 * Returns the (default implementation) debug string for a Stackable. The
	 * extensiveness of the string is determined by the enum parameter.
	 * 
	 * @param e The debug string extensiveness. Refer to the enum documentation for
	 *          information about the different extensivenesses.
	 * @param s The Stackable to compute the debug string for.
	 * @return A debug string for displayal to the SOF interpreter system developer,
	 *         as in logger output, describe(s) PT calls etc.
	 */
	public static String toDebugString(Stackable s, DebugStringExtensiveness e) {
		// THE SWITCH EXPRESSIONS ARE HERE AND I LOVE THEM
		return switch (e) {
			case Type -> e.ensureLength(s.typename());
			case Compact, Full -> e.ensureLength(String.format("[%s %h]", s.getClass().getSimpleName(), s.hashCode()));
		};
	}

	/**
	 * Return the end-user representation of this Stackable, to be used for printing
	 * and converting to String type (StringPrimitive).
	 * 
	 * @return the end-user representation of this Stackable, to be used for
	 *         printing and converting to String type (StringPrimitive).
	 */
	public default String print() {
		return this.toDebugString(DebugStringExtensiveness.Full);
	}

	@Override
	public default void formatTo(Formatter fmt, int f, int width, int precision) {
		fmt.format(
				"%" + ((f & FormattableFlags.LEFT_JUSTIFY) > 0 ? "-" : "") + (width > 0 ? width : "")
						+ (precision > 0 ? "." + precision : "") + ((f & FormattableFlags.UPPERCASE) > 0 ? "S" : "s"),
				(f & FormattableFlags.ALTERNATE) > 0 ? this.print() : this.toString());
	}

	/**
	 * Every Stackable must be able to make a copy of itself.
	 * 
	 * @return An exact copy of this Stackable.
	 */
	public Stackable copy();

	/**
	 * Returns the internal type name of this Stackable given with the StackableName
	 * annotation.
	 * 
	 * @return the internal type name of this Stackable given with the StackableName
	 *         annotation.
	 */
	public default String typename() {
		return this.getClass().getAnnotation(StackableName.class) != null
				? this.getClass().getAnnotation(StackableName.class).value()
				: "<Anonymous>";
	}

	/**
	 * Checks the stackables for logical SOF-internal equality. This check should be
	 * type-agnostic, meaning that incompatible types are always considered unequal. This
	 * is the behavior of the default fallback implementation.<br>
	 * <br>
	 * As for usual with equality, this check should be commutative, i.e.
	 * {@code a.equals(b) == b.equals(a)}.
	 * 
	 * @param other Other stackable to check against
	 * @return true if the Stackables are considered equal according to SOF logic,
	 *         false if they aren't.
	 */
	public default boolean equals(Stackable other) {
		return false;
	}

	/**
	 * Compares this stackable to another stackable, with the ususal Comparable
	 * rules: greater than zero if self greater than other.
	 */
	@Override
	public default int compareTo(Stackable o) {
		throw new RuntimeException(new IncompleteCompilerException("type", "type.compare", this.typename(), o.typename()));
	}

	/**
	 * Returns whether this Stackable's data can be considered truthy. This is
	 * commonly the case with most values except "falsy" exceptions. The default
	 * implementation always returns true.
	 * 
	 * @return whether this Stackable's data can be considered truthy.
	 */
	public default boolean isTrue() {
		return true;
	}

	/**
	 * Returns whether this Stackable's data can be considered falsy. This is most
	 * commonly the case with a few "falsy" exceptions, such as the number 0, the
	 * empty string, the boolean false and any Nothing-like value. The default
	 * implementation always returns false.
	 * 
	 * @return whether this Stackable's data can be considered falsy.
	 */
	public default boolean isFalse() {
		return false;
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
