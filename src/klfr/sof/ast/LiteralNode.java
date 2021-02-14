package klfr.sof.ast;

import java.util.*;

import klfr.sof.SOFFile;
import klfr.sof.lang.Stackable;
import klfr.sof.lang.Stackable.DebugStringExtensiveness;

/**
 * A node that holds a simple Stackable to be placed on the stack.
 */
public class LiteralNode extends Node {
	private static final long serialVersionUID = 1L;

	/** The value that this literal node represents and places on the stack. */
	private final Stackable value;

	/**
	 * Return the SOF data that this literal represents.
	 * @return the SOF data that this literal represents.
	 */
	public Stackable getValue() {
		return value;
	}

	/**
	 * Create a new literal node.
	 * @param data The SOF data that this literal represents.
	 * @param codeIndex The index in the code where this literal was parsed from.
	 * @param source The source code unit where this literal resides.
	 */
	public LiteralNode(final Stackable data, final int codeIndex, final SOFFile source) {
		super(codeIndex, source);
		this.value = data;
	}

	@Override
	public Node cloneNode() {
		return new LiteralNode(value.copy(), getCodeIndex(), getSource());
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
		return "Literal: " + value.toDebugString(DebugStringExtensiveness.Type) + " [ " + value.toDebugString(DebugStringExtensiveness.Full) + " ] @ " + this.getCodeIndex();
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
