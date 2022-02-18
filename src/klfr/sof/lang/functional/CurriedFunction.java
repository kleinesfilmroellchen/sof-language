package klfr.sof.lang.functional;

import java.util.List;

import klfr.sof.lang.*;

/**
 * A proxy function that can be executed like any other. In addition to holding the code, it also contains a list of
 * curried arguments. When executed, these arguments are added to the stack before any others.
 * 
 * @author klfr
 */
@StackableName("CurriedFunction")
public class CurriedFunction extends Function {

	/**
	 * The curried arguments. They are provided in the order that they are passed to the function, so the first element is
	 * the lowest on the stack of arguments passed into the function.
	 */
	private final List<Stackable> curriedArguments;

	/**
	 * Create a new curried function.
	 * 
	 * @param base             The function that is curried.
	 * @param curriedArguments The arguments that are curried to the function, in the order that they are supposed to be
	 *                            passed to the function.
	 * @param globalNametable  global nametable of the function's global scope.
	 */
	public CurriedFunction(Function base, List<Stackable> curriedArguments, Nametable globalNametable) {
		super(base.code, base.arguments - curriedArguments.size(), globalNametable);
		this.curriedArguments = curriedArguments;
	}

	/**
	 * Returns the curried arguments of this function, i.e. the arguments that are pre-stored for later.
	 * 
	 * @return The curried arguments of this function.
	 */
	public final List<Stackable> getCurriedArguments() {
		return curriedArguments;
	}

	/**
	 * Returns the regular function beneath this curried function.
	 * 
	 * @return The regular function beneath this curried function.
	 */
	public final Function getRegularFunction() {
		return new Function(this.code, this.arguments + this.curriedArguments.size(), this.globalNametable);
	}

	@Override
	public String toDebugString(DebugStringExtensiveness e) {
		return switch (e) {
		case Compact -> String.format("[CurriedFunction/%d-%d %dn ]", this.curriedArguments.size(), this.arguments, this.code.count());
		case Full -> String.format("[CurriedFunction/%d - %s { %s } %h]", this.arguments, this.curriedArguments.toString(), this.code, this.hashCode());
		case Type -> "CurriedFunction";
		default -> Stackable.toDebugString(this, e);
		};
	}

	@Override
	public String print() {
		return String.format("{ %d argument Function curried @ %d }", this.arguments, this.curriedArguments.size());
	}

	@Override
	public Stackable clone() {
		return new CurriedFunction(this, curriedArguments, globalNametable);
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof CurriedFunction ? this.equals((CurriedFunction) obj) : false;
	}

	@Override
	public boolean equals(Stackable other) {
		if (other instanceof CurriedFunction otherFunction) {
			return code.equals(otherFunction.code) && (arguments == otherFunction.arguments) && curriedArguments.equals(otherFunction.curriedArguments);
		} else {
			return false;
		}
	}

}

/*  
The SOF programming language interpreter.
Copyright (C) 2019-2022  kleinesfilmröllchen

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
