package klfr.sof.test;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import klfr.sof.lang.*;
import klfr.sof.lang.Stackable.DebugStringExtensiveness;

class SmallClassesTests extends SofTestSuper {

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
		// statement in. Although this feature has existed since Java 10, I have not
		// noticed it until 11/2019 Lol. I love Java.
		var nt = assertDoesNotThrow(() -> new Nametable());
		var p = IntPrimitive.createIntPrimitive(123l);
		var id = new Identifier("abc");
		assertDoesNotThrow(() -> nt.put(id, p));
		var recievedP = nt.get(new Identifier("abc"));
		assertEquals(recievedP, p);
		assertTrue(nt.identifiers().contains(id));
		assertDoesNotThrow(() -> nt.toDebugString(DebugStringExtensiveness.Full));
		assertDoesNotThrow(() -> nt.print());
		Nametable clone = (Nametable) assertDoesNotThrow(() -> nt.copy());
		assertTrue(clone.identifiers().contains(id));
		assertTrue(nt.hasMapping(id));
		assertFalse(nt.hasMapping(new Identifier("zzz")));
	}
}
