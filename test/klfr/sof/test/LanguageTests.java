package klfr.sof.test;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.opentest4j.TestAbortedException;

import klfr.sof.CompilerException;
import klfr.sof.IOInterface;
import klfr.sof.Interpreter;
import klfr.sof.Parser;
import klfr.sof.Preprocessor;
import klfr.sof.cli.CLI;

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
	public static final String ASSERT_PT = "assert";
	/**
	 * Directory or package where the SOF source files for the language tests
	 * reside.
	 */
	public static final String SOURCE_FOLDER = "klfr/sof/test/source/";
	public static final Charset TEST_SOURCE_CHARSET = Charset.forName("utf-8");

	public static final Logger log = Logger.getLogger(LanguageTests.class.getCanonicalName());

	@DisplayName("SOF language tests from test files")
	@TestFactory
	DynamicNode generateLanguageTests() {
		final var resLoader = LanguageTests.class.getModule().getClassLoader();

		final var files = listDirectory(SOURCE_FOLDER);
		log.log(Level.INFO, () -> String.format("Test source directory contents: %s", files));
		final var sofFiles = files.stream().map(cs -> cs.toString()).filter(f -> f.toString().endsWith(".sof"))
				.collect(Collectors.toSet());
		log.log(Level.INFO, () -> String.format("SOF source files for testing: %s", sofFiles));
		final var sofFileIterator = sofFiles.iterator();

		return dynamicContainer("SOF language tests from test files", new Iterable<DynamicTest>() {
			public Iterator<DynamicTest> iterator() {
				return new Iterator<DynamicTest>() {
					@Override
					public boolean hasNext() {
						return sofFileIterator.hasNext();
					}

					@Override
					public DynamicTest next() {
						final var file = sofFileIterator.next();
						try {
							final var codeStr = resLoader.getResourceAsStream(SOURCE_FOLDER + file);
							final var out = new ByteArrayOutputStream(codeStr.available());
							codeStr.transferTo(out);
							final var code = Preprocessor.preprocessCode(new String(out.toByteArray(), TEST_SOURCE_CHARSET));
							return dynamicTest(String.format("Test source file: %s", file), () -> {
								try {
									log.info(String.format("Source test %s initializing...", file));
									final IOInterface iface = new IOInterface(InputStream.nullInputStream(), System.out);
									final var engine = new Interpreter(iface);
									final var ast = Parser.parse(code);
									final var time = System.nanoTime();
									CLI.runPreamble(engine);
									engine.run(ast, code);
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
		});
	}

	/**
	 * Lists all the files in the directory by using this class's resource loader.
	 * The method is independent of how the resources are stored, as long as the URL
	 * connection to the resource that is a directory returns the files in the
	 * directory separated by a newline.
	 * 
	 * @param directoryName The directory to be read
	 * @return A collection of file name strings that represent the files found in
	 *         the directory
	 */
	private static Set<CharSequence> listDirectory(String directoryName) {
		try {
			final var resLoader = LanguageTests.class.getModule().getClassLoader();
			final var rsrc = resLoader.getResource(directoryName);
			// log.fine(String.format("folder: %s", rsrc));
			final var out = new ByteArrayOutputStream(256);
			rsrc.openStream().transferTo(out);
			final var directory = new String(out.toByteArray());
			return new HashSet<>(Arrays.asList(Pattern.compile("\n", Pattern.MULTILINE).split(directory)));
		} catch (IOException e) {
			log.log(Level.SEVERE, "Could not recieve directory contents.", e);
			return Collections.emptySet();
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
