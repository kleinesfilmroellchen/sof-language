package klfr.sof.test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.stream.Collectors;

import org.junit.jupiter.api.*;

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

		// final List<Tuple<Long, Integer>> counts = randomInts.parallelStream()
		// .collect(Collectors.groupingByConcurrent(i ->
		// i.longValue())).entrySet().parallelStream()
		// .map(entry -> new Tuple<Long, Integer>(entry.getKey(),
		// entry.getValue().size()))
		// .collect(Collectors.toList());
		assertTrue(
				randomInts.parallelStream().collect(Collectors.groupingByConcurrent(i -> i.longValue())).entrySet()
						.parallelStream().map(entry -> entry.getValue().size())
						.allMatch(count -> count > avgCountPerNum * 0.8d && count <= avgCountPerNum * 1.2d),
				"All values are no more than 20% from the expected mean");
	}

}

/*
 * The SOF programming language interpreter. Copyright (C) 2019-2020
 * kleinesfilmröllchen
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 */
