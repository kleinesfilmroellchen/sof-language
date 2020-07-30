package klfr.sof.ast;

import java.util.*;

/**
 * A node representing primitive tokens. It uses the inner enum type
 * {@link PrimitiveToken} to represent all possible primitive tokens.
 */
public class PrimitiveTokenNode implements Node {
	private static final long serialVersionUID = 1L;

	public enum PrimitiveToken {
		/** The <code>+</code> primitive token. */
		Add("+"),
		/** The <code>-</code> primitive token. */
		Subtract("-"),
		/** The <code>*</code> primitive token. */
		Multiply("*"),
		/** The <code>/</code> primitive token. */
		Divide("/"),

		/** The <code>and</code> primitive token. */
		And("and"),
		/** The <code>or</code> primitive token. */
		Or("or"),
		/** The <code>xor</code> primitive token. */
		ExclusiveOr("xor"),
		/** The <code>not</code> primitive token. */
		Not("not"),

		/** The <code>></code> primitive token. */
		GreaterThan(">"),
		/** The <code>>=</code> primitive token. */
		GreaterThanEquals(">="),
		/** The <code><</code> primitive token. */
		LessThan("<"),
		/** The <code><=</code> primitive token. */
		LessThanEquals("<="),

		/** The <code>=</code> primitive token. */
		Equals("="),
		/** The <code>/=</code> primitive token. */
		NotEquals("/="),

		/** The <code>dup</code> primitive token. */
		Duplicate("dup"),
		/** The <code>pop</code> primitive token. */
		Discard("pop"),
		/** The <code>swap</code> primitive token. */
		Swap("swap"),

		/** The <code>if</code> primitive token. */
		If("if"),
		/** The <code>ifelse</code> primitive token. */
		IfElse("ifelse"),
		/** The <code>switch</code> primitive token. */
		Switch("switch"),
		/** The <code>while</code> primitive token. */
		While("while"),

		/** The <code>def</code> primitive token. */
		Define("def"),
		/** The <code>globaldef</code> primitive token. */
		GlobalDefine("globaldef"),
		/** The <code>.</code> primitive token. */
		Call("."),
		/** The <code>:</code> primitive token. */
		DoubleCall(":"),
		/** The <code>nativefunction</code> primitive token. */
		// NativeFunction("nativefunction"),

		/** The <code>function</code> primitive token. */
		Function("function"),
		/** The <code>return</code> primitive token. */
		Return("return"),
		/** The <code>constructor</code> primitive token. */
		// Constructor("constructor"),

		/** The <code>use</code> primitive token. */
		// Use("use"),
		/** The <code>export</code> primitive token. */
		// Export("export"),

		/** The <code>write</code> primitive token. */
		Write("write"),
		/** The <code>writeln</code> primitive token. */
		WriteLine("writeln"),
		/** The <code>input</code> primitive token. */
		Input("input"),
		/** The <code>inputln</code> primitive token. */
		InputLine("inputln"),

		/** The <code>describe</code> primitive token. */
		DescribeElement("describe"),
		/** The <code>describes</code> primitive token. */
		DescribeStack("describes"),
		/** The <code>assert</code> primitive token. */
		Assert("assert"),

		;

		public final String symbol;

		private PrimitiveToken(String symbol) {
			this.symbol = symbol;
		}
	}

	private final PrimitiveToken symbol;
	private final int index;
	
	@Override
	public int getCodeIndex() {
		return index;
	}

	public PrimitiveToken symbol() {
		return symbol;
	}

	public PrimitiveTokenNode(PrimitiveToken symbol, int index) {
		this.symbol = symbol;
		this.index = index;
	}

	/**
	 * Returns the primitive token associated with the symbol, if it exists.
	 */
	public static Optional<PrimitiveTokenNode> make(final String symbol, final int index) {
		for (PrimitiveToken pt : PrimitiveToken.values()) {
			if (pt.symbol.equals(symbol))
				return Optional.of(new PrimitiveTokenNode(pt, index));
		}
		return Optional.empty();
	}

	@Override
	public String toString() {
		return "PT: " + this.symbol.name() + " [ " + this.symbol.symbol + " ] @ " + index;
	}

	@Override
	public Object cloneNode() throws CloneNotSupportedException {
		return new PrimitiveTokenNode(this.symbol, this.index);
	}
}