package klfr.sof.test;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import klfr.sof.lang.*;

class SmallClassesTests {

	/**
	 * Tests the stub classes in all packages. These are: ScopeDelimiter
	 * FunctionDelimiter
	 */
	@Test
	void testStubClasses() {
		assertDoesNotThrow(() -> new FunctionDelimiter());
	}

	@Test
	void testNametable() {
		// For the record:
		// This is the first piece of code that klfr used the 'var' type inference
		// statement in.
		// Although this feature has existed since Java 10, I have not noticed it until
		// 11/2019
		// Lol. I love Java.
		var nt = assertDoesNotThrow(() -> new Nametable());
		var p = new Primitive<Long>(123l);
		var id = new Identifier("abc");
		assertDoesNotThrow(() -> nt.put(id, p));
		var recievedP = nt.get(new Identifier("abc"));
		assertEquals(recievedP, p);
		assertTrue(nt.identifiers().contains(id));
		assertDoesNotThrow(() -> nt.getDebugDisplay());
		assertDoesNotThrow(() -> nt.toString());
		Nametable clone = (Nametable) assertDoesNotThrow(() -> nt.clone());
		assertTrue(clone.identifiers().contains(id));
		assertTrue(nt.hasMapping(id));
		assertFalse(nt.hasMapping(new Identifier("zzz")));
	}

	@Test
	void testCodeBlock() {
		var cb = assertDoesNotThrow(() -> new CodeBlock(0, 12, "abc def ghi j k"));
		assertDoesNotThrow(() -> cb.getDebugDisplay());
		assertDoesNotThrow(() -> cb.toString());
		// TODO insert equals test
		var clone = assertDoesNotThrow(() -> cb.clone());
		// TODO test call provider somewhere else
	}

}
