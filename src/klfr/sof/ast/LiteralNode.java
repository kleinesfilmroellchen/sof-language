package klfr.sof.ast;

import java.util.*;
import java.util.function.Consumer;

import klfr.sof.lang.Stackable;
import klfr.sof.lang.Stackable.DebugStringExtensiveness;

/**
 * A node that holds a simple Stackable to be placed on the stack.
 */
public class LiteralNode implements Node {
	private static final long serialVersionUID = 1L;

	private final Stackable value;

	public Stackable getValue() {
		return value;
	}

	public LiteralNode(final Stackable data) {
		this.value = data;
	}

	@Override
	public Object cloneNode() {
		return new LiteralNode(value);
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
		return "Literal: " + value.toDebugString(DebugStringExtensiveness.Type) + " [ " + value.toDebugString(DebugStringExtensiveness.Full) + " ]";
	}

	@Override
	public void forEach(Consumer<? super Node> action) {
		action.accept(this);
	}

	@Override
	public Iterator<Node> iterator() {
		return List.<Node>of(this).iterator();
	}

}