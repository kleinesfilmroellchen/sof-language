package klfr.sof.test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.NoSuchElementException;
import java.util.regex.Matcher;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import klfr.sof.Tokenizer;
import klfr.sof.Tokenizer.TokenizerState;

class TokenizerTest extends SofTestSuper {

	@Test
	void testFromSourceCode() {
		Tokenizer t = assertDoesNotThrow(() -> Tokenizer.fromSourceCode("hello code"));
		assertEquals("hello code", t.getCode());
	}

	@Test
	void testFromState() {
		TokenizerState s = assertDoesNotThrow(() -> new TokenizerState(0, 0, 3, 5, "abc def ghi jkl mno p"));
		Tokenizer t = assertDoesNotThrow(() -> Tokenizer.fromState(s));
		assertEquals("abc def ghi jkl mno p", t.getCode());
		assertEquals("abc def ghi jkl mno p", t.getState().code);
		assertEquals(0, t.getState().start);
		assertEquals(0, t.getState().end);
		assertEquals(3, t.getState().regionStart);
		assertEquals(5, t.getState().regionEnd);
	}

	@Test
	@Order(Integer.MAX_VALUE - 1)
	void testTokenizerState() {
		TokenizerState s = assertDoesNotThrow(() -> new TokenizerState(0, 12, 3, 5, "abc def ghi jkl mno p"));
		assertEquals(0, s.start);
		assertEquals(12, s.end);
		assertEquals(3, s.regionStart);
		assertEquals(5, s.regionEnd);
		assertEquals("abc def ghi jkl mno p", s.code);
		assertDoesNotThrow(() -> s.toString());
		TokenizerState clone = assertDoesNotThrow(() -> s.clone());
		assertEquals(clone.start, s.start);
		assertEquals(clone.end, s.end);
		assertEquals(clone.regionStart, s.regionStart);
		assertEquals(clone.regionEnd, s.regionEnd);
		assertEquals(clone.code, s.code);
		assertTrue(clone.equals(s));
	}

	@Test
	void testWithCodeAppended() {
		Tokenizer t = Tokenizer.fromSourceCode("hello code");
		Tokenizer more = assertDoesNotThrow(() -> t.withCodeAppended("newline"));
		assertEquals(String.format("hello code\nnewline"), more.getCode());
		assertEquals(String.format("hello code"), t.getCode());
	}

	@Test
	void testAppendCode() {
		Tokenizer t = Tokenizer.fromSourceCode("hello code");
		t.appendCode("newline");
		assertEquals(String.format("hello code\nnewline"), t.getCode());
	}

	@Test
	void testClone() {
		Tokenizer t = assertDoesNotThrow(() -> Tokenizer.fromSourceCode("hello code"));
		Tokenizer clone = assertDoesNotThrow(() -> t.clone());
		assertTrue(t.getCode().equals(clone.getCode()));
	}

	@Test
	@SuppressWarnings("deprecation")
	void testStateStack() {
		Tokenizer t = assertDoesNotThrow(() -> Tokenizer.fromSourceCode("hello other code"));
		assertDoesNotThrow(() -> t.next());
		Matcher m = t.getMatcher();
		int regS = m.regionStart(), regE = m.regionEnd();
		t.pushState();
		var st = assertDoesNotThrow(() -> t.getState());
		st.regionStart = 3;
		st.regionEnd = 6;
		assertDoesNotThrow(() -> t.setState(st));
		assertEquals(3, t.getState().regionStart);
		assertEquals(6, t.getState().regionEnd);
		int currentStart = t.getState().start, currentEnd = t.getState().end;
		t.pushState();
		t.popState();
		assertEquals(currentStart, t.getState().start);
		assertEquals(currentEnd, t.getState().end);
		assertEquals(3, t.getState().regionStart);
		assertEquals(6, t.getState().regionEnd);
		t.popState();
		assertEquals(regS, t.getState().regionStart);
		assertEquals(regE, t.getState().regionEnd);
		assertThrows(NoSuchElementException.class, () -> t.popState());
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
