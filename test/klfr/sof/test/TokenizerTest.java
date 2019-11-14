package klfr.sof.test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.NoSuchElementException;
import java.util.regex.Matcher;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import klfr.sof.CompilerException;
import klfr.sof.Tokenizer;
import klfr.sof.Tokenizer.TokenizerState;

class TokenizerTest {

	@Test
	void testFromSourceCode() {
		Tokenizer t = assertDoesNotThrow(() -> Tokenizer.fromSourceCode("hello code"));
		assertEquals(String.format("hello code%n"), t.getCode());
		assertThrows(CompilerException.class, () -> Tokenizer.fromSourceCode("\" unclosed string"), "illegal code");
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
	@Order(Integer.MAX_VALUE-1)
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
		assertEquals(String.format("hello code%nnewline%n"), more.getCode());
		assertEquals(String.format("hello code%n"), t.getCode());
	}

	@Test
	void testAppendCode() {
		Tokenizer t = Tokenizer.fromSourceCode("hello code");
		t.appendCode("newline");
		assertEquals(String.format("hello code%nnewline%n"), t.getCode());
	}

	@Test
	void testClone() {
		Tokenizer t = assertDoesNotThrow(() -> Tokenizer.fromSourceCode("hello code"));
		Tokenizer clone = assertDoesNotThrow(() -> t.clone());
		assertTrue(t.getCode().equals(clone.getCode()));
	}

	@Test
	void testStateStack() {
		Tokenizer t = assertDoesNotThrow(() -> Tokenizer.fromSourceCode("hello other code"));
		String nextToken = assertDoesNotThrow(() -> t.next());
		Matcher m = t.getMatcher();
		int regS = m.regionStart(), regE = m.regionEnd();
		t.pushState();
		m.region(3, 6);
		assertEquals(3, t.getState().regionStart);
		assertEquals(6, t.getState().regionEnd);
		int currentStart = t.getState().start, currentEnd = t.getState().end;
		t.pushState();
		t.next();
		t.popState();
		assertEquals(currentStart, t.getState().start);
		assertEquals(currentEnd, t.getState().end);
		assertEquals(3, t.getState().regionStart);
		assertEquals(6, t.getState().regionEnd);
		t.popState();
		assertEquals(regS, t.getMatcher().regionStart());
		assertEquals(regE, t.getMatcher().regionEnd());
		assertThrows(NoSuchElementException.class, () -> t.popState());
	}

}
