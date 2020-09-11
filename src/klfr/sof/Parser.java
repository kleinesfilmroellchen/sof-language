package klfr.sof;

import java.util.*;
import java.util.logging.*;

import klfr.sof.Tokenizer.TokenizerState;
import klfr.sof.ast.*;
import klfr.sof.lang.*;

/**
 * Parses preprocessed SOF code into an AST.
 */
public class Parser {
	private static final Logger log = Logger.getLogger(Parser.class.getCanonicalName());

	public static Node parse(String code) throws CompilerException {
		return parse(code, 0, code.length());
	}

	@SuppressWarnings("deprecation")
	public static Node parse(String code, int start, int end) throws CompilerException {
		log.finer(() -> String.format("Parsing {%s} in %s", code.substring(start, end), code));
		Tokenizer tokenizer = Tokenizer.fromSourceCode(code);
		TokenizerState s = tokenizer.getState();
		s.end = s.start = start;
		s.regionStart = start;
		s.regionEnd = end;
		tokenizer.setState(s);
		final var tokens = new LinkedList<Node>();

		while (tokenizer.hasNext()) {
			String token = tokenizer.next();
			try {
				// primitive token
				final var pt = PrimitiveTokenNode.make(token, tokenizer.start(), code);
				if (pt.isPresent())
					tokens.add(pt.get());
				else
				// code block
				if (Patterns.codeBlockStartPattern.matcher(token).matches()) {
					log.finest(() -> String.format("Code block start token @ %4d", tokenizer.start()));
					final var endPos = Preprocessor.indexOfMatching(tokenizer.getCode(), tokenizer.start(),
							Patterns.codeBlockStartPattern, Patterns.codeBlockEndPattern) - 1;
					if (endPos < 0) {
						throw CompilerException.fromCurrentPosition(tokenizer, "syntax", "syntax.codeblock");
					}
					final Node cbParsed = parse(code, tokenizer.start() + 1, endPos);
					tokens.add(cbParsed);
					final var state = tokenizer.getState();
					state.start = state.end = endPos + 1;
					tokenizer.setState(state);
				} else
				// int literal
				if (Patterns.intPattern.matcher(token).matches()) {
					log.finest(() -> String.format("Literal integer token %30s @ %4d", token, tokenizer.start()));
					final IntPrimitive literal = IntPrimitive.createIntegerFromString(token.toLowerCase());
					tokens.add(new LiteralNode(literal, tokenizer.start(), code));
				} else
				// float literal
				if (Patterns.doublePattern.matcher(token).matches()) {
					log.finest(() -> String.format("Literal float token %30s @ %4d", token, tokenizer.start()));
					final FloatPrimitive literal = FloatPrimitive.createFloatFromString(token);
					tokens.add(new LiteralNode(literal, tokenizer.start(), code));
				} else
				// boolean literal
				if (Patterns.boolPattern.matcher(token).matches()) {
					log.finest(() -> String.format("Literal boolean token %30s @ %4d", token, tokenizer.start()));
					final BoolPrimitive literal = BoolPrimitive.createBoolFromString(token);
					tokens.add(new LiteralNode(literal, tokenizer.start(), code));
				} else
				// string literal
				if (Patterns.stringPattern.matcher(token).matches()) {
					log.finest(() -> String.format("Literal string token %30s @ %4d", token, tokenizer.start()));
					final StringPrimitive literal = StringPrimitive.createStringPrimitive(Preprocessor.preprocessSofString(token));
					tokens.add(new LiteralNode(literal, tokenizer.start(), code));
				} else
				// identifier
				if (Patterns.identifierPattern.matcher(token).matches()) {
					log.finest(() -> String.format("Identifier token %30s @ %4d", token, tokenizer.start()));
					tokens.add(new LiteralNode(new Identifier(token), tokenizer.start(), code));
				} else
					throw CompilerException.fromCurrentPosition(tokenizer, "syntax", null);

			} catch (final CompilerException.Incomplete e) {
				throw CompilerException.fromIncomplete(tokenizer, e);
			}
		}

		return new TokenListNode(new ArrayList<>(tokens), start, code);
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
