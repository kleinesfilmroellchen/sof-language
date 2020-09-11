package klfr.sof.ast;

import java.util.*;
import java.util.function.*;

/**
 * A list of nodes that are to be executed in order. This may be the main
 * program itself, or a code block literal.
 */
public class TokenListNode implements Node {
	private static final long serialVersionUID = 1L;

	private final List<Node> subNodes;
	private final int index;
	private final String code;

	@Override
	public int getCodeIndex() {
		return index;
	}
	@Override
	public String getCode() {
		return code;
	}

	public TokenListNode(List<Node> subNodes, int index, String code) {
		this.subNodes = subNodes;
		this.index = index;
		this.code = code;
	}

	@Override
	public Object cloneNode() throws CloneNotSupportedException {
		return new TokenListNode(subNodes, index, code);
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof TokenListNode ? ((TokenListNode) obj).subNodes.equals(this.subNodes) : false;
	}

	@Override
	public int hashCode() {
		return subNodes.hashCode() ^ 0x23ff8912;
	}

	@Override
	public String toString() {
		return "Tokens: {"
				+ subNodes.stream().map(elt -> elt.toString()).reduce("",
						(a, b) -> a + System.lineSeparator() + "\t" + b.replaceAll("\n", "\n\t"))
				+ System.lineSeparator() + "} @ " + this.getCodeIndex();
	}

	/**
	 * Run the specified action on every subnode.
	 */
	@Override
	public void forEach(Function<? super Node, Boolean> action) {
		for (Node subnode : subNodes) {
			// run the action, if false was returned, return as well
			if (!action.apply(subnode))
				return;
		}
	}

	@Override
	public Iterator<Node> iterator() {
		return subNodes.iterator();
	}

	public int count() {
		return subNodes.size();
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
