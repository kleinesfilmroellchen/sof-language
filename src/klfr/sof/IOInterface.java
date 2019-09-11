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

	public void print(boolean b) {
		if (outputPrinter != null)
			outputPrinter.print(b);
	}

	public void print(char c) {
		if (outputPrinter != null)
			outputPrinter.print(c);
	}

	public void print(int i) {
		if (outputPrinter != null)
			outputPrinter.print(i);
	}

	public void print(long l) {
		if (outputPrinter != null)
			outputPrinter.print(l);
	}

	public void print(float f) {
		if (outputPrinter != null)
			outputPrinter.print(f);
	}

	public void print(double d) {
		if (outputPrinter != null)
			outputPrinter.print(d);
	}

	public void print(char[] s) {
		if (outputPrinter != null)
			outputPrinter.print(s);
	}

	public void print(String s) {
		if (outputPrinter != null)
			outputPrinter.print(s);
	}

	public void print(Object obj) {
		if (outputPrinter != null)
			outputPrinter.print(obj);
	}

	public void println() {
		if (outputPrinter != null)
			outputPrinter.println();
	}

	public void println(boolean x) {
		if (outputPrinter != null)
			outputPrinter.println(x);
	}

	public void println(char x) {
		if (outputPrinter != null)
			outputPrinter.println(x);
	}

	public void println(int x) {
		if (outputPrinter != null)
			outputPrinter.println(x);
	}

	public void println(long x) {
		if (outputPrinter != null)
			outputPrinter.println(x);
	}

	public void println(float x) {
		if (outputPrinter != null)
			outputPrinter.println(x);
	}

	public void println(double x) {
		if (outputPrinter != null)
			outputPrinter.println(x);
	}

	public void println(char[] x) {
		if (outputPrinter != null)
			outputPrinter.println(x);
	}

	public void println(String x) {
		if (outputPrinter != null)
			outputPrinter.println(x);
	}

	public void println(Object x) {
		if (outputPrinter != null)
			outputPrinter.println(x);
	}

	public PrintStream printf(String format, Object... args) {
		if (outputPrinter != null)
			return outputPrinter.printf(format, args);
		return null;
	}

	public PrintStream printf(Locale l, String format, Object... args) {
		if (outputPrinter != null)
			return outputPrinter.printf(l, format, args);
		return null;
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
	
}
