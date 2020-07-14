package klfr.sof.lang;

import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Logger;

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
public abstract class Primitive implements Callable {
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
		switch (e) {
			case Compact:
				if (v() instanceof String)
					return this.typename() + ":\"" + v().toString() + '"';
				return this.typename() + ':' + v().toString();
			default:
				return Stackable.toDebugString(this, e);
		}
	}

	@Override
	public CallProvider getCallProvider() {
		final Primitive self = this;
		return (interpreter) -> self.copy();
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
