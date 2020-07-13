package klfr.sof;

// ALL THE STANDARD LIBRARY
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static klfr.Tuple.t;

import klfr.Tuple;
import klfr.sof.lang.*;
import klfr.sof.lang.Stackable.DebugStringExtensiveness;

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
public class Interpreter implements Iterator<Interpreter>, Iterable<Interpreter>, Serializable {
	private static final long serialVersionUID = 1L;
	protected static final Logger log = Logger.getLogger(Interpreter.class.getCanonicalName());
	/** Version of the interpreter. */
	public static final String VERSION = "0.1";

	// #region Nested classes

	/**
	 * Access Interpreter internals through this pseudo-class.
	 * 
	 * @deprecated This pseudo-class accesses the Interpreter internals. Its usage
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

		/**
		 * Sets the execution position to the specified index in the code.
		 */
		@Deprecated
		public void setExecutionPos(final int index) {
			final var state = tokenizer.getState();
			state.end = state.start = index;
			tokenizer.setState(state);
		}
	}

	/**
	 * Simple functional interface for an action with side-effects that operates on
	 * an interpreter.
	 */
	@FunctionalInterface
	public static interface InterpreterAction {
		/**
		 * Execute this PTAction.
		 * 
		 * @param self The interpreter that asked for the action.
		 */
		public void execute(Interpreter self) throws CompilerException;
	}

	// #endregion

	// #region Utility

	private static String f(final String s, final Object... args) {
		return String.format(s, args);
	}

	/** Convenience constant for the 66-character line ─ */
	public static final String line66 = String.format("%66s", " ").replace(" ", "─");

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

	// #endregion

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// #region PATTERNS
	/**
	 * Pattern for integer number literals. Group 1 matches the entire number if not
	 * the simple '0' literal, group 2 matches the sign, if present. Group 3 matches
	 * the base prefix, if present.
	 */
	public static final Pattern intPattern = Pattern.compile("((\\+|\\-)?(0[bhxod])?[0-9a-fA-F]+)|0");
	/**
	 * Pattern for floating point number literals (represented with 64-bit "double"
	 * values internally). Group 1 matches the sign, if present, group 2 matches the
	 * integer section of the number, group 3 matches the fractional section of the
	 * number. Group 4 matches the scientific exponent, if present.
	 */
	public static final Pattern doublePattern = Pattern
			.compile("(\\+|\\-)?(?:([0-9]+)\\.([0-9]+)([eE][\\-\\+][0-9]+)?)");
	/**
	 * String pattern. Matches a starting quote character followed by any number of
	 * arbitrary characters followed by an ending quote character. The quote escape
	 * works in the way that a {@code \"} sequence is accepted as an "arbitrary
	 * character" (while single quote is, of course, not) and the ending quote
	 * character cannot be preceded by a {@code \} (may otherwise cause issues on
	 * end of string). This pattern does not match other escape sequences, see
	 * {@link Interpreter#escapeSequencePattern}.
	 */
	public static final Pattern stringPattern = Pattern.compile("\"((?:[^\"]|(\\\\\"))*?(?<!\\\\))\"");
	/**
	 * String escape sequence pattern. Matches the entire escape sequence except the
	 * leading backslash as group 1 and the unicode code point for {@code \ u}
	 * escapes as group 2. Does not match the escaped quote, which is not treated by
	 * the "preprocessor".
	 */
	public static final Pattern escapeSequencePattern = Pattern.compile("\\\\(f|t|(?:u([0-9a-fA-F]{4})))");
	/**
	 * Boolean literal pattern. Matches "true" and "false", capitalized as well.
	 */
	public static final Pattern boolPattern = Pattern.compile("True|False|true|false");
	/**
	 * (Basic) Token pattern. Matches all contiguous non-space text as well as the
	 * pattern for string literals.
	 */
	public static final Pattern tokenPattern = Pattern.compile("(" + stringPattern.pattern() + ")|(\\S+)");// \\b{g}
	/**
	 * Identifier pattern. An identifier is any unicode letter possibly followed by
	 * more unicode letters, numbers or the punctuation {@code : _ '} Note that
	 * using <code>\p{L}</code> allows for inter-language identifiers; one could
	 * write variable names completely with Chinese logographs, for example.
	 */
	public static final Pattern identifierPattern = Pattern.compile("\\p{L}[\\p{L}0-9_'\\:]*");
	/**
	 * The start of a code block; the single character <code>{</code>. Positive
	 * lookbehinds and lookaheads are used to ensure that the character is either
	 * before the end of input or any number of whitespace; and either after the
	 * beginning of input or any number of whitespace.
	 */
	public static final Pattern codeBlockStartPattern = Pattern.compile("(?<=^|\\s+)\\{(?=$|\\s+)");
	/**
	 * The end of a code block; the single character <code>}</code>. Positive
	 * lookbehinds and lookaheads are used to ensure that the character is either
	 * before the end of input or any number of whitespace; and either after the
	 * beginning of input or any number of whitespace.
	 */
	public static final Pattern codeBlockEndPattern = Pattern.compile("(?<=^|\\s+)\\}(?=$|\\s+)");

	/**
	 * Single-line comment; starting with a # and ending with a newline.
	 */
	public static final Pattern commentOneLinePattern = Pattern.compile("\\#.*?$", Pattern.MULTILINE);

	/**
	 * Multi-line comment; starting with a #* and ending with *#
	 */
	public static final Pattern commentMultilinePattern = Pattern.compile("\\#\\*.*?\\*\\#", Pattern.DOTALL);

	/**
	 * The start of a line; pattern created by compiling the start of string flag
	 * "^" in MULTILINE mode.
	 */
	public static final Pattern nlPat = Pattern.compile("^", Pattern.MULTILINE);

	// #endregion Patterns
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Defines a mapping from primitive tokens (PTs) to actions that correspond with
	 * them.
	 */
	// higher load factor can be used b/c of small number of entries where
	// collisions are unlikely
	private static Map<String, InterpreterAction> ptActions = new TreeMap<>();
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// #region PRIMITIVE TOKEN ACTIONS
	static {
		/// OPERATIONS
		ptActions.put("+", self -> self.doCall(BuiltinPTs.add));
		ptActions.put("-", self -> self.doCall(BuiltinPTs.subtract));
		ptActions.put("*", self -> self.doCall(BuiltinPTs.multiply));
		ptActions.put("/", self -> self.doCall(BuiltinPTs.divide));
		ptActions.put(">", self -> self.doCall(BuiltinPTs.greaterThan));
		ptActions.put("<", self -> self.doCall(BuiltinPTs.lessThan));
		ptActions.put(">=", self -> self.doCall(BuiltinPTs.greaterEqualThan));
		ptActions.put("<=", self -> self.doCall(BuiltinPTs.lessEqualThan));
		ptActions.put("=", self -> self.doCall(BuiltinPTs.equals));
		ptActions.put("/=", self -> self.doCall(BuiltinPTs.notEquals));
		// pop and discard
		ptActions.put("pop", self -> self.stack.pop());
		// peek and push, thereby duplicate
		ptActions.put("dup", self -> {
			final var param = self.stack.peek();
			if (param instanceof Nametable)
				throw CompilerException.fromCurrentPosition(self.tokenizer, "StackAccess",
						"A nametable cannot be duplicated.");
			// IMPORTANT: No copy() call is made here. The duplicate is the same reference!
			self.stack.push(param);
		});
		ptActions.put("swap", self -> {
			var eltop = self.stack.pop();
			var elbot = self.stack.pop();
			self.stack.push(eltop);
			self.stack.push(elbot);
		});
		// debug commands that are effectively no-ops in terms of data and code
		ptActions.put("describes", self -> self.io.describeStack(self.stack));
		ptActions.put("describe", self -> self.io.debug(self.stack.peek().toDebugString(DebugStringExtensiveness.Full)));
		// i/o
		ptActions.put("write", self -> self.io.print(self.stack.pop().print()));
		ptActions.put("writeln", self -> self.io.println(self.stack.pop().print()));
		// define
		final Function<Function<Interpreter, Nametable>, InterpreterAction> definer = scope -> self -> {
			// The scope lookup is only performed in this moment. The scope retrieval
			// function should leave the identifier, whether actually provided by the user
			// or not, as the topmost element of the stack and the value directly below.
			final var targetScope = scope.apply(self);
			// pop value, define
			final var id = self.stack.popTyped(Identifier.class);
			final var valS = self.stack.pop();
			targetScope.put(id, valS);
		};
		ptActions.put("def", definer.apply(self -> {
			final var idS = self.stack.popTyped(Identifier.class);
			self.stack.push(idS);
			if (self.stack.namingScope().hasMapping(idS))
				return self.stack.namingScope();
			return self.stack.localScope();
		}));
		ptActions.put("globaldef", definer.apply(self -> self.stack.namingScope()));

		ptActions.put("if", self -> {
			final var cond = self.stack.popTyped(BoolPrimitive.class);
			final var callable = self.stack.popTyped(Callable.class);
			if (cond.value()) {
				self.doCall(callable);
			}
		});

		ptActions.put("ifelse", self -> {
			final var elseCallable = self.stack.popTyped(Callable.class);
			final var cond = self.stack.popTyped(BoolPrimitive.class);
			final var callable = self.stack.popTyped(Callable.class);
			self.doCall(cond.value() ? callable : elseCallable);
		});

		ptActions.put("switch", self -> {
			// first argument is the default action callable
			final var defaultCallable = self.stack.popTyped(Callable.class);
			// loop to throw or switch end marker
			while (true) {
				// get case and corresponding body
				Callable _case = self.stack.popTyped(Callable.class), body = self.stack.popTyped(Callable.class);
				// execute case
				self.doCall(_case);
				final var result = self.stack.popTyped(BoolPrimitive.class);
				// ... and check if successful; if so, exit
				if (result.value()) {
					self.doCall(body);
					// remove elements until identifier "switch"
					var elt = self.stack.pop();
					while (!(elt instanceof Identifier && ((Identifier) elt).getValue().equals("switch::")))
						elt = self.stack.pop();
					break;
				} else {
					final var elt = self.stack.pop();
					if (elt instanceof Identifier && ((Identifier) elt).getValue().equals("switch::")) {
						// switch end was reached without executing any case: execute default callable
						self.doCall(defaultCallable);
						break;
					} else {
						// just another pair of case and body; do that in the next loop
						self.stack.push(elt);
					}
				}
			}
		});
		ptActions.put("while", self -> {
			final var condCallable = self.stack.popTyped(Callable.class);
			final var bodyCallable = self.stack.popTyped(Callable.class);
			while (true) {
				self.doCall(condCallable);
				var preContinue = self.stack.popTyped(BoolPrimitive.class);
				if (preContinue.value())
					self.doCall(bodyCallable);
			}
		});

		///// CALL OPERATOR /////
		ptActions.put(".", self -> {
			final var param = self.stack.popTyped(Callable.class);
			log.finer(f("@ CALL: %s", param));
			self.doCall(param);
		});

		// double call operator; convenience for function and method invocations
		ptActions.put(":", self -> {
			final var param = self.stack.popTyped(Callable.class);
			log.finer(f("@ DOUBLE CALL: %s", param));
			final var retval = param.getCallProvider().call(self);
			// try to call again
			if (retval instanceof Callable) {
				self.doCall((Callable) retval);
			} else {
				if (retval == null)
					throw CompilerException.fromCurrentPosition(self.tokenizer, "Call",
							"Cannot complete double-call operator \":\" : First call didn't return anything.");
				else
					throw CompilerException.fromCurrentPosition(self.tokenizer, "Call",
							"Cannot complete double-call operator \":\" : First call returned non-Callable \"" + retval.print()
									+ "\".");
			}
		});
	}
	// #endregion PT actions
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	// #region TOKEN HANDLERS

	/**
	 * Token handler for all primitive, i.e. immediate-action tokens.
	 */
	public static Optional<InterpreterAction> primitiveTokenHandler(String token) {
		if (ptActions.containsKey(token)) {
			return Optional.of(self -> {
				final var toExec = ptActions.get(token);
				log.finest(() -> f("PT-EXEC %30s :: %10s @ %4d", token, toExec, self.tokenizer.getState().start));
				toExec.execute(self);
			});
		}
		return Optional.empty();
	}

	/**
	 * Token handler for all kinds of literals.
	 */
	public static Optional<InterpreterAction> literalTokenHandler(String token) {
		if (intPattern.matcher(token).matches()) {
			return Optional.of(self -> {
				log.finest(() -> f("LITERAL INT %30s @ %4d", token, self.tokenizer.getState().start));
				try {
					final IntPrimitive literal = IntPrimitive.createIntegerFromString(token.toLowerCase());
					self.stack.push(literal);
				} catch (final CompilerException e) {
					throw CompilerException.fromCurrentPosition(self.tokenizer, "Syntax",
							f("No integer literal found in \"%s\".", token));
				}
			});
		}
		if (doublePattern.matcher(token).matches()) {
			return Optional.of(self -> {
				log.finest(() -> f("LITERAL DOUBLE %30s @ %4d", token, self.tokenizer.getState().start));
				try {
					final FloatPrimitive literal = FloatPrimitive.createFloatFromString(token);
					self.stack.push(literal);
				} catch (final NumberFormatException e) {
					throw CompilerException.fromCurrentPosition(self.tokenizer, "Syntax",
							f("No double literal found in \"%s\".", token));
				}
			});
		}
		if (boolPattern.matcher(token).matches()) {
			return Optional.of(self -> {
				log.finest(() -> f("LITERAL BOOL %30s @ %4d", token, self.tokenizer.getState().start));
				final BoolPrimitive literal = BoolPrimitive.createBoolFromString(token);
				self.stack.push(literal);
			});
		}
		if (stringPattern.matcher(token).matches()) {
			return Optional.of(self -> {
				log.finest(() -> f("LITERAL STRING %30s @ %4d", token, self.tokenizer.getState().start));
				self.stack.push(StringPrimitive.createStringPrimitive(Preprocessor.preprocessSofString(token)));
			});
		}
		return Optional.empty();
	}

	public static Optional<InterpreterAction> codeBlockTokenHandler(String token) {
		if (codeBlockStartPattern.matcher(token).matches()) {
			return Optional.of(self -> {
				final var endPos = Preprocessor.indexOfMatching(self.tokenizer.getCode(), self.tokenizer.start(),
						codeBlockStartPattern, codeBlockEndPattern) - 1;
				self.check(endPos >= 0, () -> t("Syntax", "Unclosed code block"));
				final var cb = new CodeBlock(self.tokenizer.getState().end, endPos, self.tokenizer.getCode());
				self.stack.push(cb);

				log.finest(() -> f("CODE BLOCK %30s @ %4d", cb.toDebugString(DebugStringExtensiveness.Full),
						self.tokenizer.getState().start));

				// setup the tokenizer just before the curly brace...
				self.internal.setExecutionPos(endPos);
				// ...and skip it
				self.tokenizer.next();
			});
		}
		return Optional.empty();
	}

	public static Optional<InterpreterAction> identifierTokenHandler(String token) {
		if (identifierPattern.matcher(token).matches()) {
			return Optional.of(self -> {
				log.finest(() -> f("IDENTIFIER %30s @ %4d", token, self.tokenizer.getState().start));
				self.stack.push(new Identifier(token));
			});
		}
		return Optional.empty();
	}

	// #endregion TOKEN HANDLERS

	/**
	 * Constructs an uninitialized reset interpreter. Use methods such as
	 * {@link Interpreter#setCode(String)}, {@link Interpreter#appendLine(String)}
	 * to initialize the interpreter to your desired state.
	 */
	public Interpreter() {
		this.reset();
		// the last handler is the first to be invoked.
		tokenHandlers.add(Interpreter::primitiveTokenHandler);
		tokenHandlers.add(Interpreter::literalTokenHandler);
		tokenHandlers.add(Interpreter::codeBlockTokenHandler);
		tokenHandlers.add(Interpreter::identifierTokenHandler);
	}

	/**
	 * Responsible for tokenizing the code and moving around when code blocks and
	 * functions affect control flow.
	 */
	protected Tokenizer tokenizer = Tokenizer.fromSourceCode("");

	// I/O
	protected IOInterface io;

	// all da memory
	protected Stack stack;

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// #region EXECUTION

	/**
	 * The list of token handlers that the interpreter uses to resolve a given token
	 * to an action. This is at the core of the Interpreter extension mechanism and
	 * facilitates easy future modifications. The most important handlers present in
	 * this list by default is the primitive token handler, the handlers for each
	 * literal token type, and the code block handler. Token handlers are always
	 * considered in the inverse order that they appear in the list. Adding a token
	 * with the {@link Interpreter#registerTokenHandler(TokenHandler)} interface
	 * will append it to the end of the list, therefore making it the first invoked
	 * handler.
	 */
	private List<TokenHandler> tokenHandlers = new ArrayList<>(20);

	/**
	 * Register a new token handler that will be used by this interpreter to handle
	 * tokens it recieves from the SOF source code. Token handlers added through
	 * this method are placed at the top of the handler hierarchy, meaning that they
	 * are the first handler to be recieving the token. This means that generic
	 * (possibly regex-based) handlers should be added first, followed by more and
	 * more specific handlers that will trigger on less and less tokens. For
	 * example, in the default token handler hierarchy, the primitive token
	 * handlers, which only match one exact token each, are at the very top of the
	 * hierarchy, i.e. added last; while the identifier token handler, which matches
	 * a lot of tokens including some literals and primitive tokens, is at the very
	 * bottom of the hierarchy, making it the last handler to ever be invoked.
	 * 
	 * @param newHandler A new token handler to be registered in this interpreter.
	 *                   It is possible to add one handler multiple times, which
	 *                   will have no effect other than effectively increasing the
	 *                   importance of the token handler in the hierarchy.
	 * @return self
	 */
	public Interpreter registerTokenHandler(TokenHandler newHandler) {
		log.config(() -> f("New token handler registered: %s", newHandler));
		this.tokenHandlers.add(0, newHandler);
		return this;
	}

	/**
	 * Does one execution step. Will do nothing if the end of the source code is
	 * reached.
	 * 
	 * @throws CompilerException If something goes wrong at runtime.
	 * @return self
	 */
	public Interpreter executeOnce() throws CompilerException {
		log.entering(this.getClass().getCanonicalName(), "executeOnce");
		final String token = tokenizer.next();
		if (token.length() == 0)
			return this;

		try {
			// Use stream processing to find the first token handler that can handle the
			// current token. This is not inefficient, as the terminal operation 'findFirst'
			// only executes the intermediate maps and filters when required. As soon as the
			// first applicable token handler is found, all others are discarded.
			final var applicableHandler = this.tokenHandlers.stream()
					// handle the token
					.map(th -> th.handle(token))
					// filter out all handlers that couldn't handle the token
					.filter(op -> op.isPresent())
					// map to remove the optional from the handle operation (checked above)
					.map(op -> op.get())
					// find the first token handler, only processes as many handlers as needed
					.findFirst();
			if (applicableHandler.isPresent()) {
				applicableHandler.get().execute(this);
			} else {
				// oh no, you have input invalid characters!
				throw CompilerException.fromCurrentPosition(this.tokenizer, "Syntax",
						f("Unexpected characters \"%s\".", token));
			}
		} catch (final CompilerException e) {
			if (e.isInfoPresent())
				throw e;
			else {
				throw CompilerException.fromIncomplete(tokenizer, e);
			}
		}
		log.exiting(this.getClass().getCanonicalName(), "executeOnce");
		log.finest(() -> "S:\n" + stack.toStringExtended() + "\nNT:\n"
				+ stack.globalNametable().toDebugString(DebugStringExtensiveness.Full));
		return this;
	}

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

	/**
	 * Checks whether the given condition holds true; if <b>not</b>, throws a
	 * CompilerException with the given name and description at the current location
	 * 
	 * @param b            Check to validate.
	 * @param errorCreator Function that creates a tuple with (name, description)
	 *                     format.
	 * @throws CompilerException If the check fails.
	 */
	private void check(final boolean b, final Supplier<Tuple<String, String>> errorCreator) throws CompilerException {
		if (!b) {
			final var errortuple = errorCreator.get();
			throw CompilerException.fromCurrentPosition(this.tokenizer, errortuple.getLeft(), errortuple.getRight());
		}
	}

	/**
	 * Helper function to execute call and push return value of callable to stack if
	 * possible.
	 */
	private void doCall(final Callable reference) {
		final var retval = reference.getCallProvider().call(this);
		if (retval != null)
			this.stack.push(retval);
	}

	// #endregion Execution
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	// #region Iterable and utility methods

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

	// #endregion

	// #region API and state modification

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
	 * Sets the code of this interpreter.
	 * 
	 * @param code The SOF code to be used by this interpreter.
	 */
	public Interpreter setCode(final String code) {
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
	public Interpreter appendLine(final String string) {
		this.tokenizer.appendCode(string);
		return this;
	}

	// #endregion

}
