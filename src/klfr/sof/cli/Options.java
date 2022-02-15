package klfr.sof.cli;

import java.io.*;
import java.util.*;
import java.util.logging.*;

/**
 * Command-line options storage and parsing.
 */
final class Options implements Serializable {
	private static final long serialVersionUID = 1L;
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
	public Optional<String> overrideLibraryPath = Optional.empty();
	public List<String> executionStrings;
	/**
	 * All flags binary OR-ed together (binary AND with a certain flag to check it).
	 */
	public int flags;

	public final String toString() {
		return "Options:" + executionType + executionStrings.toString() + "f:" + Integer.toBinaryString(flags);
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
				case "-l":
				case "--library":
					if (idx - 1 >= cmdLineArguments.size() - 1) {
						throw new IllegalArgumentException("No parameter specified for option -l. See -h for help.");
					}
					opt.overrideLibraryPath = Optional.ofNullable(cmdLineArguments.get(idx++));
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
