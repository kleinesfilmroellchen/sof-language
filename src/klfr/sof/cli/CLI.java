package klfr.sof.cli;

// MOAR STANDARD LIBRARY
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Arrays;
import java.util.MissingResourceException;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import klfr.sof.CompilerException;
import klfr.sof.IOInterface;
import klfr.sof.Interpreter;

public class CLI {

	static Logger log = Logger.getLogger(CLI.class.getCanonicalName());

	public static void main(String[] args) throws InvocationTargetException, UnsupportedEncodingException, IOException {
		// setup console info logging
		// LogManager.getLogManager().updateConfiguration(new
		// ByteArrayInputStream(".level = FINEST".getBytes("UTF-8")), null);
		LogManager.getLogManager().reset();
		Logger.getLogger("").setLevel(Level.FINEST);
		var ch = new ConsoleHandler();
		ch.setLevel(Level.SEVERE);
		Logger.getLogger("").addHandler(ch);
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			Logger.getLogger("").info("SOF exiting.");
		}));

		var opt = Options.parseOptions(args);

		IOInterface io = new IOInterface();
		io.setInOut(System.in, System.out);

		if ((opt.flags & Options.DEBUG) > 0) {
			try {
				LogManager.getLogManager().reset();
				var rootLog = Logger.getLogger("");
				rootLog.setLevel(Level.FINEST);
				ch = new ConsoleHandler();
				ch.setLevel(Level.FINE);

				ch.setFormatter(new Formatter() {
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

						return String.format("[%s %-20s |%6s] %s%n", time, logName, level, msg) + (record.getThrown() == null
								? ""
								: String.format("EXCEPTION: %s | Stack trace:%n%s", record.getThrown().toString(),
										Arrays.asList(record.getThrown().getStackTrace()).stream().map(x -> x.toString()).collect(
												() -> new StringBuilder(),
												(builder, str) -> builder.append("in ").append(str).append(System.lineSeparator()),
												(b1, b2) -> b1.append(b2))));
					}
				});
				rootLog.addHandler(ch);
				var handler = new FileHandler("sof-log.log");
				handler.setFormatter(new SimpleFormatter());
				handler.setEncoding("utf-8");
				handler.setLevel(Level.FINEST);
				rootLog.addHandler(handler);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// execute
		var error = opt.apply(io);
		if (error.isPresent()) {
			var t = error.get();
			log.log(Level.SEVERE,
					String.format("Uncaught Interpreter exception: %s%nStack trace:%n%s", t.getLocalizedMessage(),
							Arrays.asList(t.getStackTrace()).stream().map(ste -> ste.toString())
									.reduce((a, b) -> a + System.lineSeparator() + b).orElse("")));
		}
	}

	/**
	 * Does full execution on one reader's input
	 * 
	 * @param codeStream  A reader that reads source code.
	 * @param interpreter The interpreter to use for the execution.
	 */
	public static void doFullExecution(Reader codeStream, Interpreter interpreter, IOInterface io) throws Exception {
		log.entering(CLI.class.getCanonicalName(), "doFullExecution");
		String code = "";
		try {
			StringWriter writer = new StringWriter();
			codeStream.transferTo(writer);
			code = writer.getBuffer().toString();
		} catch (IOException e) {
			throw new Exception("Unknown exception occurred during input reading.", e);
		}

		interpreter.reset().setCode(code).internal.setIO(io);
		while (interpreter.canExecute())
			interpreter.executeOnce();

		log.exiting(CLI.class.getCanonicalName(), "doFullExecution");
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
