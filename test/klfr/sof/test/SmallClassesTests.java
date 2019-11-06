package klfr.sof.test;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import klfr.sof.lang.*;

class SmallClassesTests {

	/**
	 * Tests the stub classes in all packages. These are:
	 * ScopeDelimiter
	 * FunctionDelimiter
	 */
	@Test
	void testStubClasses() {
		assertDoesNotThrow( () -> new ScopeDelimiter());
		assertDoesNotThrow( () -> new FunctionDelimiter());
	}
	
	@Test
	void testNametable() {
		// For the record:
		// This is the first piece of code that klfr used the 'var' type inference statement in.
		// Although this feature has existed since Java 10, I have not noticed it until 11/2019
		// Lol. I love Java.
		var nt = assertDoesNotThrow(() -> new Nametable());
		var p = new Primitive<Long>(123l); var id = new Identifier("abc");
		assertDoesNotThrow(() -> nt.put(id, p));
		var recievedP = nt.get(new Identifier("abc"));
		assertEquals(recievedP, p);
	}

}
