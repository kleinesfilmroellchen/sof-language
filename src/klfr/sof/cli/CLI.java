package klfr.sof.cli;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import klfr.sof.CompilerException;
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
		public static final int DEBUG = 0b1;
		public ExecutionType executionType;
		public List<String> executionStrings;
		/**
		 * All flags binary OR-ed together (binary AND with a certain flag to check it).
		 */
		public int flags;

		public String toString() {
			return "Options:" + executionType + executionStrings.toString() + "flags:" + Integer.toBinaryString(flags);
		}
	}

	public static void main(String[] args) throws InvocationTargetException {
		int idx = 0;

		// options
		Options opt = new Options();
		opt.executionType = Options.ExecutionType.Interactive;
		opt.executionStrings = new LinkedList<String>();
		List<String> cmdLineArguments = new ArrayList<String>(args.length);
		for (String e : args)
			cmdLineArguments.add(e);
		while (idx < cmdLineArguments.size() && cmdLineArguments.get(idx).startsWith("-")) {
			String s = cmdLineArguments.get(idx++).toLowerCase();
			switch (s) {
			case "-v":
			case "--version":
				System.out.println(getInfoString());
				exitUnnormal(0);
			case "-h":
			case "--help":
				//                                                        |
				// we want the new multiline strings here, but eclipse is not capable of java 13 yet
				System.out.printf(
						"sof - Interpreter for Stack with Objects and%n" +
						"      Functions (SOF) Programming Language.%n" +
						"usage: sof [-h|-v] [-d] [-c command]%n" +
						"           filename [...filenames]%n" +
						"%n" +
						"positional arguments:%n" +
						"   filename  Path to a file to be read and%n" +
						"             executed. Can be a list of files that%n" +
						"             are executed in order.%n" +
						"             %n" +
						"options:%n" +
						"   --help, -h%n" +
						"             Display this help message and exit.%n" +
						"   --version, -v%n" +
						"             Display version information and exit.%n" +
						"   -d        Execute in debug mode. Read the manual%n" +
						"             for more information.%n" +
						"   --command=<command>, -c <command>%n" +
						"             Execute <command> and exit.%n" +
						"             %n" +
						"When used without execution-starting arguments (-c%n" +
						"or filename), sof is started in interactive mode.%n" +
						"%n" +
						"Quit the program with ^C.%n" +
						"%n");
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
				// extract combined option flags into separate options
				String remaining = s.substring(1);
				if (remaining.length() <= 1) {
					System.out.printf("Unknown option \"%s\". Try -h for help.%n", s);
					exitUnnormal(1);
				}
				for (char c : remaining.toCharArray()) {
					cmdLineArguments.add("-" + c);
				}
				System.out.println(cmdLineArguments.toString());
			}
		}

		IOInterface io = new IOInterface();
		io.debug = (opt.flags & Options.DEBUG) > 0;
		io.setInOut(System.in, System.out);

		// decide over execution type depending on argument count
		if (opt.executionType == Options.ExecutionType.Interactive)
			opt.executionType = idx < args.length ? Options.ExecutionType.File : Options.ExecutionType.Interactive;
		
		if (opt.executionType == Options.ExecutionType.File) {
			//// File interpretation
			while (idx < cmdLineArguments.size()) {
				opt.executionStrings.add(cmdLineArguments.get(idx++));
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
			//// Single literal to be executed
			Interpreter interpreter = new Interpreter();
			doFullExecution(new StringReader(opt.executionStrings.get(0)), interpreter, io);
			
		} else if (opt.executionType == Options.ExecutionType.Interactive) {
			//// Interactive interpretation
			io.println(getInfoString());
			Interpreter interpreter = new Interpreter().reset();
			interpreter.setIO(io);
			
			Scanner scanner = io.newInputScanner();
			// scanner.useDelimiter("[[^\n]\\s+]");
			io.print(">>> ");
			 while (scanner.hasNextLine()) {
				String code = scanner.nextLine();
				// catches all unwanted compilation errors
				try {
					// catches "unclosed"-compilation errors which might be resolved by adding more content on another line
					try {
						interpreter.appendLine(code);
					} catch (CompilerException e) {
						// one invalid line: let user input as many continuation lines as they want
						while (true) {
							io.print("... ");
							var nl = scanner.nextLine();
							// end on blank line
							if (nl.isBlank()) break;
							code += System.lineSeparator() + nl;
						}
						interpreter.appendLine(code);
					}
					while (interpreter.canExecute()) {
						interpreter.executeOnce();
					}
				} catch (CompilerException e) {
					io.println("!!! " + e.getLocalizedMessage());
				}
				io.print(">>> ");
			};
			scanner.close();
		}
	}

	/**
	 * Does full execution on one reader's input
	 * 
	 * @param codeStream  A reader that reads source code.
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
		} catch (CompilerException e) {
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
				// awww yesss, the Java Time API 😋
				DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
						.format(buildTime().atZone(ZoneId.systemDefault())));
	}

	public static Instant buildTime() {
		try {
			URI classuri = CLI.class.getClassLoader()
					.getResource(CLI.class.getCanonicalName().replace(".", "/") + ".class").toURI();
//			System.out.println(classuri.getScheme());
			if (classuri.getScheme().equals("rsrc") || classuri.getScheme().equals("jar")) {
				// we are in a jar file
				// returns the containing folder of the jar file
				// String jarpath = new
				// File(ClassLoader.getSystemResource(".").getFile()).getCanonicalPath();
				String jarfilepath = new File(".").getCanonicalPath() + File.separator
						+ System.getProperty("java.class.path");
//				System.out.println(jarfilepath);
				return Instant.ofEpochMilli(new File(jarfilepath).lastModified());
			} else if (classuri.getScheme().equals("file")) {
				return Instant.ofEpochMilli(new File(classuri.getRawPath()).lastModified());
			}
		} catch (URISyntaxException e) {} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

}
