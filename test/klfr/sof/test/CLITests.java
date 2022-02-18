package klfr.sof.test;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import klfr.sof.cli.*;

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
	}

	@DisplayName("Test CLI library option")
	@Test
	void testLibraryOption() {
		assertTrue(Options.parseOptions(new String[] { "--library", "library" }).overrideLibraryPath.isPresent());
		assertFalse(Options.parseOptions(new String[] { "--help" }).overrideLibraryPath.isPresent());
		assertEquals("library", Options.parseOptions(new String[] { "--library", "library" }).overrideLibraryPath.get());
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
