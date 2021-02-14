package klfr.sof.ast;

import java.util.*;
import java.util.stream.Collectors;

import klfr.sof.SOFFile;
import klfr.sof.exceptions.CompilerException;
import klfr.sof.exceptions.IncompleteCompilerException;

/**
 * A list of AST nodes that are to be executed in order. This may be the main
 * program itself, a code block literal or other callables.
 */
public class TokenListNode extends Node {
	private static final long serialVersionUID = 1L;

	/** The list of nodes that are contained in this token list. */
	private final List<Node> subNodes;

	/**
	 * Create a new list of SOF tokens.
	 * @param subNodes The list of AST nodes to be contained in this list of nodes.
	 * @param index The index inside the source code where this token list is located.
	 * @param source The SOF source file unit where this token list comes from.
	 */
	public TokenListNode(final List<Node> subNodes, final int index, final SOFFile source) {
		super(index, source);
		this.subNodes = new ArrayList<>(subNodes);
	}

	@Override
	public Node cloneNode() throws CloneNotSupportedException {
		// just give me maybes plz
		final var newSubNodes = subNodes.stream().map(node -> {
			try {
				return node.cloneNode();
			} catch (CloneNotSupportedException e) {
				return null;
			}
		}).collect(Collectors.toList());
		if (newSubNodes.stream().anyMatch(node -> node == null)) {
			throw new CloneNotSupportedException("A subnode could not be cloned.");
		}
		return new TokenListNode(newSubNodes, getCodeIndex(), getSource());
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

	/**
	 * Returns the number of nodes in this list. Does NOT recursively count the subnodes in the child nodes themselves.
	 * @return the number of nodes in this list.
	 */
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
