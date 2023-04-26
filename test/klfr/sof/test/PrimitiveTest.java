package klfr.sof.test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.*;

import klfr.sof.lang.BuiltinOperations;
import klfr.sof.lang.Nametable;
import klfr.sof.lang.Stackable.DebugStringExtensiveness;
import klfr.sof.lang.primitive.*;
import klfr.sof.lib.ChurchNumeral;
import klfr.sof.exceptions.*;

@DisplayName("Test SOF primitives")
class PrimitiveTest extends SofTestSuper {

	static IntPrimitive		i;
	static FloatPrimitive	d;

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
		assertThrows(IncompleteCompilerException.class, () -> FloatPrimitive.createFloatFromString("jksdf"));
		assertThrows(IncompleteCompilerException.class, () -> FloatPrimitive.createFloatFromString("	778"));
		assertThrows(IncompleteCompilerException.class, () -> FloatPrimitive.createFloatFromString("107.304e12"));
		assertThrows(IncompleteCompilerException.class, () -> FloatPrimitive.createFloatFromString(".666"));
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
		assertThrows(IncompleteCompilerException.class, () -> IntPrimitive.createIntegerFromString("jksdf"));
		assertThrows(IncompleteCompilerException.class, () -> IntPrimitive.createIntegerFromString("	0xiwo3i"));
		assertThrows(IncompleteCompilerException.class, () -> IntPrimitive.createIntegerFromString("0b8373"));
		assertThrows(IncompleteCompilerException.class, () -> IntPrimitive.createIntegerFromString("0f879"));
	}

	@DisplayName("Test the creation of boolean primitives from string")
	@Test
	void testCreateBooleanFromString() {
		BoolPrimitive b = assertDoesNotThrow(() -> BoolPrimitive.createBoolFromString("True"));
		assertEquals(true, b.value());
		assertDoesNotThrow(() -> BoolPrimitive.createBoolFromString("FALSE"));
		assertDoesNotThrow(() -> BoolPrimitive.createBoolFromString("trUe"));
		assertThrows(IncompleteCompilerException.class, () -> BoolPrimitive.createBoolFromString("Trueblah"));
		assertThrows(IncompleteCompilerException.class, () -> BoolPrimitive.createBoolFromString("FALSEfalse"));
	}

	@DisplayName("Test the list primitive")
	@Test
	void testListPrimitive() {
		// these tests are pretty basic because all of the code should delegate to java lists
		var l = assertDoesNotThrow(() -> new ListPrimitive());
		IntPrimitive five = IntPrimitive.createIntPrimitive(5l), ten = IntPrimitive.createIntPrimitive(10l);
		assertDoesNotThrow(() -> l.add(five));
		assertEquals(five, l.get(0), "First element in the list");
		assertEquals(1, l.size(), "List size");
		assertDoesNotThrow(() -> l.add(five), "Add elements twice");
		l.add(ten);
		assertEquals(ten, l.get(2), "Add to the end");
		assertEquals(five, l.get(1), "Get from specific location");

		assertEquals(3, l.size());
		assertDoesNotThrow(() -> l.remove(1));
		assertEquals(2, l.size(), "Size after removing");
		assertEquals(ten, l.get(1), "Other elements were shifted");

		assertDoesNotThrow(() -> l.clear());
		assertEquals(0, l.size(), "Size after clearing");
		assertThrows(IndexOutOfBoundsException.class, () -> l.get(3), "Index out of bounds");
	}

	@DisplayName("Test type-incompatible builtin operations")
	@Test
	void testIncompatiblePrimitives() {
		final var floatPrimitive = FloatPrimitive.createFloatPrimitive(8.7d);
		final var intPrimitive = IntPrimitive.createIntPrimitive(78l);
		final var stringPrimitive = StringPrimitive.createStringPrimitive("test");
		final var boolPrimitive = BoolPrimitive.createBoolPrimitive(true);
		final var churchNumeral = new ChurchNumeral(89l);
		assertThrows(IncompleteCompilerException.class, () -> BuiltinOperations.add(floatPrimitive, stringPrimitive));
		assertThrows(IncompleteCompilerException.class, () -> BuiltinOperations.divide(intPrimitive, churchNumeral));
		assertThrows(IncompleteCompilerException.class, () -> BuiltinOperations.subtract(boolPrimitive, stringPrimitive));
		assertThrows(IncompleteCompilerException.class, () -> BuiltinOperations.multiply(intPrimitive, boolPrimitive));
		assertThrows(IncompleteCompilerException.class, () -> BuiltinOperations.modulus(floatPrimitive, boolPrimitive));
		assertThrows(IncompleteCompilerException.class, () -> BuiltinOperations.lessThan(churchNumeral, stringPrimitive));
		assertThrows(IncompleteCompilerException.class, () -> BuiltinOperations.lessEqualThan(intPrimitive, stringPrimitive));
		assertThrows(IncompleteCompilerException.class, () -> BuiltinOperations.greaterThan(boolPrimitive, stringPrimitive));
		assertThrows(IncompleteCompilerException.class, () -> BuiltinOperations.greaterEqualThan(intPrimitive, boolPrimitive));
	}

	@DisplayName("Test Church numerals")
	@Test
	void testChurchNumeral() {
		final var churchNumeral = new ChurchNumeral(17l);
		assertFalse(IntPrimitive.createIntPrimitive(57l).equals(churchNumeral));
		assertTrue(churchNumeral.equals(churchNumeral));
		assertFalse(churchNumeral.equals(new ChurchNumeral(0l)));
		assertFalse(churchNumeral.equals(new Nametable()));
		assertTrue(churchNumeral.copy().equals(churchNumeral));
		assertTrue(churchNumeral.equals(IntPrimitive.createIntPrimitive(churchNumeral.value())));
		assertFalse(churchNumeral.equals(IntPrimitive.createIntPrimitive(0l)));
		assertTrue(churchNumeral.compareTo(IntPrimitive.createIntPrimitive(10l)) > 0);
		assertTrue(churchNumeral.compareTo(new ChurchNumeral(200)) < 0);
		assertTrue(churchNumeral.isTrue());
		assertFalse(churchNumeral.isFalse());
		assertFalse(new ChurchNumeral(0l).isTrue());
		assertTrue(new ChurchNumeral(0l).isFalse());
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
