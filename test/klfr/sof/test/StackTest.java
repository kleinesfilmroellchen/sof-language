package klfr.sof.test;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import klfr.sof.Interpreter;
import klfr.sof.lang.Nametable;
import klfr.sof.lang.Stack;

class StackTest {
	
	private Stack stack;

	@BeforeEach
	void setUp() throws Exception {
		stack = new Stack(new Interpreter());
		stack.push(new Nametable());
	}
	
	/**
	 * Fundamental class tests.
	 */
	@Test
	void testStack() {
		var s = assertDoesNotThrow( ()-> new Stack(new Interpreter()));
	}

	/**
	 * Tests basic stacking functions, such as push and pop.
	 */
	@Test
	void testStacking() {
		fail("Not yet implemented");
	}

	@Test
	void testNametables() {
		fail("Not yet implemented");
	}

	@Test
	void testScoping() {
		fail("Not yet implemented");
	}
}
