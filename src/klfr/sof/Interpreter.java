package klfr.sof;

// ALL THE STANDARD LIBRARY
import java.lang.reflect.InvocationTargetException;
import java.util.AbstractMap.SimpleEntry;
import java.util.Deque;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.function.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import klfr.sof.lang.*;

/**
 * The most basic type of an SOF language interpreter without special
 * functionality.<br>
 * Is the central point of the SOF RE because it handles all code execution.<br>
 * <br>
 * The main method of this class is {@code executeOnce()}, which processes one
 * token from the input code and therefore does the smallest step in execution
 * possible.<br>
 * <br>
 * This interpreter does only access I/O on its own given input and output
 * streams, therefore, one can redirect this interpreter's I/O to other sources
 * and destinations easily.<br>
 * <br>
 * This class is special in that it always needs special setup after
 * construction; before that, it is an unusable dummy object. This is necessary
 * because one can more easily pass different interpreters around with the help
 * of unused, freshly instantiated interpreters (and respective subclasses). The
 * constructor therefore does neither recieve code nor throw any exceptions, as
 * the method that recieves the interpreter's code needs to preprocess it, which
 * can cause some types of user-side compilation errors.
 * 
 * @author klfr
 * @version 0.1
 */
public class Interpreter implements Iterator<Interpreter>, Iterable<Interpreter> {
	/**
	 * Access Interpreter internals through this pseudo-class.
	 * 
	 * @deprecated This pseudo-class's accesses the Interpreter internals. Its usage
	 *             may break the currently running SOF interpretation system.
	 */
	@Deprecated()
	public Internal internal = new Internal();

	/**
	 * Access Interpreter internals through this pseudo-class.
	 * 
	 * @deprecated This pseudo-class accesses the Interpreter internals. Its usage
	 *             may break the currently running SOF interpretation system.
	 */
	@Deprecated
	public class Internal {
		@Deprecated
		public Stack stack() {
			return stack;
		}

		/**
		 * Access the Interpreter's tokenizer.
		 * 
		 * @deprecated This method accesses the Interpreter internals. Its usage may
		 *             break the currently running SOF interpretation system.
		 */
		@Deprecated
		public Tokenizer tokenizer() {
			return tokenizer;
		}

		/**
		 * Access the Interpreter's Input-output system.
		 * 
		 * @deprecated This method accesses the Interpreter internals. Its usage may
		 *             break the currently running SOF interpretation system.
		 */
		@Deprecated
		public IOInterface io() {
			return io;
		}

		/**
		 * Push the current tokenizer state onto the tokenizer stack.
		 * 
		 * @deprecated This method accesses the Interpreter internals. Its usage may
		 *             break the currently running SOF interpretation system.
		 */
		@Deprecated
		public void pushState() {
			tokenizer.pushState();
		}

		/**
		 * Pop the current tokenizer state from the tokenizer stack and activate it on
		 * the tokenizer.
		 * 
		 * @deprecated This method accesses the Interpreter internals. Its usage may
		 *             break the currently running SOF interpretation system.
		 */
		@Deprecated
		public void popState() {
			tokenizer.popState();
		}

		/**
		 * Sets the execution region for the interpreter; it is recommended to push the
		 * interpreter state beforehand and popping it back afterwards.
		 * 
		 * @param start start of the region, inclusive.
		 * @param end   end of the region, exclusive.
		 * @deprecated This method accesses the Interpreter internals. Its usage may
		 *             break the currently running SOF interpretation system.
		 */
		@Deprecated
		public void setRegion(final int start, final int end) {
			final var state = tokenizer.getState();
			state.regionStart = start;
			state.regionEnd = end;
			tokenizer.setState(state);
		}

		@Deprecated
		public void setIO(final IOInterface newio) {
			io = newio;
		}
	}

	private final Logger log = Logger.getLogger(Interpreter.class.getCanonicalName());
	public static final String VERSION = "0.1";

	/** Convenience constant for the 66-character line ─ */
	public static final String line66 = String.format("%66s", " ").replace(" ", "─");

