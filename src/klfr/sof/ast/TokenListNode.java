package klfr.sof.ast;

import java.util.*;
import java.util.function.Consumer;

/**
 * A list of nodes that are to be executed in order. This may be the main
 * program itself, or a code block literal.
 */
public class TokenListNode implements Node {
	private static final long serialVersionUID = 1L;

	private final List<Node> subNodes;

	public TokenListNode(List<Node> subNodes) {
		this.subNodes = subNodes;
	}

	@Override
	public Object cloneNode() throws CloneNotSupportedException {
		return new TokenListNode(subNodes);
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
				+ System.lineSeparator() + "}";
	}

	@Override
	public void forEach(Consumer<? super Node> action) {
		subNodes.forEach(action);
	}

	@Override
	public Iterator<Node> iterator() {
		return subNodes.iterator();
	}

	public int count() {
		return subNodes.size();
	}
}