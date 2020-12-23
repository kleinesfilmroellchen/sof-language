package klfr.sof.test;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import klfr.sof.exceptions.*;
import klfr.sof.lang.*;
import klfr.sof.lang.Stackable.DebugStringExtensiveness;
import klfr.sof.lang.functional.FunctionDelimiter;
import klfr.sof.lang.primitive.IntPrimitive;

class SmallClassesTests extends SofTestSuper {

	/**
	 * Tests the stub classes in all packages. These are: ScopeDelimiter
	 * FunctionDelimiter
	 */
	@Test
	void testStubClasses() {
		assertDoesNotThrow(() -> new FunctionDelimiter());
	}

	@Test
	void testNametable() throws IncompleteCompilerException {
		// For the record:
		// This is the first piece of code that klfr used the 'var' type inference
		// statement in. Although this feature has existed since Java 10, I have not
		// noticed it until 11/2019 Lol. I love Java.
		var nt = assertDoesNotThrow(() -> new Nametable());
		var p = IntPrimitive.createIntPrimitive(123l);
		var id = new Identifier("abc");
		assertDoesNotThrow(() -> nt.put(id, p));
		var recievedP = nt.get(new Identifier("abc"));
		assertEquals(recievedP, p);
		assertTrue(nt.identifiers().contains(id));
		assertDoesNotThrow(() -> nt.toDebugString(DebugStringExtensiveness.Full));
		assertDoesNotThrow(() -> nt.print());
		Nametable clone = (Nametable) assertDoesNotThrow(() -> nt.copy());
		assertTrue(clone.identifiers().contains(id));
		assertTrue(nt.hasMapping(id));
		assertFalse(nt.hasMapping(new Identifier("zzz")));
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
