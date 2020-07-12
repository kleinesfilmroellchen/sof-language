package klfr.sof.test;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import klfr.sof.CompilerException;
import klfr.sof.Preprocessor;

/**
 * Tests the interpreter and preprocessor basic functionality.
 */
class PreprocessorTest extends SofTestSuper {

	@Test
	void testPreprocessor() {
		try {
			assertEquals("abc def ghi jkl", Preprocessor.preprocessCode("abc def ghi jkl"),
					"Basic code without cleaning required");
			assertEquals("abc def ghi jkl                   \nand a newline",
					Preprocessor.preprocessCode("abc def ghi jkl # a simple comment\nand a newline"));
			assertEquals("abc def ghi jkl                       \n             \n                 \nthis comes after the blockcomment", Preprocessor.preprocessCode(
					"abc def ghi jkl #* a multiline comment\nand a newline\nand another one*#\nthis comes after the blockcomment"));
			assertEquals("abc \" def # here is no comment\" ghi\n",
					Preprocessor.preprocessCode("abc \" def # here is no comment\" ghi\n"));
			assertEquals("abc \" def # here is no comment\"\n                                \n",
					Preprocessor.preprocessCode("abc \" def # here is no comment\"\n#but here is one \" with strings.\n"));
		} catch (CompilerException e) {
			fail(e);
		}
	}

	@Test
	void testIndexOfMatching() {
		assertEquals(28, Preprocessor.indexOfMatching("( blah blah ( sjfjdk)    xx) )))", 0, "(", ")"));
		assertEquals(32, Preprocessor.indexOfMatching("( blah blah ( sjfjdk)    xxdd)dd    dddd", 2, "blah", "dd"));
		assertEquals(21, Preprocessor.indexOfMatching("( blah blah ( sjfjdk)    xx) )))))", 12, "(", ")"));
		assertEquals(-1, Preprocessor.indexOfMatching("( blah blah ( sjfjdk    xx", 12, "(", ")"));
	}

}
