package klfr.sof.test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.*;

import klfr.sof.lang.Stackable.DebugStringExtensiveness;
import klfr.sof.lang.primitive.BoolPrimitive;
import klfr.sof.lang.primitive.IntPrimitive;
import klfr.sof.lang.primitive.Primitive;
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
