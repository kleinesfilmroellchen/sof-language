package klfr.sof.test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import klfr.sof.lang.Callable.CallProvider;
import klfr.sof.lang.Stackable.DebugStringExtensiveness;
import klfr.sof.CompilerException;
import klfr.sof.lang.BoolPrimitive;
import klfr.sof.lang.IntPrimitive;
import klfr.sof.lang.Primitive;

@SuppressWarnings("unchecked")
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
	void testGetCallProvider() {
		CallProvider lambda = assertDoesNotThrow(() -> i.getCallProvider());
		IntPrimitive result = (IntPrimitive) assertDoesNotThrow(() -> lambda.call(null));
		assertTrue(i.equals(result), "Primitive calling should return the primitive itself copied");
	}

	@Test
	void testCreateIntegerFromString() {
		IntPrimitive j = assertDoesNotThrow(() -> IntPrimitive.createIntegerFromString(" 123"));
		assertEquals(123l, j.value());
		assertDoesNotThrow(() -> IntPrimitive.createIntegerFromString("\n0h44af  "));
		assertDoesNotThrow(() -> IntPrimitive.createIntegerFromString("-0b1010011  "));
		assertDoesNotThrow(() -> IntPrimitive.createIntegerFromString(" +0o776352"));
		assertDoesNotThrow(() -> IntPrimitive.createIntegerFromString("		-0d490"));
		assertThrows(CompilerException.class, () -> IntPrimitive.createIntegerFromString("jksdf"));
		assertThrows(CompilerException.class, () -> IntPrimitive.createIntegerFromString("	0xiwo3i"));
		assertThrows(CompilerException.class, () -> IntPrimitive.createIntegerFromString("0b8373"));
		assertThrows(CompilerException.class, () -> IntPrimitive.createIntegerFromString("0f879"));
	}

	@Test
	void testCreateBooleanFromString() {
		BoolPrimitive b = assertDoesNotThrow(() -> BoolPrimitive.createBoolFromString("True"));
		assertEquals(true, b.value());
		assertDoesNotThrow(() -> BoolPrimitive.createBoolFromString("FALSE"));
		assertDoesNotThrow(() -> BoolPrimitive.createBoolFromString("trUe"));
		assertThrows(CompilerException.class, () -> BoolPrimitive.createBoolFromString("Trueblah"));
		assertThrows(CompilerException.class, () -> BoolPrimitive.createBoolFromString("FALSEfalse"));
	}

}
