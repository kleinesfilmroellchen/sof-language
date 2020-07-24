package klfr.sof.test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.*;

import klfr.sof.lang.Stackable.DebugStringExtensiveness;
import klfr.sof.*;
import klfr.sof.lang.*;

class PrimitiveTest extends SofTestSuper {

	static IntPrimitive i;

	@BeforeEach
	void setUp() {
		i = IntPrimitive.createIntPrimitive(20l);
	}

	/**
	 * Tests basic functionalities of Primitive as well as the numberChars static
	 * field & its initializer.
	 */
	@Test
	void testIntPrimitive() {
		assertDoesNotThrow(() -> IntPrimitive.createIntPrimitive(42l));
		Map<Character, Integer> nums = assertDoesNotThrow(() -> Primitive.numberChars);
		assertTrue(() -> nums.size() > 0);
		assertDoesNotThrow(() -> i.value());
		assertDoesNotThrow(() -> i.toDebugString(DebugStringExtensiveness.Full));
		assertDoesNotThrow(() -> i.print());
		IntPrimitive clone = (IntPrimitive) assertDoesNotThrow(() -> i.copy());
		assertTrue(i.equals(clone));
		assertFalse(i.equals(IntPrimitive.createIntPrimitive(30l)));
	}

	@Test
	void testCreateIntegerFromString() {
		IntPrimitive j = assertDoesNotThrow(() -> IntPrimitive.createIntegerFromString(" 123"));
		assertEquals(123l, j.value());
		assertDoesNotThrow(() -> IntPrimitive.createIntegerFromString("\n0h44af  "));
		assertDoesNotThrow(() -> IntPrimitive.createIntegerFromString("-0b1010011  "));
		assertDoesNotThrow(() -> IntPrimitive.createIntegerFromString(" +0o776352"));
		assertDoesNotThrow(() -> IntPrimitive.createIntegerFromString("		-0d490"));
		assertThrows(CompilerException.Incomplete.class, () -> IntPrimitive.createIntegerFromString("jksdf"));
		assertThrows(CompilerException.Incomplete.class, () -> IntPrimitive.createIntegerFromString("	0xiwo3i"));
		assertThrows(CompilerException.Incomplete.class, () -> IntPrimitive.createIntegerFromString("0b8373"));
		assertThrows(CompilerException.Incomplete.class, () -> IntPrimitive.createIntegerFromString("0f879"));
	}

	@Test
	void testCreateBooleanFromString() {
		BoolPrimitive b = assertDoesNotThrow(() -> BoolPrimitive.createBoolFromString("True"));
		assertEquals(true, b.value());
		assertDoesNotThrow(() -> BoolPrimitive.createBoolFromString("FALSE"));
		assertDoesNotThrow(() -> BoolPrimitive.createBoolFromString("trUe"));
		assertThrows(CompilerException.Incomplete.class, () -> BoolPrimitive.createBoolFromString("Trueblah"));
		assertThrows(CompilerException.Incomplete.class, () -> BoolPrimitive.createBoolFromString("FALSEfalse"));
	}

}
