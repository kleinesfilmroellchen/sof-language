package klfr.sof.cli;

import static klfr.sof.Interpreter.R;

// MOAR STANDARD LIBRARY
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.*;
import java.nio.charset.Charset;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.function.Supplier;
import java.util.logging.*;

import klfr.sof.*;
import klfr.sof.exceptions.CompilerException;
import klfr.sof.lib.*;
import klfr.sof.module.ModuleDiscoverer;

/**
 * The SOF Language standard command line interface. This implements the SOF file interpreter and the REPL. It is the
 * most important main class in the project.
 * 
 * @author klfr
 */
public final class CLI {

	/**
	 * The SOF info string printed as the first output before the REPL starts.
	 */
	public static final String					INFO_STRING					= String.format(R.getString("sof.cli.version"), Interpreter.VERSION,
			// awww yesss, the Java Time API 😋
			DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).format(buildTime().atZone(ZoneId.systemDefault())));

	private static final Logger				log							= Logger.getLogger(CLI.class.getCanonicalName());

	/**
	 * Parsed Code for the SOF preamble, which is responsible for the builtin function setup.
	 */
	private static SOFFile						preambleCode;

	private static NativeFunctionRegistry	nativeFunctionRegistry	= new NativeFunctionRegistry();

	/**
	 * Main entry point of the SOF interpreter system. This method handles command-line arguments as described in the
	 * documentation and then runs the SOF interpreter system.
	 * 
	 * @param args Command-line arguments
	 * @throws InvocationTargetException    Should not be thrown: If reflectively invoked methods fail.
	 * @throws UnsupportedEncodingException Should not be thrown: If the UTF-8 encoding is not supported.
	 * @throws IOException                  If any I/O operation fails unrecoverable.
	 */
	public static void main(String[] args) throws InvocationTargetException, UnsupportedEncodingException, IOException {
		// setup console info logging
		LogManager.getLogManager().reset();
		final var bl = Logger.getLogger("");
		bl.setLevel(Level.FINEST);
		var ch = new ConsoleHandler();
		ch.setLevel(Level.OFF);
		bl.addHandler(ch);
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			bl.info("SOF exiting.");
		}));
		// System.out.println(R.getBaseBundleName());
		// System.out.println(ResourceBundle.getBundle(Interpreter.MESSAGE_RESOURCE).getBaseBundleName());
		//bl.setResourceBundle(R);

		Options opt = null;
		try {
			opt = Options.parseOptions(args);
		} catch (IllegalArgumentException e) {
			System.out.println(e.getLocalizedMessage());
			System.exit(3);
		}

		IOInterface io = new IOInterface();
		io.setInOut(System.in, System.out);

		if ((opt.flags & Options.DEBUG) > 0) {
			try {
				LogManager.getLogManager().reset();
				final var rootLog = Logger.getLogger("");
				rootLog.setLevel(Level.ALL);
				ch = new ConsoleHandler();
				ch.setLevel(Level.FINE);

				ch.setFormatter(new DebugFormatter());
				rootLog.addHandler(ch);
				final var handler = new FileHandler("sof-log.log");
				handler.setFormatter(new SimpleFormatter());
				handler.setEncoding("utf-8");
				handler.setLevel(Level.FINEST);
				rootLog.addHandler(handler);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		log.config(opt.toString());

		// main code starts here
		try {
			nativeFunctionRegistry.registerAllFromPackage("klfr.sof.lib");

			// execute
			runSOF(opt, io);

		} catch (CompilerException error) {
			log.log(Level.SEVERE, "Uncaught Interpreter exception", error);
		}
	}

	/**
	 * Run a standard SOF system with the given options and IO interface.
	 * 
	 * @param clo The options that determine what sort of execution should be done and what other parameters apply to the
	 *               execution.
	 * @param io  The I/O interface. Will be passed to any interpreters and is also used internally by this method.
	 * @throws CompilerException If the interpreter encounters an error.
	 * @throws IOException       If an I/O error occurs, e.g. reading a source code file.
	 */
	public static void runSOF(Options clo, IOInterface io) throws CompilerException, IOException {
		io.debug = (clo.flags & Options.DEBUG) > 0;
		log.config(() -> String.format("FLAG :: DEBUG %5s", io.debug ? "on" : "off"));

		final var moduleDiscoverer = clo.overrideLibraryPath.isPresent() ? new ModuleDiscoverer(new File(clo.overrideLibraryPath.get())) : new ModuleDiscoverer();
		log.config(() -> String.format("Using standard library '%s'", moduleDiscoverer.getStdlibBaseDirectory()));

		switch (clo.executionType) {
		case File: {
			//// File execution
			log.log(Level.FINE, () -> clo.executionStrings.toString());
			List<File> files = new ArrayList<>(clo.executionStrings.size());
			for (String filename : clo.executionStrings) {
				File f = new File(filename);
				files.add(f);
			}
			final var throwable = files.stream().map(file -> {
				try {
					log.log(Level.INFO, () -> String.format("EXECUTE :: %30s", file));
					if ((clo.flags & Options.ONLY_PREPROCESSOR) > 0) {
						CLI.runPreprocessor(new FileReader(file, Charset.forName("utf-8")), io);
						io.println("^D");
					} else
						CLI.doFullExecution(file, new Interpreter(io, moduleDiscoverer, nativeFunctionRegistry), io, clo.flags);
					return null;
				} catch (Throwable t) {
					io.println(t.getMessage());
					return t;
				}
			})
					// The above map will execute on all readers and then return null, if the
					// execution method just exited normally. If, however, some exception was
					// raised, whether controlled (CompilerException, IOException etc.)
					// or not (ArrayIndexOutOfBoundsException, RuntimeException), the Throwable is
					// returned. Thus, in the first step, we "map" all readers to a possibly null
					// Throwable. The filter then removes all nulls with the instanceof check. We
					// now of course have a situation where, if all files executed successfully, the
					// stream can be empty. The "findFirst" will exactly exhibit the behavior we
					// need: return an Optional<Throwable> if any error occurred, or an empty
					// Optional otherwise. I call all of this "Java do notation". Praise Haskell!
					.filter(val -> val instanceof Throwable).findFirst();

			// error handling
			if (throwable.isPresent()) {
				final var error = throwable.get();
				if (error instanceof CompilerException ceError) {
					throw ceError;
				} else if (error instanceof IOException ioError) {
					throw ioError;
				}
				throw new RuntimeException("Unhandled exception in executing SOF.", error);
			}
			break;
		}
		case Literal: {
			//// Single literal to be executed
			CLI.doFullExecution(new StringReader(clo.executionStrings.get(0)), new Interpreter(io, moduleDiscoverer, nativeFunctionRegistry), io, clo.flags);
			break;
		}
		case Interactive: {
			//// Interactive interpretation
			io.println(CLI.INFO_STRING);
			Interpreter engine = new Interpreter(io, moduleDiscoverer, nativeFunctionRegistry);
			CLI.runPreamble(engine);
			Scanner scanner = io.newInputScanner();
			// scanner.useDelimiter("[[^\n]\\s+]");
			io.print(">>> ");
			while (scanner.hasNextLine()) {
				String code = scanner.nextLine();
				// catches all unwanted compilation errors
				try {
					SOFFile codeUnit = null;
					try {
						// may throw
						codeUnit = Parser.parse(new File("<stdin>"), Preprocessor.preprocessCode(code));
					} catch (CompilerException e) {
						// give the user more lines to possibly fix the syntax error
						while (scanner.hasNextLine()) {
							io.print("... ");
							final var nl = scanner.nextLine();
							// end on blank line
							if (nl.isBlank())
								break;
							// update unclean code
							code += "\n" + nl;
						}
						// may throw again, in this case even with additional lines the code is bad
						codeUnit = Parser.parse(new File("<stdin>"), Preprocessor.preprocessCode(code));
					}
					// in any case, execute
					if (codeUnit != null) {
						log.fine(codeUnit.toString());
						engine.run(codeUnit);
					}
				} catch (CompilerException e) {
					// outer catch catches all runtime errors e.g. type and stack errors
					io.println("!!! " + e.getLocalizedMessage());
					log.log(Level.SEVERE, "Compiler Exception occurred.", e);
				}
				io.print(">>> ");
			}
			scanner.close();
			break;
		}
		case VersionInfo: {
			io.println(CLI.INFO_STRING);
			io.printf(R.getString("sof.cli.license"));
			break;
		}
		case HelpInfo: {
			io.printf(R.getString("sof.cli.help"));
			break;
		}
		}
	}

	/**
	 * Does full execution on SOF source code given the environment. This takes a file to read code from.
	 * 
	 * @param codeSource  A file that points to SOF source code.
	 * @param interpreter The interpreter to use for the execution.
	 * @param io          The Input-Output interface that the full execution should use.
	 * @param flags       The flags passed to the program on the command line.
	 * 
	 * @throws CompilerException If the execution of the code fails.
	 * @throws IOException       If the file cannot be read, for example, it does not exist.
	 */
	public static void doFullExecution(File codeSource, Interpreter interpreter, IOInterface io, int flags) throws IOException, CompilerException {
		log.entering(CLI.class.getCanonicalName(), "doFullExecution");
		String code = "";
		final var codeStream = new FileReader(codeSource, Charset.forName("utf-8"));
		final StringWriter writer = new StringWriter();
		codeStream.transferTo(writer);
		codeStream.close();
		code = writer.getBuffer().toString();
		doFullExecution(codeSource, code, interpreter, io, flags);
	}

	/**
	 * Does full execution on SOF source code given the environment. This takes a reader as a code source.
	 * 
	 * @param codeStream  A reader that reads SOF source code.
	 * @param interpreter The interpreter to use for the execution.
	 * @param io          The Input-Output interface that the full execution should use.
	 * @param flags       The flags passed to the program on the command line.
	 * 
	 * @throws CompilerException If the execution of the code fails.
	 * @throws IOException       If the file cannot be read, for example, it does not exist.
	 */
	public static void doFullExecution(Reader codeStream, Interpreter interpreter, IOInterface io, int flags) throws IOException, CompilerException {
		log.entering(CLI.class.getCanonicalName(), "doFullExecution");
		// Because this file is never read, it is safe to create it with a placeholder name that indicates a literal string from a reader.
		final var dummyFile = new File("<literal>");
		String code = "";
		final StringWriter writer = new StringWriter();
		codeStream.transferTo(writer);
		code = writer.getBuffer().toString();
		doFullExecution(dummyFile, code, interpreter, io, flags);
	}

	/**
	 * Handler for the common part of all full execution routines; retrieves finished SOF source code and a "dummy" file
	 * that is never read.
	 */
	private static void doFullExecution(File fdummy, String code, Interpreter interpreter, IOInterface io, int flags) throws IOException, CompilerException {
		// if no preprocessing flag NOT set
		if ((flags & Options.NO_PREPROCESSOR) == 0)
			code = Preprocessor.preprocessCode(code);

		// parse
		final var codeUnit = Parser.parse(fdummy, code);
		if (io.debug)
			io.println(codeUnit.ast());

		// count nodes
		final var nodeCount = (io.debug || (flags & Options.PERFORMANCE) > 0) ? codeUnit.ast().nodeCount() : 0;

		// run preamble
		runPreamble(interpreter);

		// run code
		final var startTime = System.nanoTime();
		interpreter.run(codeUnit);
		final var finishTime = System.nanoTime();
		final var execTimeµs = (finishTime - startTime) / 1_000d;

		// logging, performance
		log.info(String.format("Ran %d asserts.", interpreter.getAssertCount()));
		final Supplier<String> perfInfo = () -> String.format("PERFORMANCE: Ran %9.3f ms (%4d nodes in %12.3f µs, avg %7.2f µs/node)", execTimeµs / 1_000d, nodeCount, execTimeµs, execTimeµs / nodeCount);
		if ((flags & Options.PERFORMANCE) > 0)
			io.println(perfInfo.get());
		log.exiting(CLI.class.getCanonicalName(), "doFullExecution");
	}

	/**
	 * Helper function that runs the preamble code on the interpreter, and may parse the preamble code if necessary.
	 * 
	 * @param interpreter The interpreter on which the preamble should be run.
	 * @throws CompilerException If the preamble encounters an error when executing.
	 */
	public static void runPreamble(Interpreter interpreter) throws CompilerException {
		// parse preamble if not yet done
		if (preambleCode == null) {
			try {
				// get code
				final var pStream = new FileInputStream(new File(interpreter.getModuleDiscoverer().getStdlibBaseDirectory(), "preamble.sof"));
				final var pReader = new InputStreamReader(pStream, Charset.forName("utf-8"));
				final var pWriter = new StringWriter();
				pReader.transferTo(pWriter);
				var preambleCodeStr = pWriter.toString();
				pWriter.close();
				pStream.close();

				// parse code
				preambleCodeStr = Preprocessor.preprocessCode(preambleCodeStr);
				preambleCode = Parser.parse(new File("<preamble>"), preambleCodeStr);
			} catch (IOException | NullPointerException e) {
				interpreter.getIO().println(R.getString("sof.cli.nopreamble"));
				System.exit(1);
			}
		}
		interpreter.run(preambleCode);
	}

	/**
	 * Runs the SOF preprocessor on the reader and prints the result to the given IO interface output
	 * 
	 * @param io     The I/O interface to be used for printing out the preprocessor result.
	 * @param reader A reader that reads SOF source code.
	 * @throws IOException If an error occurs when reading the input.
	 */
	public static void runPreprocessor(Reader reader, IOInterface io) throws IOException {
		String code = "";
		try {
			StringWriter writer = new StringWriter();
			reader.transferTo(writer);
			code = writer.getBuffer().toString();
		} catch (IOException e) {
			throw new IOException("Unknown exception occurred during input reading.", e);
		}
		io.print(Preprocessor.preprocessCode(code));
	}

	/**
	 * Return the build time of SOF, i.e. the last time that the CLI class file was modified.
	 * 
	 * @return the build time of SOF, i.e. the last time that the CLI class file was modified.
	 */
	public static Instant buildTime() {
		try {
			URI classuri = CLI.class.getClassLoader().getResource(CLI.class.getCanonicalName().replace(".", "/") + ".class").toURI();
			// log.finest(classuri.getScheme() + " " + classuri.getPath());
			if (classuri.getScheme().equals("rsrc") || classuri.getScheme().equals("jar")) {
				// we are in a jar file
				// returns the containing folder of the jar file
				// String jarpath = new
				// File(ClassLoader.getSystemResource(".").getFile()).getCanonicalPath();
				String jarfilepath = new File(".").getCanonicalPath() + File.separator + System.getProperty("java.class.path");
				// log.finest(jarfilepath);
				return Instant.ofEpochMilli(new File(jarfilepath).lastModified());
			} else if (classuri.getScheme().equals("file")) {
				return Instant.ofEpochMilli(new File(classuri.getPath()).lastModified());
			}
		} catch (URISyntaxException e) {
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
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
