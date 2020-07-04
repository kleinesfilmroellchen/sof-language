package klfr.sof.test;

import static org.junit.jupiter.api.Assertions.*;
import static java.lang.String.format;
import org.junit.jupiter.api.Test;

import klfr.sof.CompilerException;
import klfr.sof.Preprocessor;

/**
 * Tests the interpreter and preprocessor basic functionality.
 */
class InterpreterTest extends SofTestSuper {

	@Test
	void testPreprocessor() {
		try {
			assertEquals(format("abc def ghi jkl%n"), Preprocessor.preprocessCode("abc def ghi jkl"),
					"Basic code without cleaning required");
			assertEquals(format("abc def ghi jkl %nand a newline%n"),
					Preprocessor.preprocessCode("abc def ghi jkl # a simple comment\nand a newline"));
			assertEquals(format("abc def ghi jkl %n%n%nthis comes after the blockcomment%n"), Preprocessor.preprocessCode(
					"abc def ghi jkl #* a multiline comment\nand a newline\nand another one*#\nthis comes after the blockcomment"));
			assertEquals(format("abc \" def # here is no comment\" ghi%n"),
					Preprocessor.preprocessCode("abc \" def # here is no comment\" ghi\n"));
			assertEquals(format("abc \" def # here is no comment\"%n%n"),
					Preprocessor.preprocessCode("abc \" def # here is no comment\"\n#but here is one \" with strings.\n"));
		} catch (CompilerException e) {
			fail(e);
		}
		assertThrows(CompilerException.class, () -> Preprocessor.preprocessCode("abc def ghi jkl \"  "));
		assertThrows(CompilerException.class, () -> Preprocessor.preprocessCode("\" unclosed string"));
		assertThrows(CompilerException.class, () -> Preprocessor.preprocessCode("{ } unclosed { codeblock"));
		assertDoesNotThrow(() -> Preprocessor.preprocessCode("abc def ghi jkl \" jkjkk \""));
		assertDoesNotThrow(() -> Preprocessor.preprocessCode("abc def ghi jkl { jkjkk }"));
	}

	@Test
	void testIndexOfMatching() {
		assertEquals(28, Preprocessor.indexOfMatching("( blah blah ( sjfjdk)    xx) )))", 0, "(", ")"));
		assertEquals(32, Preprocessor.indexOfMatching("( blah blah ( sjfjdk)    xxdd)dd    dddd", 2, "blah", "dd"));
		assertEquals(21, Preprocessor.indexOfMatching("( blah blah ( sjfjdk)    xx) )))))", 12, "(", ")"));
		assertEquals(-1, Preprocessor.indexOfMatching("( blah blah ( sjfjdk    xx", 12, "(", ")"));
	}

}
