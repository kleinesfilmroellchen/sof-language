package klfr.sof.test;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import klfr.sof.CompilerException;
import klfr.sof.lang.Identifier;
import klfr.sof.lang.Stackable.DebugStringExtensiveness;

class IdentifierTest {

	@Test
	void testGetValue() {
		Identifier i = assertDoesNotThrow(() -> new Identifier("test'"));
		assertEquals("test'", i.getValue(), "Basic identifier getvalue test");
		i = assertDoesNotThrow(() -> new Identifier("blah  "));
		assertEquals("blah", i.getValue(), "identifier getvalue test with trim");
		i = assertDoesNotThrow(() -> new Identifier("はるこ"));
		assertEquals("はるこ", i.getValue(), "identifier getvalue test with japanese");
		i = assertDoesNotThrow(() -> new Identifier("はるこ_κοπαΞΕΚ"));
		assertEquals("はるこ_κοπαΞΕΚ", i.getValue(), "identifier getvalue test with japanese and greek");
	}

	@Test
	void testGetDebugDisplay() {
		Identifier i = assertDoesNotThrow(() -> new Identifier("test'"));
		assertEquals("test'", i.print(), "Basic identifier print test");
	}

	@Test
	void testIsValidIdentifier() {
		assertThrows(CompilerException.class, () -> new Identifier("-abc-def"), "Invalid Identifier test");
		assertThrows(CompilerException.class, () -> new Identifier("abc-def  kl"), "Invalid Identifier test spaces");
		assertDoesNotThrow(() -> new Identifier("abcdefgはるこ"), "Valid Identifier test");
		assertDoesNotThrow(() -> new Identifier("   abcdefgはるこ     "), "Valid Identifier test with trim");
		assertDoesNotThrow(() -> new Identifier("   abc__d9090efg''はるこ     "), "Valid Identifier test with non-alnum");
		assertThrows(CompilerException.class, () -> new Identifier("90abc__d9090efg"),
				"Invalid Identifier test with starting numeric");
	}

	/**
	 * Tests other methods such as equals(), clone() etc.
	 */
	@Test
	void testOther() {
		Identifier i = new Identifier("abc");
		assertDoesNotThrow(() -> i.hashCode());
		Identifier clone = (Identifier) assertDoesNotThrow(() -> i.copy());
		assertTrue(i.equals(i));
		assertFalse(i.equals(new Identifier("bcd")));
		assertTrue(i.equals(clone));
		assertDoesNotThrow(() -> i.print() + i.toDebugString(DebugStringExtensiveness.Full));
	}

}
