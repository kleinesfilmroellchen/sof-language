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
import klfr.sof.ast.*;
import klfr.sof.exceptions.CompilerException;
import klfr.sof.exceptions.IncompleteCompilerException;
import klfr.sof.exceptions.SOFException;
import klfr.sof.lib.*;

public class CLI {

	public static final String INFO_STRING = String.format(R.getString("sof.cli.version"), Interpreter.VERSION,
			// awww yesss, the Java Time API 😋
			DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).format(buildTime().atZone(ZoneId.systemDefault())));

	private static final Logger log = Logger.getLogger(CLI.class.getCanonicalName());

	/**
	 * Parsed Code for the SOF preamble, which is responsible for the builtin
	 * function setup.
	 */
	private static SOFFile preambleCode;

	/**
	 * Main entry point of the SOF interpreter system. This method handles
	 * command-line arguments as described in the documentation and then runs the
	 * SOF interpreter system.
	 * 
	 * @param args Command-line arguments
	 * @throws InvocationTargetException    Should not be thrown: If reflectively
	 *                                      invoked methods fail.
	 * @throws UnsupportedEncodingException Should not be thrown: If the UTF-8
	 *                                      encoding is not supported.
	 * @throws IOException                  If any I/O operation fails
	 *                                      unrecoverable.
	 * @throws SOFException
	 */
	public static void main(String[] args)
			throws InvocationTargetException, UnsupportedEncodingException, IOException, SOFException {
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

		//TODO: move this into some centralized system
		NativeFunctionRegistry.registerNativeFunctions(Builtins.class);
		NativeFunctionRegistry.registerNativeFunctions(Formatting.class);

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

				ch.setFormatter(new java.util.logging.Formatter() {
					@Override
					public String format(LogRecord record) {
						final var msg = record.getMessage();
						try {
							record.getResourceBundle().getString(record.getMessage());
						} catch (MissingResourceException | NullPointerException e) {
							// do nothing
						}
						final var time = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM)
								.format(record.getInstant().atZone(ZoneId.systemDefault()));
						final var level = record.getLevel().getLocalizedName().substring(0,
								Math.min(record.getLevel().getLocalizedName().length(), 6));
						final var logName = record.getLoggerName().replace("klfr.sof", "~");

						return String.format("[%s %-20s |%6s] %s%n", time, logName, level, msg) + (record.getThrown() == null ? ""
									: formatException(record.getThrown()));
					}
					/** Helper to format an exception and its causality chain. */
					private String formatException(final Throwable exc) {
						final var sb = new StringBuilder(512);
						sb.append(String.format("EXCEPTION: %s | Stack trace:%n", exc.toString()));

						var currentExc = exc;
						int level = 0;
						while (currentExc != null) {
							sb.append(Arrays.asList(currentExc.getStackTrace()).stream().map(x -> x.toString()).collect(
									() -> new StringBuilder(),
									(builder, str) -> builder.append(" in ").append(str).append(System.lineSeparator()),
									(b1, b2) -> b1.append(b2))
								.toString().indent(level*2));
							
							currentExc = currentExc.getCause(); ++level;
							if (currentExc != null) sb.append("Caused by: ").append(currentExc.toString()).append("\n");
						}
						return sb.toString();
					}
				});
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

		// execute
		final var error = runSOF(opt, io);

		if (error.isPresent()) {
			final var t = error.get();
			log.log(Level.SEVERE,
					String.format("Uncaught Interpreter exception: %s%nStack trace:%n%s", t.getLocalizedMessage(),
							Arrays.asList(t.getStackTrace()).stream().map(ste -> ste.toString())
									.reduce((a, b) -> a + System.lineSeparator() + b).orElse("")));
		}
	}

	/**
	 * Run a standard SOF system with the given options and IO interface.
	 * 
	 * @param clo The options that determine what sort of execution should be done
	 *            and what other parameters apply to the execution.
	 * @param io  The I/O interface. Will be passed to any interpreters and is also
	 *            used internally by this method.
	 * @return An exception, if something went wrong, or nothing, if everything went
	 *         fine.
	 * @throws CompilerException for such compiler exceptions that should only happen if the user is not responsible for them.
	 */
	public static Optional<Throwable> runSOF(Options clo, IOInterface io) throws CompilerException {
		io.debug = (clo.flags & Options.DEBUG) > 0;
		log.config(() -> String.format("FLAG :: DEBUG %5s", io.debug ? "on" : "off"));
		switch (clo.executionType) {
			case File: {
				//// File execution
				log.log(Level.FINE, () -> clo.executionStrings.toString());
				List<File> files = new ArrayList<>(clo.executionStrings.size());
				for (String filename : clo.executionStrings) {
					File f = new File(filename);
					files.add(f);
				}
				return files.stream().map(file -> {
					try {
						log.log(Level.INFO, () -> String.format("EXECUTE :: %30s", file));
						if ((clo.flags & Options.ONLY_PREPROCESSOR) > 0) {
							CLI.runPreprocessor(new FileReader(file, Charset.forName("utf-8")), io);
							io.println("^D");
						} else
							CLI.doFullExecution(file, new Interpreter(io), io, clo.flags);
						return null;
					} catch (Throwable t) {
						io.println(t.getMessage());
						return t;
					}
				})
						// The above map will execute on all readers and then return null, if the
						// execution method just exited normally. If, however, some exception was
						// raised, whether controlled (CompilerException, IllegalArgumentException etc.)
						// or not (ArrayIndexOutOfBoundsException, RuntimeException), the Throwable is
						// returned. Thus, in the first step, we "map" all readers to a possibly null
						// Throwable. The filter then removes all nulls with the instanceof check. We
						// now of course have a situation where, if all files executed successfully, the
						// stream can be empty. The "findFirst" will exactly exhibit the behavior we
						// need: return an Optional<Throwable> if any error occurred, or an empty
						// Optional otherwise. I call all of this "Java do notation". Praise Haskell!
						.filter(val -> val instanceof Throwable).findFirst();
			}
			case Literal: {
				//// Single literal to be executed
				try {
					CLI.doFullExecution(new StringReader(clo.executionStrings.get(0)), new Interpreter(io), io, clo.flags);
				} catch (Throwable t) {
					io.println(t.getLocalizedMessage());
					return Optional.of(t);
				}
				break;
			}
			case Interactive: {
				//// Interactive interpretation
				io.println(CLI.INFO_STRING);
				Interpreter engine = new Interpreter(io);
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
							while (true) {
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
		return Optional.empty();
	}

	/**
	 * Does full execution on SOF source code given the environment.
	 * 
	 * @param codeSource  A file that points to SOF source code.
	 * @param interpreter The interpreter to use for the execution.
	 * @param io          The Input-Output interface that the full execution should
	 *                    use.
	 * @param flags       The flags passed to the program on the command line.
	 */
	public static void doFullExecution(File codeSource, Interpreter interpreter, IOInterface io, int flags)
			throws Exception {
		log.entering(CLI.class.getCanonicalName(), "doFullExecution");
		String code = "";
		try {
			final var codeStream = new FileReader(codeSource, Charset.forName("utf-8"));
			final StringWriter writer = new StringWriter();
			codeStream.transferTo(writer);
			codeStream.close();
			code = writer.getBuffer().toString();
		} catch (IOException e) {
			throw new Exception("Unknown exception occurred during input reading.", e);
		}
		doFullExecution(codeSource, code, interpreter, io, flags);
	}

	public static void doFullExecution(Reader codeStream, Interpreter interpreter, IOInterface io, int flags) throws Exception {
		log.entering(CLI.class.getCanonicalName(), "doFullExecution");
		// Because this file is never read, it is safe to create it with a placeholder name that indicates a literal string from a reader.
		final var dummyFile = new File("<literal>");
		String code = "";
		try {
			final StringWriter writer = new StringWriter();
			codeStream.transferTo(writer);
			code = writer.getBuffer().toString();
		} catch (IOException e) {
			throw new Exception("Unknown exception occurred during input reading.", e);
		}
		doFullExecution(dummyFile, code, interpreter, io, flags);
	}

	/**
	 * Handler for the common part of all full execution routines; retrieves finished SOF source code and a "dummy" file that is never read.
	 */
	private static void doFullExecution(File fdummy, String code, Interpreter interpreter, IOInterface io, int flags) throws Exception {
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
		final Supplier<String> perfInfo = () -> String.format(
				"PERFORMANCE: Ran %9.3f ms (%4d nodes in %12.3f µs, avg %7.2f µs/node)", execTimeµs / 1_000d, nodeCount,
				execTimeµs, execTimeµs / nodeCount);
		if ((flags & Options.PERFORMANCE) > 0)
			io.println(perfInfo.get());
		log.exiting(CLI.class.getCanonicalName(), "doFullExecution");
	}

	/**
	 * Helper function that runs the preamble code on the interpreter, and may parse
	 * the preamble code if necessary.
	 * 
	 * @param interpreter The interpreter on which the preamble should be run.
	 */
	public static void runPreamble(Interpreter interpreter) throws CompilerException {
		// parse preamble if not yet done
		if (preambleCode == null) {
			try {
				// get code
				final var pStream = CLI.class.getModule().getClassLoader().getResource("klfr/sof/lib/preamble.sof").openStream();
				final var pReader = new InputStreamReader(pStream, Charset.forName("utf-8"));
				final var pWriter = new StringWriter();
				pReader.transferTo(pWriter);
				var preambleCodeStr = pWriter.toString();
				pWriter.close(); pStream.close();

				// parse code
				preambleCodeStr = Preprocessor.preprocessCode(preambleCodeStr);
				preambleCode = Parser.parse(new File("<preamble>"), preambleCodeStr);
			} catch (IOException | NullPointerException e) {
				interpreter.getIO().println(R.getString("sof.cli.nopreamble"));
				throw new RuntimeException(e);
			}
		}
		interpreter.run(preambleCode);
	}

	/**
	 * Runs the SOF preprocessor on the reader and prints the result to the given IO
	 * interface output
	 * 
	 * @param reader A reader that reads SOF source code.
	 */
	public static void runPreprocessor(Reader reader, IOInterface io) throws Exception {
		String code = "";
		try {
			StringWriter writer = new StringWriter();
			reader.transferTo(writer);
			code = writer.getBuffer().toString();
		} catch (IOException e) {
			throw new Exception("Unknown exception occurred during input reading.", e);
		}
		io.print(Preprocessor.preprocessCode(code));
	}

	public static void exitUnnormal(int status) {
		System.out.println("So long, and thank's for all the fish...");
		System.exit(status);
	}

	public static Instant buildTime() {
		try {
			URI classuri = CLI.class.getClassLoader()
					.getResource(CLI.class.getCanonicalName().replace(".", "/") + ".class").toURI();
			// log.finest(classuri.getScheme() + " " + classuri.getPath());
			if (classuri.getScheme().equals("rsrc") || classuri.getScheme().equals("jar")) {
				// we are in a jar file
				// returns the containing folder of the jar file
				// String jarpath = new
				// File(ClassLoader.getSystemResource(".").getFile()).getCanonicalPath();
				String jarfilepath = new File(".").getCanonicalPath() + File.separator
						+ System.getProperty("java.class.path");
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
