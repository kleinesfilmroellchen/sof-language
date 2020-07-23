package klfr.sof.ast;

import java.util.*;

/**
 * A node representing primitive tokens. Because there are only a limited number
 * of primitive tokens, this node type is an enum to allow for easy switching
 * and instance checks.
 */
public enum PrimitiveTokenNode implements Node {
	
	Add("+"),
	Subtract("-"),
	Multiply("*"),
	Divide("/"),

	And("and"),
	Or("or"),
	ExclusiveOr("xor"),
	Not("not"),

	GreaterThan(">"),
	GreaterThanEquals(">="),
	LessThan("<"),
	LessThanEquals("<="),

	Equals("="),
	NotEquals("/="),

	Duplicate("dup"),
	Discard("pop"),
	Swap("swap"),

	If("if"),
	IfElse("ifelse"),
	Switch("switch"),
	While("while"),

	Define("def"),
	GlobalDefine("globaldef"),
	Call("."),
	DoubleCall(":"),
	//NativeCall("nativecall"),

	// Function("function"),
	// Constructor("constructor"),

	// Use("use"),
	// Export("export"),
	
	Write("write"),
	WriteLine("writeln"),
	Input("input"),
	InputLine("inputln"),

	DescribeElement("describe"),
	DescribeStack("describes"),
	Assert("assert"),
	
	;

	public final String symbol;

	private PrimitiveTokenNode(String symbol) {
		this.symbol = symbol;
	}

	/**
	 * Returns the primitive token associated with the symbol, if it exists.
	 */
	public static Optional<PrimitiveTokenNode> make(String symbol) {
		for (PrimitiveTokenNode pt : PrimitiveTokenNode.values()) {
			if (pt.symbol.equals(symbol)) return Optional.of(pt);
		}
		return Optional.empty();
	}

	@Override
	public String toString() {
		return "PT: " + this.name() + " [ " + symbol + " ]";
	}

	@Override
	public Object cloneNode() throws CloneNotSupportedException {
		return this;
	}
}