package klfr.sof.test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.stream.Collectors;

import org.junit.jupiter.api.*;

import static klfr.Utility.*;
import klfr.sof.lang.*;
import klfr.sof.lib.*;

@DisplayName("Test the Builtins SOF builtin function implementor class")
class BuiltinFunctionsTest extends SofTestSuper {

	@Test
	@DisplayName("Test mostSignificantSetBit()")
	void testMSBSet() {
		assertEquals(0, Builtins.mostSignificantSetBit(0l), "Zero has no bits set");
		assertEquals(1, Builtins.mostSignificantSetBit(1l), "One has 1 bit set");
		assertEquals(5, Builtins.mostSignificantSetBit(0b10000l), "Small power of two");
		assertEquals(8, Builtins.mostSignificantSetBit(0b10101110l), "Larger number with multiple bits set/reset");
		assertEquals(64, Builtins.mostSignificantSetBit(0x8000000000000000l), "Edge case 64th bit set");
		assertEquals(63, Builtins.mostSignificantSetBit(0x4000000000000000l), "Edge case 63rd bit set");
		assertEquals(64, Builtins.mostSignificantSetBit(-1l), "Negative numbers work (see edge cases)");
	}

	@Test
	@DisplayName("Test SOF builtin function random:int")
	void testRandomInt() {
		ArrayList<Long> randomInts = new ArrayList<>();
		final double avg = (25l + 7l) / 2d, avgCountPerNum = 20000d / (25l - 7l);
		for (int i = 0; i < 20000; ++i)
			assertDoesNotThrow(() -> randomInts.add(
					Builtins.random(IntPrimitive.createIntPrimitive(7l), IntPrimitive.createIntPrimitive(25l)).value()));

		assertTrue(randomInts.parallelStream().allMatch(i -> ((i >= 7l) && (i <= 25l))), "RNG generation limits");

		final double actualAverage = randomInts.parallelStream()
				.collect(Collectors.averagingDouble(x -> x.doubleValue()));
		assertTrue(actualAverage >= avg - 1d && actualAverage <= avg + 1d,
				"Average value is approximately in the middle");

		assertTrue(
				randomInts.parallelStream().collect(Collectors.groupingByConcurrent(i -> i.longValue())).entrySet()
						.parallelStream().map(entry -> entry.getValue().size())
						.allMatch(count -> count > avgCountPerNum * 0.8d && count <= avgCountPerNum * 1.2d),
				"All values are no more than 20% from the expected mean");
	}

	@Test
	@DisplayName("Test entire string formatter")
	void testInternalFmt() {
		assertEquals("blah" + System.lineSeparator() + "blah", Formatting.internalFormat("blah%nblah"), "Simple newline format");
		assertEquals("blah", Formatting.internalFormat("blah"), "No format specifiers");
		assertEquals("A is 3.558 and 0013 hehe le    ", Formatting.internalFormat("A is %.3f and %04d hehe %<6s", new Stackable[]{FloatPrimitive.createFloatPrimitive(3.55806d), IntPrimitive.createIntPrimitive(13l), StringPrimitive.createStringPrimitive("le")}), "Some format specifiers");
	}

	@Test
	@DisplayName("Test formatting helper function fullDoubleToString()")
	void testDoubleToString() {
		assertEquals("0.000000000000", fullDoubleToString(0d), "Zero tostring");
		assertEquals("1.000000000000", fullDoubleToString(1d), "One tostring");
		assertEquals("3020190293.000000000000", fullDoubleToString(3020190293d), "Large number tostring (accounting for float inacuraccy)");
		assertEquals("3020190293.184730052947", fullDoubleToString(3020190293.18473d), "Large number with decimals tostring");
		assertEquals("3.141592653588", fullDoubleToString(3.141592653589d), "Small number with decimals tostring");
		// floating point inaccuracy, for god's sake you're killing me
		assertEquals("3.141528899999", fullDoubleToString(3.1415289d), "Small number with not full decimals tostring");
		assertEquals("-197.335000000000", fullDoubleToString(-197.335d), "Negative number tostring");
	}

	@Test
	@DisplayName("Test formatting functions - Float")
	void testFmtFloat() {
		var one = FloatPrimitive.createFloatPrimitive(1.0d);
		var zero = FloatPrimitive.createFloatPrimitive(0.0d);
		var dec = FloatPrimitive.createFloatPrimitive(3.141529d);
		var neg = FloatPrimitive.createFloatPrimitive(-5.67d);
		assertEquals("0", Formatting.handleFormatter("%f", 0, new Stackable[]{zero}).getRight(), "Simple float");
		assertEquals("0.0", Formatting.handleFormatter("%#f", 0, new Stackable[]{zero}).getRight(), "Float force decimal point");
		assertEquals("3.141529", Formatting.handleFormatter("%f", 0, new Stackable[]{dec}).getRight(), "Float, decimal, no precision");
		assertEquals("-5.67", Formatting.handleFormatter("%f", 0, new Stackable[]{neg}).getRight(), "Float, decimal, negative");
		assertEquals("3.14153", Formatting.handleFormatter("%.5f", 0, new Stackable[]{dec}).getRight(), "Float, decimal, precision & rounding");
		assertEquals("1.0000", Formatting.handleFormatter("%.4f", 0, new Stackable[]{one}).getRight(), "Float, precision & padding");
		assertEquals("+1", Formatting.handleFormatter("%+f", 0, new Stackable[]{one}).getRight(), "Float force sign");
		assertEquals("-5.67", Formatting.handleFormatter("%+f", 0, new Stackable[]{neg}).getRight(), "Float force sign, negative");
		assertEquals(" 1", Formatting.handleFormatter("% f", 0, new Stackable[]{one}).getRight(), "Float force sign space");
		assertEquals("  3.1415", Formatting.handleFormatter("%8.4f", 0, new Stackable[]{dec}).getRight(), "Float precision and width");
	}

