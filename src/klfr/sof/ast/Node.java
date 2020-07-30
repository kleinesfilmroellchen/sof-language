package klfr.sof.ast;

import java.io.Serializable;
import java.util.*;
import java.util.function.*;
import java.util.logging.Logger;

/**
 * A node is an element in the abstract syntax tree (AST) that may contain other
 * nodes. The implementations are responsible for properly traversing their
 * subnodes as well as providing relevant data to the actions.
 */
public interface Node extends Serializable, Cloneable, Iterable<Node> {
	static final Logger log = Logger.getLogger(Node.class.getCanonicalName());

	/**
	 * Returns the index inside the source code where this node was located.
	 * 
	 * @return the index inside the source code where this node was located.
	 */
	public int getCodeIndex();

	/**
	 * The primary method of the node. Traverses the node and all of its children in
	 * proper order and hands them off to the action for processing.
	 */
	@Override
	default void forEach(Consumer<? super Node> action) {
		this.forEach(n -> {
			action.accept(n);
			return true;
		});
	}

	/**
	 * The primary method of the node. Traverses the node and all of its children in
	 * proper order and hands them off to the action for processing. If the action
	 * ever returns false, any iterative application should stop.<br><br>
	 * The default method applies the function to this node directly, which is
	 * useful for most "primitive" nodes.
	 */
	default void forEach(Function<? super Node, Boolean> action) {
		action.apply(this);
	}

	/**
	 * Returns an iterator over this node's children, or an iterator with only this
	 * node if this node has no children.
	 */
	@Override
	default Iterator<Node> iterator() {
		return List.of(this).iterator();
	}

	public Object cloneNode() throws CloneNotSupportedException;

	@Override
	boolean equals(Object obj);

	@Override
	int hashCode();

	@Override
	String toString();

}