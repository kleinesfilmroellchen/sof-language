package klfr.sof.ast;

import java.util.*;
import java.util.function.*;

import klfr.sof.SOFFile;
import klfr.sof.exceptions.CompilerException;
import klfr.sof.exceptions.IncompleteCompilerException;

/**
 * A list of nodes that are to be executed in order. This may be the main
 * program itself, or a code block literal.
 */
public class TokenListNode extends Node {
	private static final long serialVersionUID = 1L;

	private final List<Node> subNodes;

	public TokenListNode(List<Node> subNodes, int index, SOFFile source) {
		super(index, source);
		this.subNodes = subNodes;
	}

	@Override
	public Object cloneNode() throws CloneNotSupportedException {
		return new TokenListNode(subNodes, getCodeIndex(), getSource());
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
	public boolean forEach(Node.ForEachType action) throws CompilerException, IncompleteCompilerException {
		for (Node subnode : subNodes) {
			// run the action, if false was returned, return as well
			if (!action.exec(subnode))
				return false;
		}
		return true;
	}

	@Override
	public Iterator<Node> iterator() {
		return subNodes.iterator();
	}

	public int count() {
		return subNodes.size();
	}

	@Override
	public int nodeCount() {
		return subNodes.parallelStream().mapToInt(n -> n.nodeCount()).sum();
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
