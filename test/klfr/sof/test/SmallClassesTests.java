package klfr.sof.test;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import klfr.sof.SOFFile;
import klfr.sof.cli.Options;
import klfr.sof.exceptions.*;
import klfr.sof.lang.*;
import klfr.sof.lang.Stackable.DebugStringExtensiveness;
import klfr.sof.lang.functional.FunctionDelimiter;
import klfr.sof.lang.oop.MethodDelimiter;
import klfr.sof.lang.primitive.*;

@DisplayName("Test miscellaneous small classes")
class SmallClassesTests extends SofTestSuper {

	@DisplayName("Stub classes")
	@Test
	void testStubClasses() {
		assertDoesNotThrow(() -> new FunctionDelimiter());
		assertDoesNotThrow(() -> new MethodDelimiter());
	}

	@DisplayName("toString()s")
	@Test
	void testToString() {
		assertDoesNotThrow(() -> new Options().toString());
	}

	@DisplayName("List primitive")
	@Test
	void testList() throws IncompleteCompilerException {
		final var list = new ListPrimitive(List.of(new Identifier("blah")));
		assertDoesNotThrow(() -> list.set(0, StringPrimitive.createStringPrimitive("blah")));
		assertDoesNotThrow(() -> list.add(0, new Identifier("value")));
		assertDoesNotThrow(() -> list.add(new Identifier("value")));
		assertDoesNotThrow(() -> list.addAll(0, List.of()));
		assertDoesNotThrow(() -> list.addAll(List.of()));
		assertDoesNotThrow(() -> list.subList(0, 2));
		assertDoesNotThrow(() -> list.forEach(value -> {
		}));
		assertTrue(list.contains(new Identifier("value")));
		assertTrue(list.containsAll(List.of()));
		assertDoesNotThrow(() -> list.remove(new Identifier("value")));
		assertDoesNotThrow(() -> list.retainAll(List.of()));
		assertDoesNotThrow(() -> list.removeAll(List.of()));
		assertDoesNotThrow(() -> list.replaceAll(elt -> elt));
		assertDoesNotThrow(() -> list.indexOf(IntPrimitive.createIntPrimitive(0l)));
		assertDoesNotThrow(() -> list.lastIndexOf(IntPrimitive.createIntPrimitive(0l)));
		assertDoesNotThrow(() -> list.toArray(new Object[] {}));
		assertDoesNotThrow(() -> list.toArray());
		assertDoesNotThrow(() -> list.listIterator());
		assertDoesNotThrow(() -> list.iterator());
		assertDoesNotThrow(() -> list.stream());
		assertDoesNotThrow(() -> list.isEmpty());
		assertDoesNotThrow(() -> list.listIterator(0));
	}

	@DisplayName("Exception formatting")
	@Test
	void testExceptionFormatting() {
		final var fakeFile = new SOFFile(new File("."), "source code source code this is a bunch of source code lol and it's so freaking long you can't actually believe it, like what the actual fricking lolz is even going on here", null);
		assertDoesNotThrow(() -> CompilerException.from(fakeFile, 0, "generic", "generic").getMessage());
		assertDoesNotThrow(() -> CompilerException.from(fakeFile, 171, "generic", "generic").getMessage());
		assertDoesNotThrow(() -> CompilerException.from(fakeFile, 40, "generic", "generic").getLocalizedMessage());
	}

	@DisplayName("Nametable")
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