	//// PATTERNS
	public static final Pattern intPattern = Pattern.compile("((\\+|\\-)?(0[bhxod])?[0-9a-fA-F]+)|0");
	public static final Pattern doublePattern = Pattern.compile("(\\+|\\-)?([0-9]+\\.[0-9]+([eE][\\-\\+][0-9]+)?)|0");
	public static final Pattern stringPattern = Pattern.compile("\"[^\"]*\"");
	public static final Pattern boolPattern = Pattern.compile("True|False|true|false");
	public static final Pattern tokenPattern = Pattern.compile("(" + stringPattern.pattern() + ")|(\\S+)");// \\b{g}
	/** The pattern to which identifiers must match to be valid */
	public static final Pattern identifierPattern = Pattern.compile("\\p{L}[\\p{L}0-9_']*");
	public static final Pattern codeBlockStartPattern = Pattern.compile("\\{");
	public static final Pattern codeBlockEndPattern = Pattern.compile("\\}");
	public static final Pattern nlPat = Pattern.compile("^", Pattern.MULTILINE);

	/**
	 * Cleans the code of comments.
	 * 
	 * @throws CompilerException if block comments, code blocks or string literals
	 *                           are not closed properly.
	 */
	public static String cleanCode(final String code) throws CompilerException {
		final StringBuilder newCode = new StringBuilder();
		@SuppressWarnings("resource")
		final Scanner scanner = new Scanner(code);
		String line = "";
		boolean insideBlockComment = false;
		int codeBlockDepth = 0;
		int lineIdx = 0;
		while (scanner.hasNextLine()) {
			line = scanner.nextLine();
			++lineIdx;
			if (line.length() == 0)
				continue;
			char c;
			for (int i = 0; i < line.length(); ++i) {
				c = line.charAt(i);

				if (insideBlockComment) {
					// if we have the ending character
					if (c == '*' && i < line.length() - 1)
						if (line.charAt(i + 1) == '#') {
							++i;
							insideBlockComment = false;
						}
				} // end of block comment
				else {
					if (c == '{')
						++codeBlockDepth;
					else if (c == '}')
						--codeBlockDepth;
					if (c == '"') {
						// search for matching quote character TODO escapes
						int j = i + 1;
						while (j < line.length() && line.charAt(j) != '"') {
							if (line.charAt(j) == '\\' && j < line.length() - 1) {
								// delete backslash character at j
								line = line.substring(0, j).concat(line.substring(j + 1));
								switch (line.charAt(j)) {
								// skip escaped quotes
								case '"':
									line = line.substring(0, j).concat(System.lineSeparator())
											.concat(line.substring(j + 1));
								case 'n':
									line = line.substring(0, j).concat(System.lineSeparator())
											.concat(line.substring(j + 1));
								case 't':
									line = line.substring(0, j).concat("\t").concat(line.substring(j + 1));
								}
							}
							++j;
						}
						// this is a syntax error of unclosed string literal
						if (j == line.length())
							throw CompilerException.fromFormatMessage(line, i + 1, lineIdx, "Syntax",
									"No closing '\"' for string literal.");
						// skip this section
						newCode.append(line.substring(i, j + 1));
						i = j;
					} else if (c == '#') {
						if (i < line.length() - 1)
							if (line.charAt(i + 1) == '*') {
								++i;
								// we found a block comment
								insideBlockComment = true;
							} else {
								// skip the single-line comment
								i = line.length();
							}
					} else {
						newCode.append(c);
					}
				} // end of non-block comment
			} // end of single line
			newCode.append(System.lineSeparator());
		} // end of scan

		scanner.close();
		if (codeBlockDepth > 0) {
			throw CompilerException.fromCurrentPosition(newCode.toString(), newCode.lastIndexOf("{"), "Syntax",
					"Unclosed code block");
		} else if (codeBlockDepth < 0) {
			throw CompilerException.fromCurrentPosition(newCode.toString(), newCode.lastIndexOf("}"), "Syntax",
					"Too many closing '}'");
		}
		return newCode.toString();
	}

