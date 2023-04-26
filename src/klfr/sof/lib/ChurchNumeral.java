package klfr.sof.lib;

import java.util.Formatter;

import klfr.sof.exceptions.IncompleteCompilerException;
import klfr.sof.lang.Stackable;
import klfr.sof.lang.primitive.IntPrimitive;

/**
 * Implements the callable version of integers, where each number is a callable verison of
 */
public final class ChurchNumeral implements Stackable {

	private final long value;

	public long value() {
		return value;
	}

	public ChurchNumeral(long value) {
		this.value = value;
	}

	@Override
	public boolean equals(Stackable obj) {
		if (obj instanceof ChurchNumeral numeral) {
			return numeral.value == this.value;
		} else if (obj instanceof IntPrimitive integer) {
			return integer.value() == this.value;
		}
		return false;
	}

	@Override
	public int compareTo(Stackable other) {
		if (other instanceof ChurchNumeral numeral) {
			return Long.valueOf(this.value).compareTo(numeral.value);
		} else if (other instanceof IntPrimitive integer) {
			return Long.valueOf(this.value).compareTo(integer.value());
		}
		throw new RuntimeException(new IncompleteCompilerException("type", "type.compare", this.typename(), other.typename()));
	}

	@Override
	public Stackable copy() {
		return new ChurchNumeral(this.value);
	}

	@Override
	public boolean isFalse() {
		return this.value == 0;
	}

	@Override
	public boolean isTrue() {
		return this.value != 0;
	}

	@Override
	public String print() {
		return String.format("{ Church numeral: %d }", this.value);
	}

	@Override
	public String toDebugString(DebugStringExtensiveness e) {
		return switch (e) {
		case Compact, Full -> String.format("[ Church Numeral %d ]", this.value);
		case Type -> this.typename();
		default -> Stackable.toDebugString(this, e);
		};
	}

	@Override
	public String typename() {
		return "Church Numeral";
	}

}
