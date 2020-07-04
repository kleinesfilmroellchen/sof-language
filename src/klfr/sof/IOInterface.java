package klfr.sof;

import java.io.*;
import java.util.Locale;
import java.util.Scanner;

import klfr.sof.lang.Stack;
import klfr.sof.lang.Stackable.DebugStringExtensiveness;

/**
 * Wrapper for all I/O functionality that SOF has.
 * 
 * @author klfr
 */
public class IOInterface {

	/**
	 * Number of milliseconds to wait between short prints until flushing anyways.
	 */
	public static final long FLUSH_MILLIS = 500;

	public Readable input;
	private Writer output;

	public boolean debug;

	public IOInterface() {
	}

	/**
	 * Initializes the interface with basic I/O.
	 */
	public IOInterface(Readable in, Writer out) {
		this();
		this.setInOut(in, out);
	}

	/**
	 * Initializes the interface with basic I/O streams which are wrapped in
	 * character encoding streams.
	 */
	public IOInterface(InputStream in, OutputStream out) {
		this();
		this.setInOut(in, out);
	}

	public Writer getOutput() {
		return output;
	}

	public Readable getInput() {
		return input;
	}

	/**
	 * Sets the output of this I/O interface. Note that this method sets a raw byte
	 * stream which is converted to an encoded character stream before use.
	 * 
	 * @param out
	 */
	public void setOut(OutputStream out) {
		if (out != null)
			this.setOut(new OutputStreamWriter(out));
	}

	/**
	 * Sets the output of this I/O interface.
	 * 
	 * @param out
	 */
	public void setOut(Writer out) {
		// hijack the output to do special auto-flushing
		if (out != null)
			this.output = new Writer() {

				private Thread t = new Thread("StreamFlusher#" + this.hashCode());

				@Override
				public void write(char[] cbuf, int off, int len) throws IOException {
					out.write(cbuf, off, len);
					// interrupt the currently running thread
					// if it had already ended, this has no effect
					// if it didn't this will prevent the thread from executing the flush
					if (!t.isAlive()) {
						// create a thread that will wait FLUSH_MILLIS and flush the writer
						t = new Thread(() -> {
							try {
								Thread.sleep(FLUSH_MILLIS);
								out.flush();
							} catch (InterruptedException | IOException e) {
								return;
							}
						}, "StreamFlusher#" + this.hashCode());
						t.setDaemon(false);
						t.start();
					}
				}

				@Override
				public void flush() throws IOException {
					out.flush();
				}

				@Override
				public void close() throws IOException {
					out.close();
				}
			};
	}

	public void setIn(Readable in) {
		if (in != null)
			this.input = in;
	}

	public void setIn(InputStream in) {
		if (in != null)
			this.setIn(new InputStreamReader(in));
	}

	/**
	 * Utility method for setting both basic I/O streams. Note that this method sets
	 * raw byte streams which are converted to encoded character streams before use.
	 * 
	 * @param in  The InputStream to use. If this is null, the Input is not changed.
	 * @param out The OutputStream to use. If this is null, the Output is not
	 *            changed.
	 */
	public void setInOut(InputStream in, OutputStream out) {
		this.setIn(in);
		this.setOut(out);
	}

	/**
	 * Directly sets reader and writer of the I/O interface.
	 * 
	 * @param in  The Readable (Character input) to use. If this is null, the Input
	 *            is not changed.
	 * @param out The Writable (Character output) to use. If this is null, the
	 *            Output is not changed.
	 */
	public void setInOut(Readable in, Writer out) {
		this.setIn(in);
		this.setOut(out);
	}

	/**
	 * Creates and returns a new scanner over the basic InputStream.
	 */
	public Scanner newInputScanner() {
		return new Scanner(input);
	}

	public void println(String x) {
		print(x);
		println();
	}

	public void println() {
		try {
			output.write(System.lineSeparator());
			output.flush();
		} catch (IOException e) {
			// TODO handle this differently?
			e.printStackTrace();
		}
	}

	public void print(String s) {
		try {
			output.write(s);
		} catch (IOException e) {
			// TODO handle this differently?
			e.printStackTrace();
		}
	}

	public void printf(String format, Object... args) {
		String toprint = String.format(format, args);
		this.print(toprint);
	}

	public void printf(Locale l, String format, Object... args) {
		String toprint = String.format(l, format, args);
		this.print(toprint);
	}

	/**
	 * Prints formatted (just as {@code printf} does) and terminates the line.
	 */
	public void printfln(String format, Object... args) {
		String toprint = String.format(format, args);
		this.println(toprint);
	}

	/**
	 * Prints formatted (just as {@code printf} does) and terminates the line.
	 */
	public void printfln(Locale l, String format, Object... args) {
		String toprint = String.format(l, format, args);
		this.println(toprint);
	}

	/**
	 * Print the stack description if debug is enabled.
	 * 
	 * @param stack
	 */
	public void describeStack(Stack stack) {
		if (debug) {
			println("Stack: " + System.lineSeparator() + stack.toStringExtended());
			println("Global Nametable: " + System.lineSeparator() + stack.globalNametable().toDebugString(DebugStringExtensiveness.Full));
		}
	}

	/**
	 * Only {@code println} the string when debug is enabled.
	 * 
	 * @param s
	 */
	public void debug(String s) {
		if (debug)
			println(s);
	}

	// print overloads
	public void print(boolean b) {
		print(Boolean.toString(b));
	}

	public void print(char c) {
		print(Character.toString(c));
	}

	public void print(int i) {
		print(Integer.toString(i));
	}

	public void print(long l) {
		print(Long.toString(l));
	}

	public void print(float f) {
		print(Float.toString(f));
	}

	public void print(double d) {
		print(Double.toString(d));
	}

	public void print(char[] s) {
		print(String.copyValueOf(s));
	}

	public void print(Object obj) {
		print(obj.toString());
	}

	// println overloads
	public void println(boolean x) {
		println(Boolean.toString(x));
	}

	public void println(char x) {
		println(Character.toString(x));
	}

	public void println(int x) {
		println(Integer.toString(x));
	}

	public void println(long x) {
		println(Long.toString(x));
	}

	public void println(float x) {
		println(Float.toString(x));
	}

	public void println(double x) {
		println(Double.toString(x));
	}

	public void println(char[] x) {
		println(String.copyValueOf(x));
	}

	public void println(Object x) {
		println(x.toString());
	}

}