	/**
	 * Searches the String for two matching (open&close, like parenthesis) character
	 * pairs and returns the index after the closing character. Also keeps track of
	 * nesting levels.
	 * 
	 * @param toSearch     The String through which is searched.
	 * @param indexOfFirst The index where the opening character combination starts.
	 *                     The method will search for the closing character
	 *                     combination that matches with this combination.
	 * @param toMatchOpen  The character combination that denotes the opening or
	 *                     introduction of a new nesting level.
	 * @param toMatchClose The character combination that denotes the closing or
	 *                     finalization of a nesting level.
	 * @return The index directly after the closing character combination that
	 *         matches the given opening character combination at the given index.
	 *         If an error occurs, such as not finding matching characters or
	 *         nesting level errors, the index returned is -1.
	 */
	public static int indexOfMatching(final String toSearch, final int indexOfFirst, final String toMatchOpen,
			final String toMatchClose) {
		final Matcher openingMatcher = Pattern.compile(Pattern.quote(toMatchOpen)).matcher(toSearch);
		final Matcher closingMatcher = Pattern.compile(Pattern.quote(toMatchClose)).matcher(toSearch);
		boolean openingAvailable = openingMatcher.find(indexOfFirst),
				closingAvailable = closingMatcher.find(indexOfFirst);
		if (!openingAvailable || !closingAvailable)
			return -1;
		int openingStart = openingMatcher.start(), closingStart = closingMatcher.start();
		int indentationLevel = 0;
		int lastValidClosing;

		do {
			lastValidClosing = closingMatcher.end();
			// only do this if there was an opening available in the last search.
			// if not, then it is useless to try further.
			if (openingStart < closingStart && openingAvailable) {
				// the opening occurs first, so advance it
				++indentationLevel;
				openingAvailable = openingMatcher.find();
				if (openingAvailable)
					openingStart = openingMatcher.start();
				// set the start of the next opening to a high value so the second clause is
				// definitely triggered next time
				else
					openingStart = Integer.MAX_VALUE;
			} else
			// only do this if there was a closing available in the last search.
			// if not, then it is useless to try further.
			if (closingAvailable) {
				// the closing occurs first, so advance it
				--indentationLevel;
				closingAvailable = closingMatcher.find();
				if (closingAvailable)
					closingStart = closingMatcher.start();
				// set the start of the next closing to a low value so the first clause is
				// definitely triggered next time
				else
					closingStart = Integer.MIN_VALUE;
			}
		} while ((openingAvailable || closingAvailable) && indentationLevel > 0);
		if (indentationLevel != 0)
			return -1;
		return lastValidClosing;
	}

	/**
	 * Utility to format a string for debug output of a stack.
	 */
	public static String stackToDebugString(final Deque<Stackable> stack) {
		return "┌─" + line66.substring(0, 37) + "─┐" + System.lineSeparator()
				+ stack.stream().collect(() -> new StringBuilder(),
						(str, elmt) -> str.append(
								String.format("│%38s │%n├─" + line66.substring(0, 37) + "─┤%n", elmt.toString(), " ")),
						(e1, e2) -> e1.append(e2)).toString();
	}

	/**
	 * Responsible for tokenizing the code and moving around when code blocks and
	 * functions affect control flow.
	 */
	private Tokenizer tokenizer = Tokenizer.fromSourceCode("");