	@Test
	@DisplayName("Test formatting functions - Integers")
	void testFmtInt() {
		// 1b, 1h, 1o
		var one = IntPrimitive.createIntPrimitive(1l);
		var zero = IntPrimitive.createIntPrimitive(0l);
		// 1011111110111110011001b, 2fef99h, 13767631o
		var large = IntPrimitive.createIntPrimitive(3141529l);
		// -1000110111b -237h, -1067o
		var neg = IntPrimitive.createIntPrimitive(-567l);
		assertEquals("0", Formatting.handleFormatter("%d", 0, new Stackable[]{zero}).getRight(), "Simple int");
		assertEquals("001", Formatting.handleFormatter("%.3i", 0, new Stackable[]{one}).getRight(), "Int, precision");
		assertEquals("-567", Formatting.handleFormatter("%d", 0, new Stackable[]{neg}).getRight(), "Int, sign");
		assertEquals("+1", Formatting.handleFormatter("%+i", 0, new Stackable[]{one}).getRight(), "Int, force sign");
		assertEquals("-567", Formatting.handleFormatter("%+d", 0, new Stackable[]{neg}).getRight(), "Int negative, force sign");
		assertEquals(" 3141529", Formatting.handleFormatter("% i", 0, new Stackable[]{large}).getRight(), "Int, force sign space");

		assertEquals("2fef99", Formatting.handleFormatter("%x", 0, new Stackable[]{large}).getRight(), "Hex int lowercase");
		assertEquals("2FEF99", Formatting.handleFormatter("%X", 0, new Stackable[]{large}).getRight(), "Hex int uppercase");
		assertEquals("0x2fef99", Formatting.handleFormatter("%#x", 0, new Stackable[]{large}).getRight(), "Hex int force 0x");
		assertEquals("0X2FEF99", Formatting.handleFormatter("%#X", 0, new Stackable[]{large}).getRight(), "Hex int uppercase force 0X");
		assertEquals("-237", Formatting.handleFormatter("%x", 0, new Stackable[]{neg}).getRight(), "Hex int negative");
		assertEquals("-0x237", Formatting.handleFormatter("%#x", 0, new Stackable[]{neg}).getRight(), "Hex int negative & force 0x");

		assertEquals("1011111110111110011001", Formatting.handleFormatter("%b", 0, new Stackable[]{large}).getRight(), "Binary int");
		assertEquals("-1000110111", Formatting.handleFormatter("%b", 0, new Stackable[]{neg}).getRight(), "Binary int negative");

		assertEquals("13767631", Formatting.handleFormatter("%o", 0, new Stackable[]{large}).getRight(), "Octal int");
		assertEquals("013767631", Formatting.handleFormatter("%#o", 0, new Stackable[]{large}).getRight(), "Octal int force 0");
		assertEquals("-1067", Formatting.handleFormatter("%o", 0, new Stackable[]{neg}).getRight(), "Octal int negative");
		assertEquals("-01067", Formatting.handleFormatter("%#o", 0, new Stackable[]{neg}).getRight(), "Octal int negative & force 0");
	}

	@Test
	@DisplayName("Test formatting functions - String & Padding")
	void testFmtString() {
		var str = StringPrimitive.createStringPrimitive("abc");
		var value = new Identifier("blah");
		assertEquals("abc", Formatting.handleFormatter("%s", 0, new Stackable[]{str}).getRight(), "Simple string");
		assertEquals("blah", Formatting.handleFormatter("%s", 0, new Stackable[]{value}).getRight(), "Tostring");

		assertEquals("  abc", Formatting.handleFormatter("%5s", 0, new Stackable[]{str}).getRight(), "String right-justify");
		assertEquals("abc", Formatting.handleFormatter("%2s", 0, new Stackable[]{str}).getRight(), "String right-justify over length");
		assertEquals("abc    ", Formatting.handleFormatter("%<7s", 0, new Stackable[]{str}).getRight(), "String left-justify");
		assertEquals("abc", Formatting.handleFormatter("%<1s", 0, new Stackable[]{str}).getRight(), "String left-justify over length");
		assertEquals(" abc ", Formatting.handleFormatter("%^5s", 0, new Stackable[]{str}).getRight(), "String center-justify");
		assertEquals("abc", Formatting.handleFormatter("%^3s", 0, new Stackable[]{str}).getRight(), "String center-justify over length");
		assertEquals(" blah", Formatting.handleFormatter("%^5s", 0, new Stackable[]{value}).getRight(), "String center-justify unevenly");

		assertEquals("0000abc", Formatting.handleFormatter("%07s", 0, new Stackable[]{str}).getRight(), "String pad with zeroes");
	}

	@Test
	@DisplayName("Test formatting functions - Newline")
	void testFmtNewline() {
		assertEquals(System.lineSeparator(), Formatting.handleFormatter("%n", 0, new Stackable[]{}).getRight(), "Newline format specifier");
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
