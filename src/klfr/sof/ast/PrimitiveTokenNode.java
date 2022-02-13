package klfr.sof.ast;

import java.util.*;

import klfr.sof.SOFFile;

/**
 * A node representing primitive tokens. It uses the inner enum type
 * {@link PrimitiveToken} to represent all possible primitive tokens.
 */
public final class PrimitiveTokenNode extends Node {
	private static final long serialVersionUID = 1L;

	/**
	 * The enumeration of all primitive tokens in SOF.
	 */
	public static enum PrimitiveToken {
		/** The <code>+</code> primitive token. */
		Add("+"),
		/** The <code>-</code> primitive token. */
		Subtract("-"),
		/** The <code>*</code> primitive token. */
		Multiply("*"),
		/** The <code>/</code> primitive token. */
		Divide("/"),
		/** The <code>%</code> primitive token. */
		Modulus("%"),

		/** The <code>cat</code> primitive token. */
		Concatenate("cat"),

		/** The <code>and</code> primitive token. */
		And("and"),
		/** The <code>or</code> primitive token. */
		Or("or"),
		/** The <code>xor</code> primitive token. */
		ExclusiveOr("xor"),
		/** The <code>not</code> primitive token. */
		Not("not"),

		/** The <code>&gt;</code> primitive token. */
		GreaterThan(">"),
		/** The <code>&gt;=</code> primitive token. */
		GreaterThanEquals(">="),
		/** The <code>&lt;</code> primitive token. */
		LessThan("<"),
		/** The <code>&lt;=</code> primitive token. */
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
		/** The <code>dexport</code> primitive token, syntax sugar for <code>IDENTIFIER globaldef IDENTIFIER export</code>. */
		DefineExport_Sugar("dexport"),
		/** The <code>.</code> primitive token. */
		Call("."),
		/** The <code>:</code> primitive token. */
		DoubleCall(":"),
		/** The <code>,</code> primitive token. */
		ObjectCall(","),
		/** The <code>;</code> primitive token. */
		ObjectMethodCall(";"),
		/** The <code>nativecall</code> primitive token. */
		NativeCall("nativecall"),

		/** The <code>function</code> primitive token. */
		Function("function"),
		/** The <code>return</code> primitive token. */
		Return("return"),
		/** The <code>return:0</code> primitive token. */
		ReturnNothing("return:0"),
		/** The <code>constructor</code> primitive token. */
		Constructor("constructor"),

		/** The <code>use</code> primitive token. */
		Use("use"),
		/** The <code>export</code> primitive token. */
		Export("export"),

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

		/**
		 * The string symbol that is used in source code to specify this primitive token.
		 */
		public final String symbol;

		private PrimitiveToken(String symbol) {
			this.symbol = symbol;
		}
	}

	/** The symbol that this primitive token node represents. */
	private final PrimitiveToken symbol;

	/**
	 * Returns the primitive token itself represented by this AST node.
	 * @return The primitive token represented by this AST node.
	 */
	public final PrimitiveToken symbol() {
		return symbol;
	}

	/**
	 * Create a new primitive token.
	 * @param symbol The primitive token itself represented by this AST node.
	 * @param index The index inside the source code where this primitive token is located.
	 * @param source The SOF source file unit where this primitive token comes from.
	 */
	public PrimitiveTokenNode(PrimitiveToken symbol, int index, SOFFile source) {
		super(index, source);
		this.symbol = symbol;
	}

	/**
	 * Returns the primitive token associated with the string symbol, if it exists.
	 * @param symbol The primitive token that should be represented, as a string.
	 * @param index The index inside the source code where the new primitive token is located.
	 * @param source The SOF source file unit where the new primitive token comes from.
	 * @return An Optional containing a primitive token if one exists for the given symbol,
	 *         or an empty Optional if the string symbol does not represent a primitive token.
	 */
	public static Optional<PrimitiveTokenNode> make(final String symbol, final int index, SOFFile source) {
		for (PrimitiveToken pt : PrimitiveToken.values()) {
			if (pt.symbol.equals(symbol))
				return Optional.of(new PrimitiveTokenNode(pt, index, source));
		}
		return Optional.empty();
	}

	@Override
	public String toString() {
		return "PT: " + this.symbol.name() + " [ " + this.symbol.symbol + " ] @ " + this.getCodeIndex();
	}

	@Override
	public Node cloneNode() throws CloneNotSupportedException {
		return new PrimitiveTokenNode(this.symbol, this.getCodeIndex(), getSource());
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof PrimitiveTokenNode ? ((PrimitiveTokenNode)obj).symbol.equals(this.symbol) : false;
	}

	@Override
	public int hashCode() {
		return symbol.hashCode();
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
