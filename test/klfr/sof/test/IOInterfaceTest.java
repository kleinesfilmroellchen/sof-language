package klfr.sof.test;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.Locale;
import java.util.Scanner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import klfr.sof.IOInterface;
import klfr.sof.lang.Nametable;
import klfr.sof.lang.Primitive;
import klfr.sof.lang.Stack;

class IOInterfaceTest {
	
	private IOInterface io;
	private StringWriter p;

	@BeforeEach
	void setUp() throws Exception {
		p = new StringWriter();
		io = new IOInterface();
	}

	@Test
	void testModifyStreams() {
		assertDoesNotThrow(()-> io.setOut(p));
		var in = new StringReader("abc def ghi\nsecond line");
		assertDoesNotThrow( () -> io.setInOut(in, p));
		var inscan = assertDoesNotThrow( () -> io.newInputScanner(), "Scanner retrieval");
		assertEquals("abc", inscan.next(), "Scanner Usage");
	}

	@Test
	void testPrintMethods() {
		io.setOut(p);
		io.print("abc");
		assertEquals("abc", p.toString(), "Simple print test");
		// clear the buffer
		p.getBuffer().setLength(0);
		io.println("cde"); io.print('f'); assertEquals("cde" + System.lineSeparator() + "f", p.toString(), "Println test"); p.getBuffer().setLength(0);
		io.print(true); assertEquals("true", p.toString(), "Boolean print"); p.getBuffer().setLength(0);
		io.print(22); assertEquals("22", p.toString(), "Int print"); p.getBuffer().setLength(0);
		io.print(24l); assertEquals("24", p.toString(), "Long print"); p.getBuffer().setLength(0);
		var b = new Primitive<Boolean>(false);
		io.print(b); assertEquals(b.toString(), p.toString(), "Object print"); p.getBuffer().setLength(0);
		assertDoesNotThrow( () -> {
			io.printf("%n");
			io.printf(Locale.getDefault(), "abc%d", 2);
		});
		io.debug = true;
		var s = new Stack(null); s.add(new Nametable());
		assertDoesNotThrow( () -> io.describeStack(s)); p.getBuffer().setLength(0);
		assertDoesNotThrow( () -> io.debug("abc"));
		assertEquals("abc" + System.lineSeparator(), p.toString()); p.getBuffer().setLength(0);
		io.debug = false;
		io.debug("abc");
		assertEquals("", p.toString());
	}

}
