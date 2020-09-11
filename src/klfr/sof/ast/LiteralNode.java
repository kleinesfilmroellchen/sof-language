package klfr.sof.ast;

import java.util.*;

import klfr.sof.lang.Stackable;
import klfr.sof.lang.Stackable.DebugStringExtensiveness;

/**
 * A node that holds a simple Stackable to be placed on the stack.
 */
public class LiteralNode implements Node {
	private static final long serialVersionUID = 1L;

	private final Stackable value;
	private final int codeIndex;
	private final String code;
	
	@Override
	public int getCodeIndex() {
		return codeIndex;
	}
	@Override
	public String getCode() {
		return code;
	}

	public Stackable getValue() {
		return value;
	}

	public LiteralNode(final Stackable data, final int codeIndex, final String code) {
		this.value = data;
		this.codeIndex = codeIndex;
		this.code = code;
	}

	@Override
	public Object cloneNode() {
		return new LiteralNode(value, codeIndex, code);
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof LiteralNode ? ((LiteralNode) obj).value.equals(this.value) : false;
	}

	@Override
	public int hashCode() {
		return value.hashCode() ^ 0x117e9a1;
	}

	@Override
	public String toString() {
		return "Literal: " + value.toDebugString(DebugStringExtensiveness.Type) + " [ " + value.toDebugString(DebugStringExtensiveness.Full) + " ] @ " + this.codeIndex;
	}

	@Override
	public Iterator<Node> iterator() {
		return List.<Node>of(this).iterator();
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
