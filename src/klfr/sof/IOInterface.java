package klfr.sof;

import java.io.*;
import java.util.Locale;
import java.util.Scanner;

import klfr.sof.lang.Stack;

/**
 * Wrapper for all I/O functionality that SOF has.
 * @author klfr
 */
public class IOInterface {

	public InputStream	input;
	private OutputStream	output;
	
	public boolean debug;

	public OutputStream getOutput() {
		return output;
	}

	public void setOutput(OutputStream output) {
		this.output = output;
		outputPrinter = new PrintStream(output);
	}

	private PrintStream outputPrinter;

	/**
	 * Utility method for setting both basic I/O streams.
	 */
	public void setStreams(InputStream in, OutputStream out) {
		this.input = in;
		this.output = out;
		outputPrinter = new PrintStream(out);
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
		if (outputPrinter != null)
			outputPrinter.println();
	}

	public void print(String s) {
		if (outputPrinter != null)
			outputPrinter.print(s);
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
	 * Print the stack description if debug is enabled.
	 * @param stack
	 */
	public void describeStack(Stack stack) {
		if (debug) {
			println("Stack: " + System.lineSeparator() + Interpreter.stackToDebugString(stack));
			println("Global Nametable: " + System.lineSeparator() + stack.globalNametable().getDebugDisplay());
		}
	}
	
	/**
	 * Only {@code println} the string when debug is enabled.
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
