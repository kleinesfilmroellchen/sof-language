package klfr.sof.test;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import klfr.sof.Preprocessor;
import klfr.sof.exceptions.CompilerException;

/**
 * Tests the interpreter and preprocessor basic functionality.
 */
class PreprocessorTest extends SofTestSuper {

	@Test
	void testPreprocessor() throws CompilerException {
		assertEquals("abc def ghi jkl", Preprocessor.preprocessCode("abc def ghi jkl"),
				"Basic code without cleaning required");
		assertEquals("abc def ghi jkl                   \nand a newline",
				Preprocessor.preprocessCode("abc def ghi jkl # a simple comment\nand a newline"));
		assertEquals(
				"abc def ghi jkl                       \n             \n                 \nthis comes after the blockcomment",
				Preprocessor.preprocessCode(
						"abc def ghi jkl #* a multiline comment\nand a newline\nand another one*#\nthis comes after the blockcomment"));
		assertEquals("abc \" def # here is no comment\" ghi\n",
				Preprocessor.preprocessCode("abc \" def # here is no comment\" ghi\n"));
		assertEquals("abc \" def # here is no comment\"\n                                \n",
				Preprocessor.preprocessCode("abc \" def # here is no comment\"\n#but here is one \" with strings.\n"));
	}

	@Test
	void testIndexOfMatching() {
		assertEquals(28, Preprocessor.indexOfMatching("( blah blah ( sjfjdk)    xx) )))", 0, "(", ")"));
		assertEquals(32, Preprocessor.indexOfMatching("( blah blah ( sjfjdk)    xxdd)dd    dddd", 2, "blah", "dd"));
		assertEquals(21, Preprocessor.indexOfMatching("( blah blah ( sjfjdk)    xx) )))))", 12, "(", ")"));
		assertEquals(-1, Preprocessor.indexOfMatching("( blah blah ( sjfjdk    xx", 12, "(", ")"));
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
