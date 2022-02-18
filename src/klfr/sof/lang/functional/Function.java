package klfr.sof.lang.functional;

import klfr.sof.ast.TokenListNode;
import klfr.sof.lang.*;

/**
 * Function type, one of the most important callable types. Functions are the most primitive scoped callable.
 */
@StackableName("Function")
public class Function extends CodeBlock {

	private static final long	serialVersionUID	= 1L;

	/**
	 * The number of arguments that this function has.
	 */
	public final long				arguments;

	/**
	 * The global nametable of the global scope that this function was created in. This is especially relevant for module
	 * functions.
	 */
	protected final Nametable	globalNametable;

	/**
	 * Returns the global nametable of this function's global scope.
	 * 
	 * @return The global nametable of this function's global scope.
	 */
	public final Nametable getGlobalNametable() {
		return globalNametable;
	}

	/**
	 * Create a function with this code, with given numbers of arguments.
	 * 
	 * @param code            code of the function
	 * @param arguments       number of arguments the function recieves
	 * @param globalNametable global nametable of the function's global scope.
	 */
	public Function(TokenListNode code, long arguments, Nametable globalNametable) {
		super(code);
		this.arguments = arguments;
		this.globalNametable = globalNametable;
	}

	@Override
	public String toDebugString(DebugStringExtensiveness e) {
		return switch (e) {
		case Compact -> String.format("[Function/%d %dn ]", this.arguments, this.code.count());
		case Full -> String.format("[Function/%d { %s } %h]", this.arguments, this.code, this.hashCode());
		case Type -> "Function";
		default -> Stackable.toDebugString(this, e);
		};
	}

	/**
	 * Create a new SOF function based on the given code block. This is a utility for the interpreter because functions in
	 * SOF programming are created from code blocks.
	 * 
	 * @param origin          The code block whose code is to be copied as this function's behavior.
	 * @param arguments       The number of arguments to this function.
	 * @param globalNametable global nametable of the function's global scope.
	 * @return A new SOF function with the given code block tokens as the behavior and the given number of arguments.
	 */
	public static Function fromCodeBlock(CodeBlock origin, int arguments, Nametable globalNametable) {
		return new Function(origin.code, arguments, globalNametable);
	}

	@Override
	public String print() {
		return String.format("{ %d argument Function }", this.arguments);
	}

	@Override
	public Stackable clone() {
		return new Function(this.code, this.arguments, this.globalNametable);
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Function ? this.equals((Function) obj) : false;
	}

	@Override
	public boolean equals(Stackable other) {
		if (other instanceof Function otherFunction) {
			return code.equals(otherFunction.code) && (arguments == otherFunction.arguments);
		} else {
			return false;
		}
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
