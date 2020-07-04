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
				//// File interpretation
				log.log(Level.FINE, () -> this.executionStrings.toString());
				List<Reader> readers = new ArrayList<>(this.executionStrings.size());
				for (String filename : this.executionStrings) {
					try {
						Reader strd = new FileReader(filename);
						readers.add(strd);
					} catch (FileNotFoundException e) {
						io.printf("error: file %s not found, check access restrictions.%n", filename);
						CLI.exitUnnormal(2);
					}
				}
				// TODO use other interpreters in certain cases
				Interpreter interpreterPrototype = new Interpreter();
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
							CLI.doFullExecution(reader, interpreterPrototype.instantiateSelf(), io);
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
				Interpreter interpreter = new Interpreter();
				try {
					CLI.doFullExecution(new StringReader(this.executionStrings.get(0)), interpreter, io);
				} catch (Throwable t) {
					return Optional.of(t);
				}
				break;
			}
			case Interactive: {
				//// Interactive interpretation
				io.println(CLI.getInfoString());
				Interpreter interpreter = new Interpreter().reset();
				interpreter.internal.setIO(io);

				Scanner scanner = io.newInputScanner();
				// scanner.useDelimiter("[[^\n]\\s+]");
				io.print(">>> ");
				while (scanner.hasNextLine()) {
					String code = scanner.nextLine();
					var curEnd = interpreter.internal.tokenizer().getState().code.length();
					// catches all unwanted compilation errors
					try {
						// catches "unclosed"-compilation errors which might be resolved by adding more
						// content on another line
						try {
							interpreter.appendLine(code);
						} catch (CompilerException e) {
							// one invalid line: let user input as many continuation lines as they want
							while (true) {
								io.print("... ");
								var nl = scanner.nextLine();
								// end on blank line
								if (nl.isBlank())
									break;
								code += System.lineSeparator() + nl;
							}
							interpreter.appendLine(code);
						}
						var state = interpreter.internal.tokenizer().getState();
						state.end = curEnd;
						interpreter.internal.tokenizer().setState(state);
						while (interpreter.canExecute()) {
							interpreter.executeOnce();
						}
					} catch (CompilerException e) {
						io.println("!!! " + e.getLocalizedMessage());
						log.log(Level.SEVERE,
								("Compiler Exception occurred.\nUser-friendly message: " + e.getLocalizedMessage()
										+ "\nStack trace:\n" + Arrays.stream(e.getStackTrace()).map(ste -> ste.toString())
												.reduce("", (a, b) -> (a + "\n  " + b).strip())
										+ "\n").indent(2));
					}
					io.print(">>> ");
				}
				scanner.close();
				break;
			}
			case VersionInfo: {
				io.println(CLI.getInfoString());
				io.printf(
						"This program is licensed under GNU General Public License 3.0.%nSee the project LICENSE for details.%n");
				break;
			}
			case HelpInfo: {
				io.printf("sof - Interpreter for Stack with Objects and%n" + "      Functions (SOF) Programming Language.%n"
						+ "usage: sof [-h|-v] [-d] [-c command]%n" + "           filename [...filenames]%n" + "%n"
						+ "positional arguments:%n" + "   filename  Path to a file to be read and%n"
						+ "             executed. Can be a list of files that%n" + "             are executed in order.%n"
						+ "             %n" + "options:%n" + "   --help, -h%n"
						+ "             Display this help message and exit.%n" + "   --version, -v%n"
						+ "             Display version information and exit.%n"
						+ "   -d        Execute in debug mode. Read the manual%n" + "             for more information.%n"
						+ "   --command=<command>, -c <command>%n" + "             Execute <command> and exit.%n"
						+ "             %n" + "When used without execution-starting arguments (-c%n"
						+ "or filename), sof is started in interactive mode.%n" + "%n" + "Quit the program with ^C.%n"
						+ "%n");
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
			String s = cmdLineArguments.get(idx++).toLowerCase();
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
					if (idx - 1 >= args.length - 1) {
						throw new IllegalArgumentException("No parameter specified for option -c. See -h for help.");
					}
					opt.executionStrings.add(0, args[idx++]);
					break;
				case "-d":
					opt.flags |= Options.DEBUG;
					break;
				case "-p":
					opt.flags |= Options.ONLY_PREPROCESSOR;
					break;
				default:
					// extract combined option flags into separate options

					if (s.length() <= 2) {
						throw new IllegalArgumentException(String.format("Unknown option \"%s\". Try -h for help.", s));
					}
					for (char c : s.substring(1).toCharArray()) {
						cmdLineArguments.add("-" + c);
					}
					log.log(Level.FINE, () -> cmdLineArguments.toString());
			}
		}
		// decide over execution type depending on argument count
		if (opt.executionType == Options.ExecutionType.Interactive)
			opt.executionType = idx < args.length ? Options.ExecutionType.File : Options.ExecutionType.Interactive;
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