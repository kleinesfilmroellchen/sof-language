package klfr.sof;

import static klfr.sof.Interpreter.R;

import java.io.*;
import java.util.*;

import klfr.sof.lang.Stack;
import klfr.sof.lang.Stackable.DebugStringExtensiveness;

/**
 * Wrapper for all I/O functionality that SOF has. This is primarily concerned with standard input and output.
 * 
 * @author klfr
 */
public final class IOInterface {

	/**
	 * Number of milliseconds to wait between short prints until flushing anyways.
	 */
	public static final long	FLUSH_MILLIS	= 100;

	private Readable				input;
	private Writer					output;

	private Scanner				scan;

	/**
	 * Whether the debug mode on this I/O interface is enabled. If debug is on, the debug(String) operation can print to the
	 * output.
	 * 
	 * @see IOInterface#debug(String)
	 */
	public boolean					debug;

	/**
	 * Creates an uninitialized I/O interface.
	 */
	public IOInterface() {
	}

	/**
	 * Initializes the interface with basic I/O.
	 * 
	 * @param in  The input readable.
	 * @param out The output writer.
	 */
	public IOInterface(Readable in, Writer out) {
		this();
		this.setInOut(in, out);
	}

	/**
	 * Initializes the interface with basic I/O streams which are wrapped in character encoding streams.
	 * 
	 * @param in  The input stream which is used with the default character encoding.
	 * @param out The output stream which is used with the default character encoding.
	 */
	public IOInterface(InputStream in, OutputStream out) {
		this();
		this.setInOut(in, out);
	}

	/**
	 * Returns the output writer of this I/O interface.
	 * 
	 * @return the output writer of this I/O interface.
	 */
	public final Writer getOutput() {
		return output;
	}

	/**
	 * Returns the input reader of this I/O interface.
	 * 
	 * @return The input reader of this I/O interface.
	 */
	public final Readable getInput() {
		return input;
	}

	/**
	 * Sets the output of this I/O interface. Note that this method sets a raw byte stream which is converted to an encoded
	 * character stream with the default encoding before use.
	 * 
	 * @param out The output stream to be set.
	 */
	public final void setOut(OutputStream out) {
		if (out != null)
			this.setOut(new OutputStreamWriter(out));
	}