	/**
	 * Simple delegate for an action to be taken when a certain primitive token (PT)
	 * is encountered.
	 */
	@FunctionalInterface
	private static interface PTAction {
		/**
		 * Execute this PTAction.
		 * 
		 * @param self The interpreter that asked for the action.
		 */
		public void execute(Interpreter self);
	}
	/**
	 * The most important variable. Defines a mapping from primitive tokens (PTs) to
	 * actions that correspond with them. Literals and code blocks are handled
	 * differently.
	 */
	// higher load factor can be used b/c of small number of entries where
	// collisions are unlikely
	private static Map<String, PTAction> ptActions = new Hashtable<>(30, 0.8f);
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//// PT ACTION DEFINITION BEGIN
	static {
		ptActions.put("+", self -> {
			// pop 2, add, push
			final Stackable paramright = self.stack.pop(), paramleft = self.stack.pop();
			final var result = self.executeFunction(paramright, paramleft, Operator.add, "+");
			self.stack.push(result);
		});
		ptActions.put("-", self -> {
			// pop 2, subtract, push
			final Stackable paramright = self.stack.pop(), paramleft = self.stack.pop();
			final var result = self.executeFunction(paramleft, paramright, Operator.subtract, "-");
			self.stack.push(result);
		});
		ptActions.put("*", self -> {
			// pop 2, multiply, push
			final Stackable paramright = self.stack.pop(), paramleft = self.stack.pop();
			final var result = self.executeFunction(paramleft, paramright, Operator.multiply, "*");
			self.stack.push(result);
		});
		ptActions.put("/", self -> {
			// pop 2, divide, push
			final Stackable paramright = self.stack.pop(), paramleft = self.stack.pop();
			final var result = self.executeFunction(paramleft, paramright, Operator.divide, "/");
			self.stack.push(result);
		});
		// pop and discard
		ptActions.put("pop", self -> self.stack.pop());
		// peek and push, thereby duplicate
		ptActions.put("dup", self -> {
			final var param = self.stack.peek();
			if (param instanceof Nametable)
				throw CompilerException.fromCurrentPosition(self.tokenizer, "StackAccess",
						"A nametable cannot be duplicated.");
			self.stack.push(param);
		});
		// debug commands that are effectively no-ops in terms of data and code
		ptActions.put("describes", self -> self.io.describeStack(self.stack));
		ptActions.put("describe", self -> self.io.debug(self.stack.peek().getDebugDisplay()));
		// i/o
		ptActions.put("write", self -> self.io.print(self.stack.pop().toOutputString()));
		ptActions.put("writeln", self -> self.io.println(self.stack.pop().toOutputString()));
		// define
		final Function<Function<Interpreter, Nametable>, PTAction> definer = scope -> self -> {
			// The scope lookup is only performed in this moment.
			// The scope retrieval function should leave the
			// actual identifier into the scope, whether actually provided by the user or
			// not,
			// as the topmost element of the stack and the value directly below.
			final var targetScope = scope.apply(self);
			// pop value, define
			final var idS = self.stack.pop();
			final var valS = self.stack.pop();
			final var id = (Identifier) idS;
			targetScope.put(id, valS);
		};
		// TODO change this to define into FNT if there exists a definition there
		ptActions.put("def", definer.apply(self -> {
			final var idS = self.stack.pop();
			self.check(idS instanceof Identifier,
					() -> err("Type", "\"" + idS.toString() + "\" is not an identifier."));
			self.stack.push(idS);
			if (self.stack.namingScope().hasMapping((Identifier) idS))
				return self.stack.namingScope();
			return self.stack.localScope();
		}));
		ptActions.put("globaldef", definer.apply(self -> self.stack.globalNametable()));

		///// CALL OPERATOR /////
		ptActions.put(".", self -> {
			// start looking at the local scope
			var currentNametable = self.stack.localScope();
			// store the namespace string for future use
			var namespaceString = "";
			do {
				final var param = self.stack.pop();
				if (param instanceof Identifier) {
					final var topId = (Identifier) param;
					// look the identifier up in the current nametable
					final var reference = currentNametable.get(topId);
					if (reference instanceof Nametable) {
						// we found a namespace
						currentNametable = (Nametable) reference;
						namespaceString += reference.toString() + ".";
					} else if (reference instanceof Callable) {
						// we found the end of the chain
						var retval = ((Callable) reference).getCallProvider().call(self);
						if (retval != null) self.stack.push(retval);
						break;
					} else if (reference == null) {
						throw CompilerException.fromCurrentPosition(self.tokenizer, "Name",
								"Identifier " + param.toString() + " is not defined"
										+ (namespaceString.length() == 0 ? "" : (" in " + namespaceString)) + ".");
					}
				} else if (param instanceof Callable) {
					// when we have a callable, it can of course be called directly
					self.log.finer(f("@ CALL directly: %s", param));
					final var retval = ((Callable) param).getCallProvider().call(self);
					if (retval != null)
						self.stack.push(retval);
					break;
				} else {
					self.stack.push(param);
					throw CompilerException.fromCurrentPosition(self.tokenizer, "Type",
							param.toString() + " is not callable.");
				}
			} while (true);
		});
	}
	//// PT ACTION DEFINITION END
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	// I/O
	private IOInterface io;

	// all da memory
	private Stack stack;

	/**
	 * <a href=
	 * "https://www.reddit.com/r/ProgrammerHumor/comments/auz30h/when_you_make_documentation_for_a_settergetter/?utm_source=share&utm_medium=web2x">...</a>
	 */
	public String getCode() {
		return tokenizer.getCode();
	}

