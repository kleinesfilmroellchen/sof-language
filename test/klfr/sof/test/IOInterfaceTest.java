package klfr.sof.test;

import java.io.*;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;

import klfr.sof.IOInterface;
import klfr.sof.lang.*;
import klfr.sof.lang.primitive.*;

class IOInterfaceTest extends SofTestSuper {

	private IOInterface io;
	private StringWriter p;

	@BeforeEach
	void setUp() throws Exception {
		p = new StringWriter();
		io = new IOInterface();
	}

	@Test
	void testModifyStreams() {
		assertDoesNotThrow(() -> io.setOut(p), "Simple output writer");
		assertDoesNotThrow(() -> io.getOutput(), "Writer can be returned");
		var in = new StringReader("abc def ghi\nsecond line");
		assertDoesNotThrow(() -> io.setInOut(in, p), "Set both in and out");
		assertDoesNotThrow(() -> io.setInOut(null, Writer.nullWriter()), "Null I/O streams are accepted");
		assertEquals(in, io.getInput(), "Input was not changed when trying to set it to null");
		var inscan = assertDoesNotThrow(() -> io.newInputScanner(), "Scanner retrieval");
		assertEquals("abc", inscan.next(), "Scanner Usage");
		assertDoesNotThrow(() -> io.setOut(OutputStream.nullOutputStream()), "Set an output stream");
		assertDoesNotThrow(() -> io.setInOut(InputStream.nullInputStream(), OutputStream.nullOutputStream()));
	}

	@Test
	void testPrintMethods() {
		io.setOut(p);
		io.print("abc");
		assertEquals("abc", p.toString(), "Simple print test");
		// clear the buffer
		p.getBuffer().setLength(0);
		io.println("cde");
		io.print('f');
		assertEquals("cde" + System.lineSeparator() + "f", p.toString(), "Println test");
		p.getBuffer().setLength(0);
		// test all da prints
		io.print(true);
		assertEquals("true", p.toString(), "Boolean print");
		p.getBuffer().setLength(0);
		io.print(22);
		assertEquals("22", p.toString(), "Int print");
		p.getBuffer().setLength(0);
		io.print(24l);
		assertEquals("24", p.toString(), "Long print");
		p.getBuffer().setLength(0);
		io.print(22.01f);
		assertEquals("22.01", p.toString(), "Float print");
		p.getBuffer().setLength(0);
		io.print(24.11d);
		assertEquals("24.11", p.toString(), "Double print");
		p.getBuffer().setLength(0);
		io.print(new char[] { 'a', 'b', 'c' });
		assertEquals("abc", p.toString(), "Char array print");
		p.getBuffer().setLength(0);
		var b = BoolPrimitive.createBoolPrimitive(false);
		io.print(b);
		assertEquals(b.toString(), p.toString(), "Object print");
		p.getBuffer().setLength(0);
		assertDoesNotThrow(() -> {
			io.printf("%n");
			io.printf(Locale.getDefault(), "abc%d", 2);
		});
		p.getBuffer().setLength(0);
		// test all da printlns
		io.println(true);
		assertEquals("true" + System.lineSeparator(), p.toString(), "Boolean println");
		p.getBuffer().setLength(0);
		io.println(22);
		assertEquals("22" + System.lineSeparator(), p.toString(), "Int println");
		p.getBuffer().setLength(0);
		io.println(24l);
		assertEquals("24" + System.lineSeparator(), p.toString(), "Long println");
		p.getBuffer().setLength(0);
		io.println(b);
		assertEquals(b.toString() + System.lineSeparator(), p.toString(), "Object println");
		p.getBuffer().setLength(0);
		io.println(22.01f);
		assertEquals("22.01" + System.lineSeparator(), p.toString(), "Float println");
		p.getBuffer().setLength(0);
		io.println(24.11d);
		assertEquals("24.11" + System.lineSeparator(), p.toString(), "Double println");
		p.getBuffer().setLength(0);
		io.println('a');
		assertEquals("a" + System.lineSeparator(), p.toString(), "Char println");
		p.getBuffer().setLength(0);
		io.println(new char[] { 'a', 'b', 'c' });
		assertEquals("abc" + System.lineSeparator(), p.toString(), "Char array println");
		p.getBuffer().setLength(0);
		assertDoesNotThrow(() -> {
			io.printfln("%n");
			io.printfln(Locale.getDefault(), "abc%d", 2);
		});
		p.getBuffer().setLength(0);
		// test debugging
		io.debug = true;
		var s = new Stack();
		s.add(new Nametable());
		assertDoesNotThrow(() -> io.describeStack(s));
		p.getBuffer().setLength(0);
		assertDoesNotThrow(() -> io.debug("abc"));
		assertEquals("abc" + System.lineSeparator(), p.toString());
		p.getBuffer().setLength(0);
		io.debug = false;
		io.debug("abc");
		assertEquals("", p.toString());
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
