package klfr.sof.lang.primitive;

import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Logger;

import klfr.sof.lang.Stackable;
import klfr.sof.lang.Stackable.DebugStringExtensiveness;

/**
 * Primitive values in SOF are represented through this interface and its
 * subtypes.<br>
 * <br>
 * Subclasses shall provide a method of the form
 * {@code static PSC createXXXPrimitive(T value)}, where T is the Java type
 * underlying the primitive, PSC is the subclass itself and XXX is the standard
 * name of the SOF primitive type.
 * 
 * @author klfr
 */
public abstract class Primitive implements Stackable {
	private static final long serialVersionUID = 1L;
	private static final Logger log = Logger.getLogger(Primitive.class.getCanonicalName());

	// all chars that can make up integers base 16 and lower, in ascending value
	public static final Map<Character, Integer> numberChars = new Hashtable<>();
	static {
		numberChars.put('0', 0);
		numberChars.put('1', 1);
		numberChars.put('2', 2);
		numberChars.put('3', 3);
		numberChars.put('4', 4);
		numberChars.put('5', 5);
		numberChars.put('6', 6);
		numberChars.put('7', 7);
		numberChars.put('8', 8);
		numberChars.put('9', 9);
		numberChars.put('a', 10);
		numberChars.put('b', 11);
		numberChars.put('c', 12);
		numberChars.put('d', 13);
		numberChars.put('e', 14);
		numberChars.put('f', 15);
	}

	/**
	 * Creates a new Primitive with auto-detecting the argument's type.
	 * 
	 * @param value the underlying value.
	 * @return a new Primitive.
	 */
	public Primitive createPrimitive(Object value) {
		// ifelse of doom - i hate java
		if (value instanceof Long || value instanceof Integer || value instanceof Short || value instanceof Byte) {
			return IntPrimitive.createIntPrimitive(((Number) value).longValue());
		} else if (value instanceof Boolean) {
			return BoolPrimitive.createBoolPrimitive((Boolean) value);
		} else if (value instanceof Float) {
			return FloatPrimitive.createFloatPrimitive(((Float) value).doubleValue());
		} else if (value instanceof Double) {
			return FloatPrimitive.createFloatPrimitive((Double) value);
		} else if (value instanceof CharSequence || value instanceof Character) {
			return StringPrimitive.createStringPrimitive(value.toString());
		}
		throw new ClassCastException(String.format("No matching type found for %s", value));
	}

	/**
	 * Returns the Primitive's internal value.
	 * 
	 * @return the Primitive's internal value.
	 */
	public abstract Object v();

	@Override
	public String toDebugString(DebugStringExtensiveness e) {
		// Yes, Java. You can do it.
		return switch (e) {
			case Full, Compact -> {
				if (v() instanceof String)
					yield this.typename() + ":\"" + v().toString() + '"';
				yield this.typename() + ':' + v().toString();
			}
			default -> Stackable.toDebugString(this, e);
		};
	}

	@Override
	public Stackable copy() {
		return this.createPrimitive(this.v());
	}

	@Override
	public String print() {
		return v().toString();
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
