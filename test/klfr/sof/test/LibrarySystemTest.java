package klfr.sof.test;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.util.Optional;

import org.junit.jupiter.api.*;

import klfr.sof.lang.*;
import klfr.sof.lang.primitive.FloatPrimitive;
import klfr.sof.lang.primitive.IntPrimitive;
import klfr.sof.lang.primitive.StringPrimitive;
import klfr.sof.lib.NativeFunctionRegistry;
import klfr.sof.lib.NativeFunctionRegistry.*;

import static klfr.sof.lib.NativeFunctionRegistry.generateDescriptor;

/**
 * This test suite tests several parts of the library system. It needs to be public because some of its public static methods are used as test methods for native function registration and execution.
 */
@SuppressWarnings("unused")
public class LibrarySystemTest extends SofTestSuper {

	public static final String ntm1Name = "klfr.sof.test.LibrarySystemTest#namingTestMethod1()";
	public static final String ntm2Name = "klfr.sof.test.LibrarySystemTest#namingTestMethod2(StringPrimitive)";
	public static final String ntm3Name = "klfr.sof.test.LibrarySystemTest#namingTestMethod3(FloatPrimitive,Stackable)";

	public static IntPrimitive namingTestMethod1() { return IntPrimitive.createIntPrimitive(42l); }
	public static void namingTestMethod2(StringPrimitive dummyArgument) {}
	public static void namingTestMethod3(FloatPrimitive dummyArgument1, Stackable dummyArgument2) {}

	@DisplayName("Test native function naming")
	@Test
	void testNFName() {
		try {
			Method m1 = LibrarySystemTest.class.getMethod("namingTestMethod1"),
					m2 = LibrarySystemTest.class.getMethod("namingTestMethod2", StringPrimitive.class),
					m3 = LibrarySystemTest.class.getMethod("namingTestMethod3", FloatPrimitive.class, Stackable.class);
			assertEquals(ntm1Name, generateDescriptor(m1));
			assertEquals(ntm2Name, generateDescriptor(m2));
			assertEquals(ntm3Name, generateDescriptor(m3));
		} catch (NoSuchMethodException e) {
			fail("Test dummy method was not found.", e);
		}
	}

	@DisplayName("Test native function discovery and registration")
	@Test
	void testRegister() {
		var nfr = new NativeFunctionRegistry();
		assertDoesNotThrow(() -> nfr.registerNativeFunctions(LibrarySystemTest.class));
		// there should now be three functions registered
		final var m1funcN = assertDoesNotThrow(() -> nfr.getNativeFunction(ntm1Name).orElseThrow(), "Function 1 was registered and can be retrieved");
		final var m2funcN = assertDoesNotThrow(() -> nfr.getNativeFunction(ntm2Name).orElseThrow(), "Function 2 was registered and can be retrieved");
		final var m3funcN = assertDoesNotThrow(() -> nfr.getNativeFunction(ntm3Name).orElseThrow(), "Function 3 was registered and can be retrieved");
		assertTrue(Optional.empty().equals(nfr.getNativeFunction("not a function")), "unregistered random string has no function name assigned");
		try {
			final var m1func = (Native0ArgFunction) m1funcN;
			final var m2func = (Native1ArgFunction) m2funcN;
			//final var m3func = (Native2ArgFunction) m3funcN;

			final var res1 = assertDoesNotThrow(() -> m1func.call(), "Function 1 call does not throw");
			assertTrue(IntPrimitive.createIntPrimitive(42l).equals(res1), "Function 1 call returns 42");

			final var res2 = assertDoesNotThrow(() -> m2func.call(StringPrimitive.createStringPrimitive("123")), "Function 2 call does not throw");
			assertTrue(res2 == null, "Function 2 returns null");

		} catch (ClassCastException e) {
			fail("Function is of incorrect argument count after registration", e);
		}
	}

}

/*
The SOF programming language interpreter. Copyright (C) 2019-2020
kleinesfilmröllchen

This program is free software: you can redistribute it and/or modify it under
the terms of the GNU General Public License as published by the Free Software
Foundation, either version 3 of the License, or (at your option) any later
version.

This program is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
details.

You should have received a copy of the GNU General Public License along with
this program. If not, see <https://www.gnu.org/licenses/>.
*/
