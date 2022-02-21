package klfr.sof.test;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.StringReader;
import java.io.StringWriter;

import klfr.sof.IOInterface;
import klfr.sof.cli.*;
import klfr.sof.exceptions.*;

@DisplayName("Test SOF's command line interface")
class CLITests extends SofTestSuper {

	@DisplayName("Test CLI options flags")
	@Test
	void testOptionFlags() {
		assertTrue((Options.parseOptions(new String[] { "-d" }).flags & Options.DEBUG) != 0);
		assertTrue((Options.parseOptions(new String[] { "-p" }).flags & Options.ONLY_PREPROCESSOR) != 0);
		assertTrue((Options.parseOptions(new String[] { "-P" }).flags & Options.NO_PREPROCESSOR) != 0);
		assertTrue((Options.parseOptions(new String[] { "--performance" }).flags & Options.PERFORMANCE) != 0);

		assertTrue((Options.parseOptions(new String[] { "-pd" }).flags & (Options.ONLY_PREPROCESSOR | Options.DEBUG)) != 0);
		assertThrows(IllegalArgumentException.class, () -> Options.parseOptions(new String[] { "-k" }));
		assertThrows(IllegalArgumentException.class, () -> Options.parseOptions(new String[] { "-c", "command", "-k" }));
	}

	@DisplayName("Test CLI options execution type")
	@Test
	void testOptionExecutionType() {
		assertEquals(Options.ExecutionType.Interactive, Options.parseOptions(new String[] {}).executionType);
		assertEquals(Options.ExecutionType.Literal, Options.parseOptions(new String[] { "-c", "no code" }).executionType);
		assertEquals(Options.ExecutionType.File, Options.parseOptions(new String[] { "./a/path.sof" }).executionType);
		assertEquals(Options.ExecutionType.HelpInfo, Options.parseOptions(new String[] { "--help" }).executionType);
		assertEquals(Options.ExecutionType.VersionInfo, Options.parseOptions(new String[] { "-v" }).executionType);
	}

	@DisplayName("Test CLI options execution strings")
	@Test
	void testExecutionStrings() {
		assertEquals("no code", Options.parseOptions(new String[] { "-c", "no code" }).executionStrings.get(0));
		assertEquals("./a/path.sof", Options.parseOptions(new String[] { "./a/path.sof" }).executionStrings.get(0));
		assertTrue(Options.parseOptions(new String[] { "--help" }).executionStrings.isEmpty());
		assertThrows(IllegalArgumentException.class, () -> Options.parseOptions(new String[] { "-c" }));
	}

	@DisplayName("Test CLI library option")
	@Test
	void testLibraryOption() {
		assertTrue(Options.parseOptions(new String[] { "--library", "library" }).overrideLibraryPath.isPresent());
		assertFalse(Options.parseOptions(new String[] { "--help" }).overrideLibraryPath.isPresent());
		assertEquals("library", Options.parseOptions(new String[] { "--library", "library" }).overrideLibraryPath.get());
		assertThrows(IllegalArgumentException.class, () -> Options.parseOptions(new String[] { "--library" }));
	}

	@DisplayName("Test CLI runSOF")
	@Test
	void testRunSOF() {
		final var io = new IOInterface(System.in, System.out);
		assertDoesNotThrow(() -> CLI.runSOF(Options.parseOptions(new String[] { "-c", "1 2 + 3 = assert" }), io));
		assertDoesNotThrow(() -> CLI.runSOF(Options.parseOptions(new String[] { "-d", "-c", "1 2 + 3 = assert" }), io));
		assertThrows(CompilerException.class, () -> CLI.runSOF(Options.parseOptions(new String[] { "-c", "1 2 + 3 /= assert" }), io));
		assertThrows(CompilerException.class, () -> CLI.runSOF(Options.parseOptions(new String[] { "-c", "1 2this is not valid code" }), io));
		assertThrows(CompilerException.class, () -> CLI.runSOF(Options.parseOptions(new String[] { "-c", "1 { missing brace" }), io));

		assertDoesNotThrow(() -> CLI.runSOF(Options.parseOptions(new String[] { "-P", "-c", "valid non preprocessed code" }), io));
		assertThrows(CompilerException.class, () -> CLI.runSOF(Options.parseOptions(new String[] { "-P", "-c", "# invalid non preprocessed code" }), io));

		assertDoesNotThrow(() -> CLI.runSOF(Options.parseOptions(new String[] { LanguageTests.SOURCE_FOLDER + "boolean.sof" }), io));
		assertDoesNotThrow(() -> CLI.runSOF(Options.parseOptions(new String[] { "-p", LanguageTests.SOURCE_FOLDER + "boolean.sof" }), io));

		final var interactiveIO = new IOInterface(new StringReader("true assert\n1 blah blah\n { incomplete line\nfinished here }\n\n"), new StringWriter());
		assertDoesNotThrow(() -> CLI.runSOF(Options.parseOptions(new String[] {}), interactiveIO));
		final var invalidInteractiveIO = new IOInterface(new StringReader("2bad code \""), new StringWriter());
		assertDoesNotThrow(() -> CLI.runSOF(Options.parseOptions(new String[] {}), invalidInteractiveIO));

		assertDoesNotThrow(() -> CLI.runSOF(Options.parseOptions(new String[] { "-h" }), io));
		assertDoesNotThrow(() -> CLI.runSOF(Options.parseOptions(new String[] { "-v" }), io));
	}

	@DisplayName("Test program main")
	@Test
	void testMain() {
		final var io = new IOInterface(System.in, System.out);
		assertDoesNotThrow(() -> CLI.main(new String[] { "-c", "1 2 + 3 = assert" }));
		assertDoesNotThrow(() -> CLI.main(new String[] { "-d", "-c", "1 2 + 3 = assert" }));
		assertDoesNotThrow(() -> CLI.main(new String[] { "-c", "1 2 + 3 /= assert" }));
		assertDoesNotThrow(() -> CLI.main(new String[] { "-c", "1 2this is not valid code" }));
		assertDoesNotThrow(() -> CLI.main(new String[] { "-c", "1 { missing brace" }));

		assertDoesNotThrow(() -> CLI.main(new String[] { "-P", "-c", "valid non preprocessed code" }));
		assertDoesNotThrow(() -> CLI.main(new String[] { "-P", "-c", "# invalid non preprocessed code" }));

		assertDoesNotThrow(() -> CLI.main(new String[] { LanguageTests.SOURCE_FOLDER + "boolean.sof" }));
		assertDoesNotThrow(() -> CLI.main(new String[] { "-p", LanguageTests.SOURCE_FOLDER + "boolean.sof" }));

		assertDoesNotThrow(() -> CLI.main(new String[] { "-h" }));
		assertDoesNotThrow(() -> CLI.main(new String[] { "-v" }));
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