	/**
	 * <a href=
	 * "https://www.reddit.com/r/ProgrammerHumor/comments/auz30h/when_you_make_documentation_for_a_settergetter/?utm_source=share&utm_medium=web2x">...</a>
	 */
	public IOInterface getIO() {
		return io;
	}

	// ------------------------------------------------------------------------------------------------------
	// EXECUTION

	@SuppressWarnings("unused")
	private Stackable executeFunction(final Stackable arg1, final Stackable arg2, final Operator function) {
		return executeFunction(arg1, arg2, function, "");
	}

	private Stackable executeFunction(final Stackable arg1, final Stackable arg2, final Operator function,
			final String funcName) throws CompilerException {
		try {
			return function.call(arg1, arg2);
		} catch (final CompilerException e) {
			throw CompilerException.fromIncomplete(tokenizer, e);
		} catch (final ClassCastException e) {
			throw CompilerException.fromCurrentPosition(this.tokenizer, "Type",
					String.format("Cannot perform function '%s' on arguments %s and %s: wrong type.", funcName,
							arg1.toString(), arg2.toString()));
		}
	}

	/**
	 * Does one execution step. Will do nothing if the end of the source code is
	 * reached.
	 * 
	 * @throws CompilerException If something goes wrong at runtime.
	 */
	@SuppressWarnings("deprecation")
	public Interpreter executeOnce() throws CompilerException {
		log.entering(this.getClass().getCanonicalName(), "executeOnce");
		final String token = tokenizer.next();
		if (token.length() == 0)
			return this;

		try {
			if (ptActions.containsKey(token)) {
				final var toExec = ptActions.get(token);
				log.finest(() -> f("PT-EXEC %30s :: %10s @ %4d", token, toExec, tokenizer.getState().start));
				toExec.execute(this);
			} else {
				if (intPattern.matcher(token).matches()) {
					log.finest(() -> f("LITERAL INT %30s @ %4d", token, tokenizer.getState().start));
					try {
						final Primitive<Long> literal = Primitive.createInteger(token.toLowerCase());
						stack.push(literal);
					} catch (final CompilerException e) {
						throw CompilerException.fromCurrentPosition(this.tokenizer, "Syntax",
								f("No integer literal found in \"%s\".", token));
					}
				} else if (doublePattern.matcher(token).matches()) {
					log.finest(() -> f("LITERAL DOUBLE %30s @ %4d", token, tokenizer.getState().start));
					try {
						final Primitive<Double> literal = new Primitive<Double>(
								Double.parseDouble(token.toLowerCase()));
						stack.push(literal);
					} catch (final NumberFormatException e) {
						throw CompilerException.fromCurrentPosition(this.tokenizer, "Syntax",
								f("No double literal found in \"%s\".", token));
					}
				} else if (boolPattern.matcher(token).matches()) {
					log.finest(() -> f("LITERAL BOOL %30s @ %4d", token, tokenizer.getState().start));
					final Primitive<Boolean> literal = Primitive.createBoolean(token);
					stack.push(literal);
				} else if (stringPattern.matcher(token).matches()) {
					log.finest(() -> f("LITERAL STRING %30s @ %4d", token, tokenizer.getState().start));
					stack.push(new Primitive<>(token.substring(1, token.length() - 1)));
				} else if (codeBlockStartPattern.matcher(token).matches()) {
					final var endPos = Interpreter.indexOfMatching(tokenizer.getCode(), tokenizer.start(), "{", "}")
							- 1;
					this.check(endPos >= 0, () -> new SimpleEntry<>("Syntax", "Unclosed code block"));
					final var cb = new CodeBlock(tokenizer.getState().end, endPos, tokenizer.getCode());
					this.stack.push(cb);
					var newstate = tokenizer.getState();
					newstate.start =  newstate.end = endPos;
					tokenizer = Tokenizer.fromState(newstate);
					log.finest(() -> f("CODE BLOCK %30s @ %4d", cb.getDebugDisplay(), tokenizer.getState().start));
					tokenizer.next();
				} else if (identifierPattern.matcher(token).matches()) {
					log.finest(() -> f("IDENTIFIER %30s @ %4d", token, tokenizer.getState().start));
					stack.push(new Identifier(token));
				} else {
					// oh no, you have input invalid characters!
					throw CompilerException.fromCurrentPosition(this.tokenizer, "Syntax",
							f("Unexpected characters \"%s\".", token));
				}
			}
		} catch (final CompilerException e) {
			if (e.isInfoPresent())
				throw e;
			else {
				throw CompilerException.fromIncomplete(tokenizer, e);
			}
		}
		log.exiting(this.getClass().getCanonicalName(), "executeOnce");
		log.finest(() -> "S:\n" + Interpreter.stackToDebugString(stack) + "\nNT:\n"
				+ stack.globalNametable().getDebugDisplay());
		return this;
	}

