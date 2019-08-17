package klfr.sof;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class InterpreterTest {

	@Test
	void testCleanCode() {
		try {
			assertEquals("abc def ghi jkl\n", Interpreter.cleanCode("abc def ghi jkl"), "Basic code without cleaning required");
			assertEquals("abc def ghi jkl \n", Interpreter.cleanCode("abc def ghi jkl # a simple comment"));
			assertEquals("abc def ghi jkl \nand a newline\n",
					Interpreter.cleanCode("abc def ghi jkl # a simple comment\nand a newline"));
			assertEquals("abc def ghi jkl \n\n\nthis comes after the blockcomment\n", Interpreter.cleanCode(
					"abc def ghi jkl #* a multiline comment\nand a newline\nand another one*#\nthis comes after the blockcomment"));
			assertEquals("abc \" def # here is no comment\"\n",
					Interpreter.cleanCode("abc \" def # here is no comment\"\n"));
			assertEquals("abc \" def # here is no comment\"\n\n",
					Interpreter.cleanCode("abc \" def # here is no comment\"\n#but here is one \" with strings.\n"));
		} catch (CompilationError e) {
			fail(e);
		}
		assertThrows(CompilationError.class, () -> Interpreter.cleanCode("abc def ghi jkl \"  "));
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
