package klfr.sof.lang;

import klfr.sof.ast.TokenListNode;

@StackableName("Codeblock")
public class CodeBlock implements Stackable {
	private static final long serialVersionUID = 1L;

	protected final TokenListNode code;

	public CodeBlock(TokenListNode code) {
		this.code = code;
	}

	@Override
	public String toDebugString(DebugStringExtensiveness e) {
		String code = this.code.toString();
		switch (e) {
			case Full:
				return String.format("[CodeBlock { %s } %h]", code, hashCode());
			case Compact:
				return "[CodeBlk " + this.code.count() + "n ]";
			default:
				return Stackable.toDebugString(this, e);
		}
	}

	@Override
	public Stackable copy() {
		return new CodeBlock(this.code);
	}

	@Override
	public String print() {
		return "{ Codeblock }";
	}

	@Override
	public boolean equals(Stackable other) {
		if (other instanceof CodeBlock)
			// although code blocks are never equal, they may be compared
			return false;
		return super.equals(other);
	}

}
