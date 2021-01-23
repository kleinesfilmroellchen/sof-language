package klfr.sof.lang.functional;

import klfr.sof.ast.TokenListNode;
import klfr.sof.lang.*;

@StackableName("Codeblock")
public class CodeBlock implements Stackable {
	private static final long serialVersionUID = 1L;

	public final TokenListNode code;

	public CodeBlock(TokenListNode code) {
		this.code = code;
	}

	@Override
	public String toDebugString(DebugStringExtensiveness e) {
		String code = this.code.toString();
		// Java Switch Expression FTW!
		return switch (e) {
			case Full -> String.format("[CodeBlock { %s } %h]", code, hashCode());
			case Compact -> "[CodeBlk " + this.code.count() + "n ]";
			default -> Stackable.toDebugString(this, e);
		};
	}

	@Override
	public Stackable copy() {
		return new CodeBlock(this.code);
	}

	@Override
	public String print() {
		return "{ Codeblock }";
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof CodeBlock ? this.equals((CodeBlock)obj): false;
	}

	@Override
	public boolean equals(Stackable other) {
		if (other instanceof CodeBlock) {
			return code.equals(((CodeBlock)other).code);
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
