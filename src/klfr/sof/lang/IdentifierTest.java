package klfr.sof.lang;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import klfr.sof.CompilationError;

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
		assertEquals("test'", i.getDebugDisplay(), "Basic identifier getdebug test");
	}

	@Test
	void testIsValidIdentifier() {
		assertThrows(CompilationError.class, () -> new Identifier("-abc-def"), "Invalid Identifier test");
		assertThrows(CompilationError.class, () -> new Identifier("abc-def  kl"), "Invalid Identifier test spaces");
		assertDoesNotThrow(() -> new Identifier("abcdefgはるこ"), "Valid Identifier test");
		assertDoesNotThrow(() -> new Identifier("   abcdefgはるこ     "), "Valid Identifier test with trim");
		assertDoesNotThrow(() -> new Identifier("   abc__d9090efg''はるこ     "), "Valid Identifier test with non-alnum");
		assertThrows(CompilationError.class, () -> new Identifier("90abc__d9090efg"),
				"Invalid Identifier test with starting numeric");
	}

}
