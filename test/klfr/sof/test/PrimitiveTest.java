package klfr.sof.test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.*;

import klfr.sof.lang.Stackable.DebugStringExtensiveness;
import klfr.sof.lang.primitive.*;
import klfr.sof.*;
import klfr.sof.lang.*;

class PrimitiveTest extends SofTestSuper {

	static IntPrimitive i;
	static FloatPrimitive d;

	@BeforeEach
	void setUp() {
		i = IntPrimitive.createIntPrimitive(20l);
		d = FloatPrimitive.createFloatPrimitive(25.7619d);
	}

	@DisplayName("Test string primitive methods")
	@Test
	void testStringPrimitive() {
		assertDoesNotThrow(() -> StringPrimitive.createStringPrimitive("abc blah\n"));
		assertDoesNotThrow(() -> StringPrimitive.createStringPrimitive("88 blah\t").value());
		assertDoesNotThrow(() -> StringPrimitive.createStringPrimitive("abcüüßah\u8974"));
		assertDoesNotThrow(() -> StringPrimitive.createStringPrimitive(""));
		var str = assertDoesNotThrow(() -> StringPrimitive.createStringPrimitive("hehe"));
		assertEquals("hehe", str.value());
		assertTrue(str.compareTo(StringPrimitive.createStringPrimitive("a")) > 0, "Lexical comparison greater");
		assertTrue(str.compareTo(StringPrimitive.createStringPrimitive("zzzz")) < 0, "Lexical comparison smaller ");
		assertTrue(str.compareTo(StringPrimitive.createStringPrimitive("hehe")) == 0, "Lexical comparison equals");
	}

	@DisplayName("Test float primitive methods")
	@Test
	void testFloatPrimitive() {
		assertDoesNotThrow(() -> FloatPrimitive.createFloatPrimitive(-42.887d));
		assertEquals(-20.76d, FloatPrimitive.createFloatPrimitive(-20.76d).value());
		assertDoesNotThrow(() -> d.print());
		FloatPrimitive clone = (FloatPrimitive) assertDoesNotThrow(() -> d.copy());
		assertTrue(d.equals(clone));
		assertFalse(d.equals(FloatPrimitive.createFloatPrimitive(107.44d)));
		
		// all debug string extensivenesses should not throw
		assertDoesNotThrow(() -> {
			d.toDebugString(DebugStringExtensiveness.Type);
			d.toDebugString(DebugStringExtensiveness.Compact);
			d.toDebugString(DebugStringExtensiveness.Full);
		});
	}

	@DisplayName("Test integer primitive methods")
	@Test
	void testIntPrimitive() {
		assertDoesNotThrow(() -> IntPrimitive.createIntPrimitive(42l));
		Map<Character, Integer> nums = assertDoesNotThrow(() -> Primitive.numberChars);
		assertTrue(() -> nums.size() > 0);
		assertEquals(40091l, IntPrimitive.createIntPrimitive(40091l).value());
		assertDoesNotThrow(() -> i.print());
		IntPrimitive clone = (IntPrimitive) assertDoesNotThrow(() -> i.copy());
		assertTrue(i.equals(clone));
		assertFalse(i.equals(IntPrimitive.createIntPrimitive(30l)));

		// all debug string extensivenesses should not throw
		assertDoesNotThrow(() -> {
			i.toDebugString(DebugStringExtensiveness.Type);
			i.toDebugString(DebugStringExtensiveness.Compact);
			i.toDebugString(DebugStringExtensiveness.Full);
		});
	}

	@DisplayName("Test the creation of float primitives from string")
	@Test
	void testCreateFloatFromString() {
		FloatPrimitive j = assertDoesNotThrow(() -> FloatPrimitive.createFloatFromString(" 103.887"));
		assertEquals(103.887d, j.value());
		assertDoesNotThrow(() -> FloatPrimitive.createFloatFromString("\n0.0  "));
		assertDoesNotThrow(() -> FloatPrimitive.createFloatFromString("-7890.887100  "));
		assertDoesNotThrow(() -> FloatPrimitive.createFloatFromString(" +229.6e+5"));
		assertDoesNotThrow(() -> FloatPrimitive.createFloatFromString("		-0.77E-3"));
		assertThrows(CompilerException.Incomplete.class, () -> FloatPrimitive.createFloatFromString("jksdf"));
		assertThrows(CompilerException.Incomplete.class, () -> FloatPrimitive.createFloatFromString("	778"));
		assertThrows(CompilerException.Incomplete.class, () -> FloatPrimitive.createFloatFromString("107.304e12"));
		assertThrows(CompilerException.Incomplete.class, () -> FloatPrimitive.createFloatFromString(".666"));
	}

	@DisplayName("Test the creation of integer primitives from string")
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

	@DisplayName("Test the creation of boolean primitives from string")
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
