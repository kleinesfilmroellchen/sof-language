package klfr.sof;

import java.io.File;
import java.util.*;
import java.util.logging.*;

import klfr.sof.Tokenizer.TokenizerState;
import klfr.sof.ast.*;
import klfr.sof.exceptions.CompilerException;
import klfr.sof.exceptions.IncompleteCompilerException;
import klfr.sof.lang.*;
import klfr.sof.lang.primitive.*;

/**
 * Parses preprocessed SOF code into an AST.
 */
public final class Parser {

	private static final Logger log = Logger.getLogger(Parser.class.getCanonicalName());

	/**
	 * Parses preprocessed SOF code into an abstract syntax tree (AST).
	 * 
	 * @param f    The file where the source code comes from. Is never read so can be invalid.
	 * @param code The code to parse, already preprocessed.
	 * @return An abstract syntax tree representing the code.
	 * @throws CompilerException If a syntax error is encountered.
	 */
	public static SOFFile parse(File f, String code) throws CompilerException {
		// Although the SOFFile technically needs the AST, nobody is going to read it at this stage anyways.
		// Therefore, just null it for now and set it after parsing.
		// After all, the nodes reference the SOFFile object, not the AST itself.
		final var source = new SOFFile(f, code, null);
		final var ast = parse(source, 0, code.length());
		source.setAST(ast);
		return source;
	}

	/**
	 * Parse the SOF source code from the start index to the end index. This means that the SOF source code is read, syntax
	 * is checked, and an AST is created. The code must already be preprocessed.
	 * 
	 * @param source The source code to be parsed.
	 * @param start  The start index, inclusive, that determines the substring to parse.
	 * @param end    The end index, exclusive, that determines the substring to parse.
	 * @return The parsed AST. The root node will always be a {@link klfr.sof.ast.TokenListNode}.
	 * @throws CompilerException If a syntax error is encountered.
	 * @see Preprocessor#preprocessCode(String)
	 */
	@SuppressWarnings("deprecation")
	public static Node parse(SOFFile source, int start, int end) throws CompilerException {
		String code = source.code();
		log.finer(() -> String.format("Parsing {%s}", code.substring(start, end)));
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
				final var pt = PrimitiveTokenNode.make(token, tokenizer.start(), source);
				if (pt.isPresent())
					tokens.add(pt.get());
				else
				// code block
				if (Patterns.codeBlockStartPattern.matcher(token).matches()) {
					log.finest(() -> String.format("Code block start token @ %4d", tokenizer.start()));
					final var endPos = Preprocessor.indexOfMatching(tokenizer.getCode(), tokenizer.start(), Patterns.codeBlockStartPattern, Patterns.codeBlockEndPattern) - 1;
					if (endPos < 0) {
						throw CompilerException.fromTokenizer(source.sourceFile().getPath(), tokenizer, "syntax", "syntax.codeblock");
					}
					final Node cbParsed = parse(source, tokenizer.start() + 1, endPos);
					tokens.add(cbParsed);
					final var state = tokenizer.getState();
					state.start = state.end = endPos + 1;
					tokenizer.setState(state);
				} else
				// int literal
				if (Patterns.intPattern.matcher(token).matches()) {
					log.finest(() -> String.format("Literal integer token %30s @ %4d", token, tokenizer.start()));
					final IntPrimitive literal = IntPrimitive.createIntegerFromString(token.toLowerCase());
					tokens.add(new LiteralNode(literal, tokenizer.start(), source));
				} else
				// float literal
				if (Patterns.doublePattern.matcher(token).matches()) {
					log.finest(() -> String.format("Literal float token %30s @ %4d", token, tokenizer.start()));
					final FloatPrimitive literal = FloatPrimitive.createFloatFromString(token);
					tokens.add(new LiteralNode(literal, tokenizer.start(), source));
				} else
				// boolean literal
				if (Patterns.boolPattern.matcher(token).matches()) {
					log.finest(() -> String.format("Literal boolean token %30s @ %4d", token, tokenizer.start()));
					final BoolPrimitive literal = BoolPrimitive.createBoolFromString(token);
					tokens.add(new LiteralNode(literal, tokenizer.start(), source));
				} else
				// string literal
				if (Patterns.stringPattern.matcher(token).matches()) {
					log.finest(() -> String.format("Literal string token %30s @ %4d", token, tokenizer.start()));
					final StringPrimitive literal = StringPrimitive.createStringPrimitive(Preprocessor.preprocessSofString(token));
					tokens.add(new LiteralNode(literal, tokenizer.start(), source));
				} else
				// identifier
				if (Patterns.identifierPattern.matcher(token).matches()) {
					log.finest(() -> String.format("Identifier token %30s @ %4d", token, tokenizer.start()));
					tokens.add(new LiteralNode(new Identifier(token), tokenizer.start(), source));
				} else
				// transparent marker
				if (Patterns.transparentPattern.matcher(token).matches()) {
					log.finest(() -> String.format("Transparent token %30s @ %4d", token, tokenizer.start()));
					tokens.add(new LiteralNode(new TransparentData(TransparentData.TransparentType.fromSymbol(token)), tokenizer.start(), source));
				} else
					throw CompilerException.fromTokenizer(source.sourceFile().getPath(), tokenizer, "syntax", null);

			} catch (final IncompleteCompilerException e) {
				throw CompilerException.fromIncompleteAndTokenizer(tokenizer, e);
			}
		}

		return new TokenListNode(new ArrayList<>(tokens), start, source);
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
