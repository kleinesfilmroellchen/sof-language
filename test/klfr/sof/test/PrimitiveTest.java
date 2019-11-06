package klfr.sof.test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import klfr.sof.lang.Callable.CallProvider;
import klfr.sof.CompilationError;
import klfr.sof.lang.Primitive;

class PrimitiveTest {

	static Primitive<Long> i;

	@BeforeEach
	void setUp() {
		i = new Primitive<Long>(20l);
	}

	/**
	 * Tests basic functionalities of Primitive as well as the numberChars static
	 * field & its initializer.
	 */
	@Test
	void testPrimitive() {
		Map<Character, Integer> nums = assertDoesNotThrow(() -> Primitive.numberChars);
		assertTrue(() -> nums.size() > 0);
		assertDoesNotThrow(() -> i.getValue());
		assertDoesNotThrow(() -> i.getDebugDisplay());
		assertDoesNotThrow(() -> i.toString());
		assertDoesNotThrow(() -> i.toOutputString());
		Primitive<Long> clone = (Primitive<Long>) assertDoesNotThrow(() -> i.clone());
		assertTrue(i.equals(clone));
		assertFalse(i.equals(new Primitive<Long>(30l)));
	}

	@Test
	void testGetCallProvider() {
		CallProvider lambda = assertDoesNotThrow(() -> i.getCallProvider());
		Primitive<Long> result = (Primitive<Long>) assertDoesNotThrow(() -> lambda.call(null));
		assertEquals(i, result, "Primitive calling should return the primitive itself");
	}

	@Test
	void testCreateInteger() {
		Primitive<Long> j = assertDoesNotThrow(() -> Primitive.createInteger(" 123"));
		assertEquals(123l, j.getValue());
		assertDoesNotThrow(() -> Primitive.createInteger("\n0h44af  "));
		assertDoesNotThrow(() -> Primitive.createInteger("-0b1010011  "));
		assertDoesNotThrow(() -> Primitive.createInteger(" +0o776352"));
		assertDoesNotThrow(() -> Primitive.createInteger("		-0d490"));
		assertThrows(CompilationError.class, () -> Primitive.createInteger("jksdf"));
		assertThrows(CompilationError.class, () -> Primitive.createInteger("	0xiwo3i"));
		assertThrows(CompilationError.class, () -> Primitive.createInteger("0b8373"));
		assertThrows(CompilationError.class, () -> Primitive.createInteger("0o879"));
	}

	@Test
	void testCreateBoolean() {
		Primitive<Boolean> b = assertDoesNotThrow(() -> Primitive.createBoolean("True"));
		assertEquals(true, b.getValue());
		assertDoesNotThrow(() -> Primitive.createBoolean("FALSE"));
		assertDoesNotThrow(() -> Primitive.createBoolean("trUe"));
		assertThrows(CompilationError.class, () -> Primitive.createBoolean("Trueblah"));
		assertThrows(CompilationError.class, () -> Primitive.createBoolean("FALSEfalse"));
	}

}