	// ------------------------------------------------------------------------------------------------------
	// ITERATION AND EXECUTION METHODS

	/**
	 * Returns whether the interpreter can execute further instructions.
	 * 
	 * @return whether the interpreter can execute further instructions.
	 */
	public boolean canExecute() {
		return tokenizer.hasNext();
	}

	/**
	 * Executes this interpreter until it either encounters an exception or the
	 * tokenizer reaches the end of its searching range.<br>
	 * <br>
	 * Note that this method might not return.<br>
	 * Subclasses may override this method to provide optimized continuous
	 * execution.
	 * 
	 * @return this Interpreter
	 * @throws CompilerException
	 */
	public Interpreter executeForever() throws CompilerException {
		while (this.canExecute()) {
			this.executeOnce();
		}
		return this;
	}

	@Override
	public Iterator<Interpreter> iterator() {
		return this;
	}

	@Override
	public boolean hasNext() {
		return this.canExecute();
	}

	@Override
	public Interpreter next() {
		try {
			return this.executeOnce();
		} catch (final CompilerException e) {
			throw new NoSuchElementException("Iteration failed because a compiler exception was encountered.");
		}
	}

	/**
	 * Creates a new instance of this interpreter. Especially useful for the
	 * specialized interpreter subclasses.
	 */
	public Interpreter instantiateSelf() {
		try {
			return this.getClass().getConstructor().newInstance();
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | NoSuchMethodException
				| SecurityException | InvocationTargetException e) {
			// this should not happen
			throw new RuntimeException("VERY DANGEROUS EXCEPTION", e);
		}
	}

	/** Resets this interpreter by deleting and reinitializing all state. */
	public Interpreter reset() {
		// make the stack
		stack = new Stack();
		// make the global nametable
		final Nametable globalNametable = new Nametable();
		stack.push(globalNametable);
		return this;
	}

	/**
	 * Sets the code of this interpreter. Also prepares the code and regex utilities
	 * for execution; this is why a compilation error can be thrown here.
	 * 
	 * @param code The SOF code to be used by this interpreter.
	 * @throws CompilerException If something during the code preprocessing stages
	 *                           goes wrong.
	 */
	public Interpreter setCode(final String code) throws CompilerException {
		this.tokenizer = Tokenizer.fromSourceCode(code);
		return this;
	}

	/**
	 * Appends a line of code to the interpreter's current code. Useful for
	 * line-by-line source code scanning and interactive sessions.
	 * 
	 * @param string The line of code to be appended
	 * @return this interpreter
	 */
	public Interpreter appendLine(final String string) throws CompilerException {
		this.tokenizer.appendCode(string);
		return this;
	}

	// ------------------------------------------------------------------------------------
	// UTILITY
	/**
	 * Checks whether the given condition holds true; if <b>not</b>, throws a
	 * CompilerException with the given name and description at the current location
	 * 
	 * @param b            Check to validate.
	 * @param errorCreator Function that creates a tuple with (name, description)
	 *                     format.
	 * @throws CompilerException If the check fails.
	 */
	private void check(final boolean b, final Supplier<SimpleEntry<String, String>> errorCreator)
			throws CompilerException {
		if (!b) {
			final var errortuple = errorCreator.get();
			throw CompilerException.fromCurrentPosition(this.tokenizer, errortuple.getKey(), errortuple.getValue());
		}
	}

	private static String f(final String s, final Object... args) {
		return String.format(s, args);
	}

	private static SimpleEntry<String, String> err(final String a, final String b) {
		return new SimpleEntry<String, String>(a, b);
	}
}
