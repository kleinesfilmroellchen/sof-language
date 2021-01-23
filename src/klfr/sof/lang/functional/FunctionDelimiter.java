package klfr.sof.lang.functional;

import java.util.Optional;

import klfr.sof.lang.*;

/**
 * A special nametable that marks the end of a function scope, also called FNT
 * 
 * @author klfr
 * @version 0.1a1
 *
 */
@StackableName("Function Nametable")
public class FunctionDelimiter extends Nametable {
	private static final long serialVersionUID = 1L;
	public Optional<Stackable> returnValue = Optional.empty();

	@Override
	public Nametable setReturn(Stackable value) {
		this.returnValue = Optional.ofNullable(value);
		return this;
	}

	/**
	 * Pushes the return value of this function delimiter to the given stack. If
	 * this function delimiter never recieved a return value, don't push anything.
	 * 
	 * @param toPushTo The stack to modify.
	 * @return Whether the stack was modified, i.e. whether there was a return
	 *         value.
	 */
	public boolean pushReturnValue(Stack toPushTo) {
		if (returnValue.isPresent()) {
			toPushTo.push(returnValue.get());
			return true;
		}
		return false;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof FunctionDelimiter ? this.equals((FunctionDelimiter)obj) : false;
	}

	@Override
	public boolean equals(Stackable other) {
		if (other instanceof FunctionDelimiter) {
			return super.equals(other) && this.returnValue.equals(((FunctionDelimiter)other).returnValue);
		} else {
			return false;
		}
	}

	@Override
	public String print() {
		return "[Function Nametable (" + this.size() + " entries) ]";
	}

	@Override
	public String toDebugString(DebugStringExtensiveness e) {
		return switch(e) {
			case Full -> "Function Nametable: return " + returnValue.toString() + System.lineSeparator() + super.toDebugString(e);
			case Compact -> "FNT[" + this.size() + "](" + returnValue.toString() + ")";
			default -> Stackable.toDebugString(this, e);
		};
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
