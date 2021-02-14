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

	/**
	 * The function type used by the interpreter for looping through nodes. <br/><br/>
	 * 
	 * This is a functional interface.
	 */
	@FunctionalInterface
	public interface ForEachType {
		/**
		 * Executes this function on the given node.
		 * 
		 * @param operand The node to be acted upon.
		 * @return The "return flag": Whether this node wants execution in the current context to continue.
		 *         When the return value is {@code true}, the traversor is instructed to continue executing the next node.
		 *         When the return value is {@code false}, the traversor is instructed to break from the current code block.
		 *         Traversors that do not emulate code blocks should pass on the "return flag" to the higher context.
		 * @throws CompilerException If an exception occurs that can be specifically located.
		 * @throws IncompleteCompilerException If an exception occurs that cannot be located to a point of failure.
		 */
		public Boolean exec(Node operand) throws CompilerException, IncompleteCompilerException;
	}

	/** The index inside the source file where this node is located. */
	private final int index;
	/** The source file where this node is located. */
	private final SOFFile source;

	/**
	 * Create a new node that is situated at the given index in the given source file.
	 * @param index The index inside the source code where this node is located.
	 * @param source The SOF source file unit where this node comes from.
	 */
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
	 * ever returns false, any iterative application should stop.<br/>
	 * <br/>
	 * The default method applies the function to this node directly, which is
	 * useful for most "primitive" nodes.
	 * 
	 * @param action The function to execute on this node and its children.
	 * @return Whether the action ever returned false.
	 *         This means that the return value of the last executed action is propagated to the caller.
	 * @throws IncompleteCompilerException If something goes wrong and cannot be located in the source code.
	 * @throws CompilerException If something goes wrong and can be located to a specific location in the code.
	 * @see klfr.sof.ast.Node.ForEachType#exec(Node)
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

	/**
	 * Clone this node, i.e. do a deep copy.
	 * @return The newly copied but equal node. This clone must be equal as determined by {@link Object#equals(Object)}
	 * @throws CloneNotSupportedException If this node fails to clone itself or some of its data.
	 */
	public abstract Node cloneNode() throws CloneNotSupportedException;

	@Override
	public abstract boolean equals(Object obj);

	@Override
	public abstract int hashCode();

	@Override
	public abstract String toString();

	/**
	 * Count the subnodes of this node, including this node itself. The default returns 1.
	 * The count should be recursive, i.e. the child nodes should be counted as well.
	 * @return The number of subnodes in this node.
	 */
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
