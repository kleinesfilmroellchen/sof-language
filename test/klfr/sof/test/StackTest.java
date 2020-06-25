package klfr.sof.test;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import klfr.sof.CompilerException;
import klfr.sof.lang.Identifier;
import klfr.sof.lang.Nametable;
import klfr.sof.lang.Stack;
import klfr.sof.lang.Stackable;
import klfr.sof.lang.StackableName;
import klfr.sof.lang.Stackable.DebugStringExtensiveness;

class StackTest extends SofTestSuper {

	private Stack stack;
	private Nametable nt;

	@StackableName("StubType")
	private static class Sbl implements Stackable {
		private static final long serialVersionUID = 1L;
		@Override
		public Stackable copy() {
			return new Sbl();
		}
	}

	@BeforeEach
	void setUp() throws Exception {
		stack = new Stack();
		nt = new Nametable();
	}

	/**
	 * Tests basic stacking functions, such as push and pop.
	 */
	@Test
	void testStack() {
		var a = new Sbl();
		var b = new Sbl();

		assertDoesNotThrow(() -> stack.push(a), "Push Stackable works");
		assertEquals(a, assertDoesNotThrow(() -> stack.peek(), "Peek works"), "Peek equivalence check");
		assertEquals(1, stack.size(), "Stack size with one element");

		assertEquals(a, assertDoesNotThrow(() -> stack.pop(), "Pop works"), "Pop equivalence check");
		assertEquals(0, stack.size(), "Stack size with zero elements");

		stack.push(a); stack.push(b);
		assertEquals(2, stack.size(), "Stack size with two elements");
		assertEquals(b, stack.peek(), "Stack push order");
		assertEquals(a, assertDoesNotThrow(() -> stack.getLast(), "getLast works"), "Stack lowest element correctness");

		stack.pop();
		assertEquals(a, stack.peek(), "Stack pop order");
		assertEquals(1, stack.size(), "Stack size");
		stack.pop();

		assertThrows(CompilerException.class, () -> stack.pop(), "Stack emptiness with Pop throws");
		assertThrows(CompilerException.class, () -> stack.getLast(), "Stack emptiness with getLast throws");
		assertThrows(CompilerException.class, () -> stack.peek(), "Stack emptiness with peek throws");
	}

	/**
	 * Test the basic nametable functionality.
	 */
	@Test
	void testNametables() {
		final var idA = new Identifier("a");
		final var idB = new Identifier("Beta");

		assertEquals(0, nt.size(), "Empty nametable");
		assertDoesNotThrow(()-> nt.put(new Identifier("a"), new Sbl()), "Nametable put");

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

	/**
	 * Test the scoping functionality that concerns both Stack and Nametable.
	 */
	@Test
	void testScoping() {
		fail("Not yet implemented");
	}
}
