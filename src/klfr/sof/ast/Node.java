package klfr.sof.ast;

import java.io.Serializable;
import java.util.*;
import java.util.function.*;
import java.util.logging.Logger;

import klfr.sof.*;
import klfr.sof.exceptions.*;

/**
 * A node is an element in the abstract syntax tree (AST) that may contain other
 * nodes. The implementations are responsible for properly traversing their
 * subnodes as well as providing relevant data to the actions.
 */
public abstract class Node implements Serializable, Cloneable, Iterable<Node> {
	private static final long serialVersionUID = 1L;

	private static final Logger log = Logger.getLogger(Node.class.getCanonicalName());

	@FunctionalInterface
	public interface ForEachType {
		public Boolean exec(Node operand) throws CompilerException, IncompleteCompilerException;
	}

	private final int index;
	private final SOFFile source;

	protected Node(int index, SOFFile source) {
		this.index = index;
		this.source = source;
	}

	/**
	 * Returns the index inside the source code where this node was located.
	 * 
	 * @return the index inside the source code where this node was located.
	 */
	public int getCodeIndex() {
		return index;
	}

	/**
	 * Returns the source file that this node is contained in.
	 * 
	 * @return the source file that this node is contained in.
	 */
	public SOFFile getSource() {
		return source;
	}

	/**
	 * The primary method of the node. Traverses the node and all of its children in
	 * proper order and hands them off to the action for processing.
	 */
	@Override
	public void forEach(Consumer<? super Node> action) {
		try {
			this.forEach(n -> {
				action.accept(n);
				return true;
			});
		} catch (CompilerException | IncompleteCompilerException e) {
			throw new RuntimeException("お前はもう死んでいる! (this shouldn't happen)", e);
		}
	}

	/**
	 * The primary method of the node. Traverses the node and all of its children in
	 * proper order and hands them off to the action for processing. If the action
	 * ever returns false, any iterative application should stop.<br>
	 * <br>
	 * The default method applies the function to this node directly, which is
	 * useful for most "primitive" nodes.
	 * 
	 * @throws IncompleteCompilerException
	 * @throws CompilerException
	 */
	public boolean forEach(ForEachType action) throws IncompleteCompilerException, CompilerException {
		return action.exec(this);
	}

	/**
	 * Returns an iterator over this node's children, or an iterator with only this
	 * node if this node has no children.
	 */
	@Override
	public Iterator<Node> iterator() {
		return List.of(this).iterator();
	}

	public abstract Object cloneNode() throws CloneNotSupportedException;

	@Override
	public abstract boolean equals(Object obj);

	@Override
	public abstract int hashCode();

	@Override
	public abstract String toString();

	/** Count the subnodes of this node, including this node itself. The default returns 1. */
	public int nodeCount() { return 1; }

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
