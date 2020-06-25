package klfr.sof.test;

import static org.junit.jupiter.api.Assertions.*;
import static java.lang.String.format;
import org.junit.jupiter.api.Test;

import klfr.sof.CompilerException;
import klfr.sof.Interpreter;
import klfr.sof.Tokenizer;

class InterpreterTest extends SofTestSuper {

	@Test
	void testCleanCode() {
		try {
			assertEquals(format("abc def ghi jkl%n"), Interpreter.cleanCode("abc def ghi jkl"),
					"Basic code without cleaning required");
			assertEquals(format("abc def ghi jkl %nand a newline%n"),
					Interpreter.cleanCode("abc def ghi jkl # a simple comment\nand a newline"));
			assertEquals(format("abc def ghi jkl %n%n%nthis comes after the blockcomment%n"), Interpreter.cleanCode(
					"abc def ghi jkl #* a multiline comment\nand a newline\nand another one*#\nthis comes after the blockcomment"));
			assertEquals(format("abc \" def # here is no comment\" ghi%n"),
					Interpreter.cleanCode("abc \" def # here is no comment\" ghi\n"));
			assertEquals(format("abc \" def # here is no comment\"%n%n"),
					Interpreter.cleanCode("abc \" def # here is no comment\"\n#but here is one \" with strings.\n"));
		} catch (CompilerException e) {
			fail(e);
		}
		assertThrows(CompilerException.class, () -> Interpreter.cleanCode("abc def ghi jkl \"  "));
		assertThrows(CompilerException.class, () -> Interpreter.cleanCode("\" unclosed string"));
		assertDoesNotThrow(() -> Interpreter.cleanCode("abc def ghi jkl \" jkjkk \""));
	}

	@Test
	void testIndexOfMatching() {
		assertEquals(28, Interpreter.indexOfMatching("( blah blah ( sjfjdk)    xx) )))", 0, "(", ")"));
		assertEquals(32, Interpreter.indexOfMatching("( blah blah ( sjfjdk)    xxdd)dd    dddd", 2, "blah", "dd"));
		assertEquals(21, Interpreter.indexOfMatching("( blah blah ( sjfjdk)    xx) )))))", 12, "(", ")"));
		assertEquals(-1, Interpreter.indexOfMatching("( blah blah ( sjfjdk    xx", 12, "(", ")"));
	}

}
