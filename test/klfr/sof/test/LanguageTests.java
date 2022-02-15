package klfr.sof.test;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.logging.*;
import java.util.stream.*;

import org.junit.jupiter.api.*;
import org.opentest4j.TestAbortedException;

import klfr.sof.*;
import klfr.sof.cli.CLI;
import klfr.sof.exceptions.*;
import klfr.sof.lib.*;

/**
 * The language test class is responsible for running the tests on the SOF
 * source code test files that check many parts of SOF execution. <br>
 * <br>
 * For these purposes, the test system uses the {@code assert} primitive token
 * that will check the topmost element of the stack for being true. If it is
 * not, the interpreter throws a custom subclass of CompilerException and the
 * test of the current SOF file will fail, otherwise, it will continue to
 * execute. Any (standard) CompilerException occurring at any point during the
 * interpretation cycle is also a failure.
 */
public class LanguageTests extends SofTestSuper {
	/**
	 * Directory or package where the SOF source files for the language tests
	 * reside.
	 */
	public static final String SOURCE_FOLDER = "build/out/testbins/klfr/sof/test/source/";
	public static final Charset TEST_SOURCE_CHARSET = Charset.forName("utf-8");

	public static final Logger log = Logger.getLogger(LanguageTests.class.getCanonicalName());

	@DisplayName("SOF language tests from test files")
	@TestFactory
	Iterator<DynamicTest> generateLanguageTests() throws SOFException {
		var nfRegistry = new NativeFunctionRegistry();
		try {
			nfRegistry.registerAllFromPackage("klfr.sof.lib");
		} catch (IOException e1) {
			fail(e1);
		}

		final var files = Arrays.asList(new File(SOURCE_FOLDER).listFiles());
		log.log(Level.INFO, () -> String.format("Test source directory contents: %s", files));
		final var sofFiles = files.stream().map(cs -> cs.toString()).filter(f -> f.toString().endsWith(".sof"))
				.collect(Collectors.toSet());
		log.log(Level.INFO, () -> String.format("SOF source files for testing: %s", sofFiles));
		final var sofFileIterator = sofFiles.iterator();

		return new Iterator<DynamicTest>() {
			@Override
			public boolean hasNext() {
				return sofFileIterator.hasNext();
			}

			@Override
			public DynamicTest next() {
				final var file = sofFileIterator.next();
				try {
					final var codeReader = new FileReader(new File(file), TEST_SOURCE_CHARSET);
					// TODO: magic number 1KiB?
					final var out = new StringWriter(1024);
					codeReader.transferTo(out);
					final var code = Preprocessor.preprocessCode(out.toString());
					out.close();
					codeReader.close();
					return dynamicTest(String.format("Test source file: %s", file), () -> {
						try {
							log.info(String.format("Source test %s initializing...", file));
							final IOInterface iface = new IOInterface(InputStream.nullInputStream(), System.out);
							final var engine = new Interpreter(iface, nfRegistry);
							final var codeUnit = Parser.parse(new File(file), code);
							final var time = System.nanoTime();
							CLI.runPreamble(engine);
							engine.run(codeUnit);
							final var finish = System.nanoTime();
							log.info(String.format("Source test %-20s completed in %12.3f µs, %3d asserts total", file,
									(finish - time) / 1_000d, engine.getAssertCount()));
						} catch (CompilerException e) {
							fail("Compiler exception while running language test '" + file + "'.", e);
						}
					});
				} catch (IOException e) {
					log.log(Level.SEVERE, String.format("Cannot test source file %s", file), e);
					return dynamicTest(String.format("Test source file: %s FAILING with external exception.", file),
							() -> {
								throw new TestAbortedException("Cannot test source file.");
							});
				}
			}
		};

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
