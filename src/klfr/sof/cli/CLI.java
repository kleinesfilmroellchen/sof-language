package klfr.sof.cli;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import klfr.sof.CompilationError;
import klfr.sof.IOInterface;
import klfr.sof.Interpreter;

public class CLI {

	static class Options {

		static enum ExecutionType {
			/** ES is treated as list of relative filenames. */
			File,
			/** ES index 0 is treated as code. */
			Literal,
			/** ES is ignored, wait for user input instead. */
			Interactive
		}

		/** Debug flag constant. */
		public static final int	DEBUG	= 0b1;
		public ExecutionType		executionType;
		public List<String>		executionStrings;
		/**
		 * All flags binary OR-ed together (binary AND with a certain flag to check it).
		 */
		public int					flags;

		public String toString() {
			return "Options:" + executionType + executionStrings.toString() + "Flags:" + Integer.toBinaryString(flags);
		}
	}

	public static void main(String[] args) throws InvocationTargetException {
		int idx = 0;

		// options
		Options opt = new Options();
		opt.executionType = Options.ExecutionType.Interactive;
		opt.executionStrings = new LinkedList<String>();
		while (idx < args.length && args[idx].startsWith("-")) {
			String s = args[idx++].toLowerCase();
			switch (s) {
			case "-v":
			case "--version":
				System.out.println(getInfoString());
				exitUnnormal(0);
			case "-h":
			case "--help":
				System.out.printf(
						"sof - Interpreter for Stack with Objects and Functions Language."
								+ "%nusage: sof [-h|-v]"
								+ "%n       sof [-d] [-c command]"
								+ "%n       sof [-d] filename [...filenames]"
								+ "%n       sof [-d]"
								+ "%n%noptions:"
								+ "%n   --help"
								+ "%n   -h           Display this help message and exit.%n"
								+ "%n   --version"
								+ "%n   -v           Display version information and exit.%n"
								+ "%n   -d           Execute in debug mode. Read the manual for%nmore information.%n"
								+ "%n   --command"
								+ "%n      <command>"
								+ "%n   -c <command> Execute command and exit.%n"
								+ "%n   filename     Path to a file to be read and executed. Can%n be a list of files that are executed in order.%n"
								+ "%nWhen used without execution-starting arguments (-c or "
								+ "%nfilename), sof is started in interactive mode.%n"
								+ "%nQuit the program with ^C.%n%n");
				exitUnnormal(0);
			case "-c":
			case "--command":
				opt.executionType = Options.ExecutionType.Literal;
				if (idx - 1 >= args.length - 1) {
					System.out.println("No parameter specified for option -c. See -h for help.");
					exitUnnormal(1);
				}
				opt.executionStrings.add(0, args[idx++]);
				break;
			case "-d":
				opt.flags |= Options.DEBUG;
				break;
			default:
				System.out.printf("Unknown option \"%s\". Try -h for help.%n", s);
				exitUnnormal(1);
			}
		}

		//System.out.println(opt);
		IOInterface io = new IOInterface();
		io.setStreams(System.in, System.out);

		//decide over execution type depending on argument count
		if (opt.executionType == Options.ExecutionType.Interactive)
			opt.executionType = idx < args.length ? Options.ExecutionType.File : Options.ExecutionType.Interactive;
		if (opt.executionType == Options.ExecutionType.File) {
			while (idx < args.length) {
				opt.executionStrings.add(args[idx++]);
				String last = opt.executionStrings.get(opt.executionStrings.size() - 1);
				if (last.startsWith("-")) {
					System.out.printf("Unknown option \"%s\". Try -h for help.%n", last);
					exitUnnormal(1);
				}
			}
			System.out.println(opt.executionStrings);
			List<Reader> readers = new ArrayList<>(opt.executionStrings.size());
			for (String filename : opt.executionStrings) {
				try {
					Reader strd = new FileReader(filename);
					readers.add(strd);
				} catch (FileNotFoundException e) {
					System.out.printf("File %s not found, check access restrictions.%n", filename);
					exitUnnormal(2);
				}
			}
			// TODO use other interpreters in certain cases
			Interpreter interpreterPrototype = new Interpreter();
			readers.forEach(reader -> doFullExecution(reader, interpreterPrototype.instantiateSelf(), io));
			System.exit(0);
		} else if (opt.executionType == Options.ExecutionType.Literal) {
			Interpreter interpreter = new Interpreter();
			doFullExecution(new StringReader(opt.executionStrings.get(0)), interpreter, io);
		} else if (opt.executionType == Options.ExecutionType.Interactive) {
			io.println(getInfoString());
			Interpreter interpreter = new Interpreter().reset();
			interpreter.setIO(io);
			Scanner scanner = new Scanner(System.in);
			while (true) {
				io.print("➤ ");
				String code = scanner.nextLine();
				try {
					interpreter.setCode(code);
					while (interpreter.canExecute()) {
						interpreter.executeOnce();
					}
				} catch (CompilationError e) {
					io.println(e.getMessage());
				} finally {
					try {
						interpreter.setCode("");
					} catch (CompilationError e) {//the catch should not happen
					}
				}
			}
		}
	}

	/**
	 * Does full execution on one reader's input
	 * @param codeStream A reader that reads source code.
	 * @param interpreter The interpreter to use for the execution.
	 */
	public static void doFullExecution(Reader codeStream, Interpreter interpreter, IOInterface io) {
		String code = "";
		try {
			StringWriter writer = new StringWriter();
			codeStream.transferTo(writer);
			code = writer.getBuffer().toString();
		} catch (IOException e) {
			System.out.println("Unknown exception occurred during input reading.");
			exitUnnormal(2);
		}

		try {
			interpreter.reset().setCode(code).setIO(io);
			while (interpreter.canExecute())
				interpreter.executeOnce();
		} catch (CompilationError e) {
			System.out.println(e.getLocalizedMessage());
			System.exit(-1);
		}

	}

	public static void exitUnnormal(int status) {
		System.out.println("So long, and thank's for all the fish...");
		System.exit(status);
	}

	public static String getInfoString() {
		return String.format("sof version %s (built %s)", Interpreter.VERSION,
				//awww yesss, the Java Time API 😋
				DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
						.format(buildTime().atZone(ZoneId.systemDefault())));
	}

	public static Instant buildTime() {
		try {
			return Instant.ofEpochMilli(
					new File(CLI.class.getClassLoader().getResource(
							CLI.class.getCanonicalName().replace('.', '/') + ".class").toURI()).lastModified());
		} catch (URISyntaxException e) {
			e.printStackTrace();
			System.exit(Integer.MIN_VALUE);
		}
		return null;
	}

}
