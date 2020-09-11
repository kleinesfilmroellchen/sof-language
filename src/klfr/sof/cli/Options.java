package klfr.sof.cli;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.function.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import klfr.sof.CompilerException;
import klfr.sof.IOInterface;
import klfr.sof.Interpreter;
import klfr.sof.Parser;
import klfr.sof.Preprocessor;
import klfr.sof.ast.Node;

import static klfr.sof.Interpreter.R;

@SuppressWarnings("deprecation")
class Options implements Function<IOInterface, Optional<Throwable>> {

	private static Logger log = Logger.getLogger(Options.class.getCanonicalName());

	/**
	 * Enum for general operation execution string (ES) treatment. An ES is any CL
	 * argument without a '-'.
	 */
	static enum ExecutionType {
		/** ES is treated as list of relative filenames. */
		File,
		/** ES index 0 is treated as code. */
		Literal,
		/** ES is ignored, wait for user input instead. */
		Interactive,
		/** ES is ignored, display version info and exit. */
		VersionInfo,
		/** ES is ignored, display help info and exit. */
		HelpInfo
	}

	/** Debug flag constant. */
	public static final int DEBUG = 0b1;
	/** Preprocessor flag constant. */
	public static final int ONLY_PREPROCESSOR = 0b10;
	/** No preprocessor flag constant. */
	public static final int NO_PREPROCESSOR = 0b100;
	/** Print performance info flag constant. */
	public static final int PERFORMANCE = 0b1000;
	public Options.ExecutionType executionType;
	public List<String> executionStrings;
	/**
	 * All flags binary OR-ed together (binary AND with a certain flag to check it).
	 */
	public int flags;

	public String toString() {
		return "Options:" + executionType + executionStrings.toString() + "f:" + Integer.toBinaryString(flags);
	}

	/**
	 * Run the interpreter with the given Options
	 */
	@Override
	public Optional<Throwable> apply(IOInterface io) {
		io.debug = (this.flags & DEBUG) > 0;
		log.config(() -> String.format("FLAG :: DEBUG %5s", io.debug ? "on" : "off"));
		switch (executionType) {
			case File: {
				//// File execution
				log.log(Level.FINE, () -> this.executionStrings.toString());
				List<Reader> readers = new ArrayList<>(this.executionStrings.size());
				for (String filename : this.executionStrings) {
					try {
						Reader strd = new FileReader(filename);
						readers.add(strd);
					} catch (FileNotFoundException e) {
						io.printf(R.getString("sof.cli.filenotfound"), filename);
						CLI.exitUnnormal(2);
					}
				}
				// This is what I envision 'typa' to look like, minus the try-catch. Exceptions
				// as subclass of Nothing, as an instance of the Maybe Monad
				// Note that the map has side-effects and this is the most unpure stream
				// pipeline ever written *haskell cringing in the corner*
				return readers.stream().map(reader -> {
					try {
						log.log(Level.INFO, () -> String.format("EXECUTE :: %30s", reader));
						if ((flags & ONLY_PREPROCESSOR) > 0) {
							CLI.runPreprocessor(reader, io);
							io.println("^D");
						} else
							CLI.doFullExecution(reader, new Interpreter(io), io, flags);
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
					CLI.doFullExecution(new StringReader(this.executionStrings.get(0)), new Interpreter(io), io, flags);
				} catch (Throwable t) {
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
						Node ast = null;
						try {
							// may throw
							ast = Parser.parse(Preprocessor.preprocessCode(code));
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
							ast = Parser.parse(Preprocessor.preprocessCode(code));
						}
						// in any case, execute
						if (ast != null) {
							log.fine(ast.toString());
							engine.run(ast, code);
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
	 * Create an Options object given the command line arguments.
	 * 
	 * @param args command line arguments.
	 * @return an Options argument that represents the cla in a more abstract way.
	 * @throws IllegalArgumentException if the arguments to the interpreter are
	 *                                  malformed
	 */
	public static Options parseOptions(String[] args) throws IllegalArgumentException {
		log.entering(CLI.class.getCanonicalName(), "parseOptions");

		Options opt = new Options();
		opt.executionType = Options.ExecutionType.Interactive;
		opt.executionStrings = new LinkedList<String>();

		List<String> cmdLineArguments = new ArrayList<String>(Arrays.asList(args));
		log.config(() -> "Command line arguments: " + cmdLineArguments.toString());

		var idx = 0;
		while (idx < cmdLineArguments.size() && cmdLineArguments.get(idx).startsWith("-")) {
			String s = cmdLineArguments.get(idx++);
			switch (s) {
				case "-v":
				case "--version":
					opt.executionType = ExecutionType.VersionInfo;
					break;
				case "-h":
				case "--help":
					opt.executionType = ExecutionType.HelpInfo;
					break;
				case "-c":
				case "--command":
					opt.executionType = Options.ExecutionType.Literal;
					if (idx - 1 >= cmdLineArguments.size() - 1) {
						throw new IllegalArgumentException("No parameter specified for option -c. See -h for help.");
					}
					opt.executionStrings.add(0, cmdLineArguments.get(idx++));
					break;
				case "-d":
					opt.flags |= Options.DEBUG;
					break;
				case "-p":
					opt.flags |= Options.ONLY_PREPROCESSOR;
					break;
				case "-P":
					opt.flags |= Options.NO_PREPROCESSOR;
					break;
				case "--performance":
					opt.flags |= Options.PERFORMANCE;
					break;
				default:
					// extract combined option flags into separate options

					if (s.length() <= 2) {
						throw new IllegalArgumentException(String.format("Unknown option \"%s\". Try -h for help.", s));
					}
					for (char c : s.substring(1).toCharArray()) {
						cmdLineArguments.add(idx, "-" + c);
					}
					log.log(Level.FINE, () -> cmdLineArguments.toString());
			}
		}
		// decide over execution type depending on argument count
		if (opt.executionType == Options.ExecutionType.Interactive)
			opt.executionType = idx < cmdLineArguments.size() ? Options.ExecutionType.File
					: Options.ExecutionType.Interactive;
		if (opt.executionType == Options.ExecutionType.File)
			while (idx < cmdLineArguments.size()) {
				opt.executionStrings.add(cmdLineArguments.get(idx++));
				String last = opt.executionStrings.get(opt.executionStrings.size() - 1);
				if (last.startsWith("-")) {
					throw new IllegalArgumentException(String.format("Unknown option \"%s\". Try -h for help.%n", last));
				}
			}

		log.exiting(CLI.class.getCanonicalName(), "parseOptions");
		return opt;
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