	/**
	 * Sets the output of this I/O interface.
	 * 
	 * @param out The output stream to be set.
	 */
	public final void setOut(Writer out) {
		// hijack the output to do special auto-flushing
		if (out != null)
			this.output = new Writer() {

				private Thread t = new Thread("StreamFlusher#" + this.hashCode());

				@Override
				public void write(char[] cbuf, int off, int len) throws IOException {
					out.write(cbuf, off, len);
					// only start a new flusher if one has not yet been started
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

	/**
	 * Sets the input readable.
	 * 
	 * @param in The input readable to set.
	 */
	public final void setIn(Readable in) {
		if (in != null) {
			this.input = in;
			this.scan = new Scanner(in);
		}
	}

	/**
	 * Sets the input stream. Note that this input stream is converted to a Readable with the default character encoding.
	 * 
	 * @param in The input stream.
	 */
	public final void setIn(InputStream in) {
		if (in != null)
			this.setIn(new InputStreamReader(in));
	}

	/**
	 * Utility method for setting both basic I/O streams. Note that this method sets raw byte streams which are converted to
	 * encoded character streams before use.
	 * 
	 * @param in  The InputStream to use. If this is null, the Input is not changed.
	 * @param out The OutputStream to use. If this is null, the Output is not changed.
	 */
	public final void setInOut(InputStream in, OutputStream out) {
		this.setIn(in);
		this.setOut(out);
	}

	/**
	 * Directly sets reader and writer of the I/O interface.
	 * 
	 * @param in  The Readable (Character input) to use. If this is null, the Input is not changed.
	 * @param out The Writable (Character output) to use. If this is null, the Output is not changed.
	 */
	public final void setInOut(Readable in, Writer out) {
		this.setIn(in);
		this.setOut(out);
	}

	/**
	 * Creates and returns a new scanner over the basic InputStream.
	 * 
	 * @return a new scanner over the basic InputStream.
	 */
	public final Scanner newInputScanner() {
		scan = new Scanner(input);
		return scan;
	}

	/**
	 * Returns the next input sequence, as defined by the input sequence terminology of SOF's input builtin. An input
	 * sequence is any sequence of non-whitespace characters.
	 * 
	 * @return The next input sequence.
	 */
	public final String nextInputSequence() {
		return scan.next();
	}

	/**
	 * Reads and returns the next line from the input.
	 * 
	 * @return The next line from the input.
	 */
	public final String nextInputLine() {
		return scan.nextLine();
	}

	/**
	 * Print the given string followed by a new line.
	 * 
	 * @param x The string to print.
	 */
	public final void println(String x) {
		print(x);
		println();
	}

	/**
	 * Print a new line.
	 */
	public final void println() {
		try {
			output.write(System.lineSeparator());
			output.flush();
		} catch (IOException e) {
			// TODO handle this differently?
			e.printStackTrace();
		}
	}

	/**
	 * Print the given string.
	 * 
	 * @param s The string to print.
	 */
	public final void print(String s) {
		try {
			output.write(s != null ? s : "null");
		} catch (IOException e) {
			// TODO handle this differently?
			e.printStackTrace();
		}
	}

	/**
	 * Print the string formatted with the format arguments.
	 * 
	 * @param format The string to format.
	 * @param args   The format arguments.
	 * @see String#format(String, Object...)
	 */
	public final void printf(String format, Object... args) {
		String toprint = String.format(format, args);
		this.print(toprint);
	}

	/**
	 * Print the string formatted with the format arguments given the locale.
	 * 
	 * @param format The string to format.
	 * @param args   The format arguments.
	 * @param l      The locale to use for formatting.
	 * @see String#format(String, Object...)
	 */
	public final void printf(Locale l, String format, Object... args) {
		String toprint = String.format(l, format, args);
		this.print(toprint);
	}

	/**
	 * Prints formatted (just as {@code printf} does) and terminates the line.
	 * 
	 * @param format The string to format.
	 * @param args   The format arguments.
	 * @see IOInterface#printf(String, Object...)
	 */
	public final void printfln(String format, Object... args) {
		String toprint = String.format(format, args);
		this.println(toprint);
	}

	/**
	 * Prints formatted (just as {@code printf} does) and terminates the line.
	 * 
	 * @param format The string to format.
	 * @param args   The format arguments.
	 * @param l      The locale to use for formatting.
	 * @see IOInterface#printf(Locale, String, Object...)
	 */
	public final void printfln(Locale l, String format, Object... args) {
		String toprint = String.format(l, format, args);
		this.println(toprint);
	}

	/**
	 * Print the stack description if debug is enabled. This will print the stack and the global nametable.
	 * 
	 * @param stack The stack to describe.
	 */
	public final void describeStack(Stack stack) {
		if (debug) {
			println(R.getString("sof.debug.stack") + System.lineSeparator() + stack.toStringExtended());
			println(R.getString("sof.debug.gnt") + System.lineSeparator() + stack.globalNametable().toDebugString(DebugStringExtensiveness.Full));
		}
	}

	/**
	 * Only {@code println} the string when debug is enabled.
	 * 
	 * @param s The debug string to print.
	 */
	public final void debug(String s) {
		if (debug)
			println(s);
	}

	//#region print overloads
	/**
	 * Print the given boolean; print "true" or "false".
	 * 
	 * @param b The boolean to print.
	 */
	public final void print(boolean b) {
		print(Boolean.toString(b));
	}

	/**
	 * Print the given character.
	 * 
	 * @param c The character to print.
	 */
	public final void print(char c) {
		print(Character.toString(c));
	}

	/**
	 * Print the given integer in decimal.
	 * 
	 * @param i The integer to print.
	 */
	public final void print(int i) {
		print(Integer.toString(i));
	}

	/**
	 * Print the given long in decimal.
	 * 
	 * @param l The long to print.
	 */
	public final void print(long l) {
		print(Long.toString(l));
	}

	/**
	 * Print the given float in decimal.
	 * 
	 * @param f The float to print.
	 */
	public final void print(float f) {
		print(Float.toString(f));
	}

	/**
	 * Print the given double in decimal.
	 * 
	 * @param d The decimal to print.
	 */
	public final void print(double d) {
		print(Double.toString(d));
	}

	/**
	 * Print the given character array.
	 * 
	 * @param s The character array to print.
	 */
	public final void print(char[] s) {
		print(String.copyValueOf(s));
	}

	/**
	 * Print the given object after converting it with {@link Object#toString()}, or "null" if it is null.
	 * 
	 * @param obj The object to print.
	 */
	public final void print(Object obj) {
		print(obj.toString());
	}

	// println overloads
	/**
	 * Print the given boolean; print "true" or "false" and then a new line.
	 * 
	 * @param x The boolean to print.
	 */
	public final void println(boolean x) {
		println(Boolean.toString(x));
	}

	/**
	 * Print the given character and then a new line.
	 * 
	 * @param x The character to print.
	 */
	public final void println(char x) {
		println(Character.toString(x));
	}

	/**
	 * Print the given integer in decimal and then a new line.
	 * 
	 * @param x The integer to print.
	 */
	public final void println(int x) {
		println(Integer.toString(x));
	}

	/**
	 * Print the given long in decimal and then a new line.
	 * 
	 * @param x The long to print.
	 */
	public final void println(long x) {
		println(Long.toString(x));
	}

	/**
	 * Print the given float in decimal and then a new line.
	 * 
	 * @param x The float to print.
	 */
	public final void println(float x) {
		println(Float.toString(x));
	}

	/**
	 * Print the given double in decimal and then a new line.
	 * 
	 * @param x The decimal to print.
	 */
	public final void println(double x) {
		println(Double.toString(x));
	}

	/**
	 * Print the given character array and then a new line.
	 * 
	 * @param x The character array to print.
	 */
	public final void println(char[] x) {
		println(String.copyValueOf(x));
	}

	/**
	 * Print the given object after converting it with {@link Object#toString()}, or "null" if it is null. Then print a
	 * newline.
	 * 
	 * @param x The object to print.
	 */
	public final void println(Object x) {
		println(x.toString());
	}

	//#endregion print overloads

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
