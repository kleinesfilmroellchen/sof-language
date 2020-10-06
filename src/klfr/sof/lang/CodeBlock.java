package klfr.sof.lang;

import klfr.sof.ast.TokenListNode;

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
		switch (e) {
			case Full:
				return String.format("[CodeBlock { %s } %h]", code, hashCode());
			case Compact:
				return "[CodeBlk " + this.code.count() + "n ]";
			default:
				return Stackable.toDebugString(this, e);
		}
	}

	@Override
	public Stackable copy() {
		return new CodeBlock(this.code);
	}

	@Override
	public String print() {
		return "{ Codeblock }";
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
