package klfr.sof.test;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import klfr.sof.exceptions.CompilerException;
import klfr.sof.exceptions.IncompleteCompilerException;
import klfr.sof.lang.Identifier;
import klfr.sof.lang.Nametable;
import klfr.sof.lang.Stack;
import klfr.sof.lang.Stackable;
import klfr.sof.lang.StackableName;
import klfr.sof.lang.Stackable.DebugStringExtensiveness;

@DisplayName("Test the SOF stack")
public class StackTest extends SofTestSuper {

	private Stack		stack;
	private Nametable	nt;

	@StackableName("StubType")
	private static class Sbl implements Stackable {

		private static final long serialVersionUID = 1L;

		@Override
		public Stackable copy() {
			return new Sbl();
		}

		@Override
		public boolean equals(Stackable other) {
			return false;
		}
	}

	@BeforeEach
	void setUp() throws Exception {
		stack = new Stack();
		nt = new Nametable();
	}

	@DisplayName("Basic stacking functions, such as push and pop")
	@Test
	void testStack() throws CompilerException, IncompleteCompilerException {
		var a = new Sbl();
		var b = new Sbl();

		assertDoesNotThrow(() -> stack.push(a), "Push Stackable works");
		assertEquals(a, assertDoesNotThrow(() -> stack.peekSafe(), "Peek works"), "Peek equivalence check");
		assertEquals(1, stack.size(), "Stack size with one element");

		assertEquals(a, assertDoesNotThrow(() -> stack.popSafe(), "Pop works"), "Pop equivalence check");
		assertEquals(0, stack.size(), "Stack size with zero elements");

		stack.push(a);
		stack.push(b);
		assertEquals(2, stack.size(), "Stack size with two elements");
		assertEquals(b, stack.peekSafe(), "Stack push order");
		assertEquals(a, assertDoesNotThrow(() -> stack.getLastSafe(), "getLast works"), "Stack lowest element correctness");

		stack.popSafe();
		assertEquals(a, stack.peekSafe(), "Stack pop order");
		assertEquals(1, stack.size(), "Stack size");
		stack.popSafe();

		assertThrows(IncompleteCompilerException.class, () -> stack.popSafe(), "Stack emptiness with Pop throws");
		assertThrows(IncompleteCompilerException.class, () -> stack.getLastSafe(), "Stack emptiness with getLast throws");
		assertThrows(IncompleteCompilerException.class, () -> stack.peekSafe(), "Stack emptiness with peek throws");
	}

	@DisplayName("Basic nametable functionality")
	@Test
	void testNametables() throws IncompleteCompilerException {
		final var idA = new Identifier("a");
		final var idB = new Identifier("Beta");

		assertEquals(0, nt.size(), "Empty nametable");
		assertDoesNotThrow(() -> nt.put(new Identifier("a"), new Sbl()), "Nametable put");

		assertTrue(nt.hasMapping(idA), "Nametable mapping exists for 'a'");
		assertFalse(nt.hasMapping(idB), "Nametable mapping for random identifier does not exist");

		final var value = new Sbl();
		nt.put(idA, value);
		assertEquals(value, assertDoesNotThrow(() -> nt.get(idA)), "Retrieve mapping for 'a'");
		assertEquals(value, nt.get(new Identifier("a")), "Retrieve mapping for 'a' (new identifier object)");

		// just convenience to use the B-identifier for value here
		nt.put(idB, idB);
		assertEquals(value, nt.get(idA), "New mapping doesn't change existing mapping");
		assertEquals(idB, nt.get(idB), "Sanity check new mapping");
		assertEquals(2, nt.size(), "Nametable with 2 elements");

		final var ids = assertDoesNotThrow(() -> nt.identifiers());
		assertTrue(ids.contains(idA), "Mapping contains first identifier");
		assertTrue(ids.contains(idB), "Mapping contains second identifier");

		assertDoesNotThrow(() -> nt.toDebugString(DebugStringExtensiveness.Compact), "Debug string (Compact)");
		assertDoesNotThrow(() -> nt.toDebugString(DebugStringExtensiveness.Type), "Debug string (Type)");
		assertDoesNotThrow(() -> nt.toDebugString(DebugStringExtensiveness.Full), "Debug string (Full)");
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
