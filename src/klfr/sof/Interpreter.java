package klfr.sof;

// ALL THE STANDARD LIBRARY
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Deque;
import java.util.Scanner;
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
 * @author klfr
 * @version 0.1
 */
public class Interpreter {
	public static final String VERSION = "0.1";

	/** Convenience constant for the 38-character line ─ */
	public static final String line38 = String.format("%38s", " ").replace(" ", "─");

	//// PATTERNS
	public static final Pattern	intPattern			= Pattern.compile("((\\+|\\-)?(0[bhxod])?[0-9a-fA-F]+)|0");
	public static final Pattern	doublePattern		= Pattern
			.compile("(\\+|\\-)?([0-9]+\\.[0-9]+([eE][\\-\\+][0-9]+)?)|0");
	public static final Pattern	stringPattern		= Pattern.compile("\"[^\"]*\"");
	public static final Pattern	boolPattern			= Pattern.compile("True|False|true|false");
	public static final Pattern	tokenPattern		= Pattern.compile("(" + stringPattern.pattern() + ")|(\\S+)");//\\b{g}
	/** The pattern to which identifiers must match to be valid */
	public static final Pattern	identifierPattern	= Pattern.compile("\\p{L}[\\p{L}0-9_']*");
	public static final Pattern	nlPat					= Pattern.compile("^", Pattern.MULTILINE);

	/**
	 * Cleans the code of comments.
	 * @throws CompilationError if block comments or string literals are not closed
	 * properly.
	 */
	public static String cleanCode(String code) throws CompilationError {
		StringBuilder newCode = new StringBuilder();
		@SuppressWarnings("resource")
		Scanner scanner = new Scanner(code);
		String line = "";
		boolean insideBlockComment = false;
		int lineIdx = 0;
		while (scanner.hasNextLine()) {
			line = scanner.nextLine();
			++lineIdx;
			char c;
			for (int i = 0; i < line.length(); ++i) {
				c = line.charAt(i);

				if (insideBlockComment) {
					//if we have the ending character
					if (c == '*' && i < line.length() - 1) if (line.charAt(i + 1) == '#') {
						++i;
						insideBlockComment = false;
					}
				} //end of block comment 
				else {
					if (c == '"') {
						//search for matching quote character TODO escapes
						int j = i + 1;
						while (j < line.length() && line.charAt(j) != '"')
							++j;
						//this is a syntax error of unclosed string literal
						if (j == line.length())
							throw new CompilationError(line, i + 1, lineIdx, "Syntax", "No closing '\"' for string literal.");
						//skip this section
						newCode.append(line.substring(i, j + 1));
						i = j;
					} else if (c == '#') {
						if (i < line.length() - 1) if (line.charAt(i + 1) == '*') {
							++i;
							//we found a block comment
							insideBlockComment = true;
						} else {
							//skip the single-line comment
							i = line.length();
						}
					} else {
						newCode.append(c);
					}
				} // end of non-block comment
			} //end of single line
			newCode.append(System.lineSeparator());
		} //end of scan

		scanner.close();
		return newCode.toString();
	}

	/**
	 * Searches the String for two matching (open&close, like parenthesis) character
	 * pairs and returns the index after the closing character. Also keeps track of
	 * nesting levels.
	 * @param toSearch The String through which is searched.
	 * @param indexOfFirst The index where the opening character combination starts.
	 * The method will search for the closing character combination that matches
	 * with this combination.
	 * @param toMatchOpen The character combination that denotes the opening or
	 * introduction of a new nesting level.
	 * @param toMatchClose The character combination that denotes the closing or
	 * finalization of a nesting level.
	 * @return The index directly after the closing character combination that
	 * matches the given opening character combination at the given index. If an
	 * error occurs, such as not finding matching characters or nesting level
	 * errors, the index returned is -1.
	 */
	public static int indexOfMatching(String toSearch, int indexOfFirst, String toMatchOpen, String toMatchClose) {
		Matcher openingMatcher = Pattern.compile(Pattern.quote(toMatchOpen)).matcher(toSearch);
		Matcher closingMatcher = Pattern.compile(Pattern.quote(toMatchClose)).matcher(toSearch);
		boolean openingAvailable = openingMatcher.find(indexOfFirst),
				closingAvailable = closingMatcher.find(indexOfFirst);
		if (!openingAvailable || !closingAvailable) return -1;
		int openingStart = openingMatcher.start(),
				closingStart = closingMatcher.start();
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
				//set the start of the next opening to a high value so the second clause is definitely triggered next time
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
				//set the start of the next closing to a low value so the first clause is definitely triggered next time
				else
					closingStart = Integer.MIN_VALUE;
			}
		} while ((openingAvailable || closingAvailable) && indentationLevel > 0);
		if (indentationLevel != 0) return -1;
		return lastValidClosing;
	}

	/**
	 * Utility to format a string for debug output of a stack.
	 */
	public static String stackToDebugString(Deque<Stackable> stack) {
		return "┌─" + line38 + "─┐" + System.lineSeparator() +
				stack.stream().collect(() -> new StringBuilder(),
						(str, elmt) -> str.append(String.format("│ %38s │%n├─" + Interpreter.line38 + "─┤%n", elmt, " ")),
						(e1, e2) -> e1.append(e2)).toString();
	}

	private Tokenizer tokenizer = Tokenizer.fromSourceCode("");;

	public void pushState() {
		tokenizer.pushState();
	}

	public void popState() {
		tokenizer.popState();
	}

	/**
	 * Sets the execution region for the interpreter; it is recommended to push the
	 * interpreter state beforehand and popping it back afterwards.
	 * @param start start of the region, inclusive.
	 * @param end end of the region, exclusive.
	 */
	public void setRegion(int start, int end) {
		tokenizer.getMatcher().region(start, end);
	}

	// I/O
	private IOInterface io;

	//all da memory
	private Stack stack;

	/**
	 * Returns whether the interpreter can execute further instructions.
	 * @return whether the interpreter can execute further instructions.
	 */
	public boolean canExecute() {
		return tokenizer.hasNext();
	}

	@SuppressWarnings("unused")
	private Stackable executeFunction(Stackable arg1, Stackable arg2, Operator function) {
		return executeFunction(arg1, arg2, function, "");
	}

	private Stackable executeFunction(Stackable arg1, Stackable arg2, Operator function, String funcName)
			throws CompilationError {
		try {
			return function.call(arg1, arg2);
		} catch (CompilationError e) {
			throw makeException(e);
		} catch (ClassCastException e) {
			throw makeException("Type",
					String.format("Cannot perform function '%s' on arguments %s and %s: wrong type.",
							funcName, arg1.toString(), arg2.toString()));
		}
	}
	/**
	 * Does one execution step. Will do nothing if the end of the source code is
	 * reached.
	 * @throws CompilationError If something goes wrong at runtime.
	 */
	public Interpreter executeOnce() throws CompilationError {
		String token = tokenizer.next();
		if (token.length() == 0) return this;
		//System.out.println(token);

		// TODO Execution time!
		try {
			// BEHOLD THE SWITCH CASE OF DOOM!
			switch (token) {
			case "+":
				//pop 2, add, push
				Stackable param1 = stack.pop(), param2 = stack.pop();
				Stackable result = executeFunction(param1, param2, Operator.add, "+");
				stack.push(result);
				break;
			case "-":
				//pop 2, subtract, push
				Stackable paramright = stack.pop(), paramleft = stack.pop();
				result = executeFunction(paramleft, paramright, Operator.subtract, "-");
				stack.push(result);
				break;
			case "*":
				//pop 2, subtract, push
				paramright = stack.pop();
				paramleft = stack.pop();
				result = executeFunction(paramleft, paramright, Operator.multiply, "*");
				stack.push(result);
				break;
			case "/":
				//pop 2, subtract, push
				paramright = stack.pop();
				paramleft = stack.pop();
				result = executeFunction(paramleft, paramright, Operator.divide, "/");
				stack.push(result);
				break;

			case "def":
				Stackable idS = stack.pop();
				if (!(idS instanceof Identifier)) {
					throw makeException("Type", "\"" + idS.toString() + "\" is not an identifier.");
				}
				Identifier id = (Identifier) idS;
				Stackable valS = stack.pop();
				Nametable definer = stack.localScope();
				definer.put(id, valS);
				break;
			case "pop":
				stack.pop();
				break;
			case "dup":
				param1 = stack.peek();
				if (param1 instanceof Nametable)
					throw makeException("StackAccess", "A nametable cannot be duplicated.");
				stack.push(param1);

			case "describes":
				//debug command for outputting stack and nametable
				io.describeStack(stack);
				break;
			case "describe":
				io.debug(stack.peek().getDebugDisplay());
				break;
			case "write":
				Stackable toPrint = stack.pop();
				io.print(toPrint.toOutputString());
				break;
			case "writeln":
				toPrint = stack.pop();
				io.println(toPrint.toOutputString());
				break;

			case ".":
				//start looking at the local scope
				Nametable currentNametable = stack.localScope();
				//store the namespace string for future use
				String namespaceString = "";
				do {
					param1 = stack.pop();
					if (param1 instanceof Identifier) {
						Identifier topId = (Identifier) param1;
						//look the identifier up in the current nametable
						Stackable reference = currentNametable.get(topId);
						if (reference instanceof Nametable) {
							//we found a namespace
							currentNametable = (Nametable) reference;
							namespaceString += reference.toString() + ".";
						} else if (reference instanceof Callable) {
							// we found the end of the chain
							Stackable val = ((Callable) reference).getCallProvider().call(this);
							stack.push(val);
							break;
						} else if (reference == null) {
							throw makeException("Reference",
									"Identifier " + param1.toString() + " is not defined" +
											(namespaceString.length() == 0 ? "" : (" in " + namespaceString)) + ".");
						}
					} else {
						stack.push(param1);
						throw makeException("Type", param1.toString() + " is not callable.");
					}
				} while (true);
				break;

			//					// look the value up
			//					Identifier id = (Identifier) param1;
			//					//TODO traverse all available nametables from local to global
			//					Nametable nametable = stack.globalNametable();
			//					if (nametable.hasMapping(id)) {
			//						Stackable toCall = nametable.get(id);
			//						if (toCall instanceof Primitive<?>) {
			//							//put the value on the stack
			//							stack.push(toCall);
			//							//TODO call codeblocks and other callables (functions, constructors, methods etc.)
			//						} else {
			//							throw makeException("Call", param1.toString() + " does not refer to a callable value.");
			//						}
			//					} else {
			//					}
			//					break;
			default: {
				if (identifierPattern.matcher(token).matches()) {
					//				System.out.println("Identifier found");
					stack.push(new Identifier(token));
				} else if (intPattern.matcher(token).matches()) {
					//				System.out.println("Int literal found");
					try {
						Primitive<Long> literal = Primitive.createInteger(token.toLowerCase());
						stack.push(literal);
					} catch (CompilationError e) {
						throw new CompilationError(e);//makeException("Syntax", "No integer literal found in \"" + token + "\".");
					}
				} else if (doublePattern.matcher(token).matches()) {
					//					System.out.println("Double literal found");
					try {
						Primitive<Double> literal = new Primitive<Double>(Double.parseDouble(token.toLowerCase()));
						stack.push(literal);
					} catch (NumberFormatException e) {
						throw makeException("Syntax", "No double literal found in \"" + token + "\".");
					}
				} else if (boolPattern.matcher(token).matches()) {
					//				System.out.println("Bool literal found");
					Primitive<Boolean> literal = Primitive.createBoolean(token);
					stack.push(literal);
				} else if (stringPattern.matcher(token).matches()) {
					//				System.out.println("String literal found");
					stack.push(new Primitive<>(token.substring(1, token.length() - 1)));
				} else {
					//oh no, you have input invalid characters!
					throw makeException("Syntax", "Unexpected character(s) \"" + token + "\".");
				}
			}
			}
		} catch (CompilationError e) {
			if (e.isInfoPresent())
				System.out.println(e.getLocalizedMessage());
			else {
				System.out.println(makeException(e).getLocalizedMessage());
			}
		}
		return this;
	}

	/**
	 * <a href=
	 * "https://www.reddit.com/r/ProgrammerHumor/comments/auz30h/when_you_make_documentation_for_a_settergetter/?utm_source=share&utm_medium=web2x">...</a>
	 */
	public String getCode() {
		return tokenizer.getCode();
	}

	public int getCurrentLine() {
		Matcher linefinder = nlPat.matcher(getCode());
		int realIndex = tokenizer.getState().start;
		int lastLineStart = 0, linenum = 0;
		while (linefinder.find() && realIndex > lastLineStart) {
			System.out.println("Advancing to index " + linefinder.start() + " line " + (linenum+1));
			lastLineStart = linefinder.start();
			++linenum;
		}
		return linenum;
	}

	public int getIndexInsideLine() {
		Matcher linefinder = nlPat.matcher(getCode());
		int realIndex = tokenizer.getState().start;
		int lastLineStart = 0;
		while (linefinder.find() && realIndex > lastLineStart) {
			lastLineStart = linefinder.start();
		}
		//last line now contains the index where the line starts that begins before the matcher's index
		//i.e. the line of the matcher
		return realIndex - lastLineStart;
	}

	/**
	 * <a href=
	 * "https://www.reddit.com/r/ProgrammerHumor/comments/auz30h/when_you_make_documentation_for_a_settergetter/?utm_source=share&utm_medium=web2x">...</a>
	 */
	public IOInterface getIO() {
		return io;
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
			//this should not happen
			throw new RuntimeException("VERY DANGEROUS EXCEPTION", e);
		}
	}

	/**
	 * Constructs a compiler exception with the given base exception that points to
	 * the current place in code the interpreter is looking at. <br>
	 * <br>
	 * This method is intended to be used with exceptions thrown by other classes
	 * unaware of the interpreter state. These classes can use the simple format "<
	 * Name > < Description >" for their exception message to achieve suitable
	 * formatting of the exception. As only one line of the exception message is
	 * extracted, further lines can provide debug information to be used otherwise.
	 * @param cause The cause of this exception. The first word of the exception
	 * message is used as the name (such as "Syntax") and the rest as the long
	 * reason.
	 * @return The newly constructed compiler error.
	 */
	public CompilationError makeException(CompilationError cause) {
		Scanner helper = new Scanner(cause.getLocalizedMessage());
		//first part of any exception message is the exception name, which we don't want
		helper.next();
		String name = helper.next();
		String reason = helper.nextLine();
		helper.close();
		return makeException(name, reason);
	}

	/**
	 * Constructs a compiler exception with the given reason that points to the
	 * current place in code the interpreter is looking at.
	 * @param reason The reason or long description of the exception.
	 * @param name The name of the exception. If null, a generic "Compiler Error"
	 * name is used.
	 * @return The newly constructed compiler error.
	 */
	public CompilationError makeException(String name, String reason) {
		String code = getCode();
		int linenum = getCurrentLine();
		String line = code.split("\n")[linenum-1];
		return new CompilationError(line, getIndexInsideLine(), getCurrentLine(),  name == null ? "Compiler" : name, reason);
	}

	/** Resets this interpreter by deleting and reinitializing all state. */
	public Interpreter reset() {
		//make the stack
		stack = new Stack(this);
		//make the global nametable
		Nametable globalNametable = new Nametable();
		stack.push(globalNametable);
		return this;
	}

	/**
	 * Sets the code of this interpreter. Also prepares the code and regex utilities
	 * for execution; this is why a compilation error can be thrown here.
	 * @param code The SOF code to be used by this interpreter.
	 * @throws CompilationError If something during the code preprocessing stages
	 * goes wrong.
	 */
	public Interpreter setCode(String code) throws CompilationError {
		this.tokenizer = Tokenizer.fromSourceCode(code);
		return this;
	}

	/**
	 * Appends a line of code to the interpreter's current code. Useful for
	 * line-by-line source code scanning and interactive sessions.
	 * @param string The line of code to be appended
	 * @return this interpreter
	 */
	public Interpreter appendLine(String string) throws CompilationError {
		this.tokenizer.appendCode(string);
		return this;
	}

	/**
	 * <a href=
	 * "https://www.reddit.com/r/ProgrammerHumor/comments/auz30h/when_you_make_documentation_for_a_settergetter/?utm_source=share&utm_medium=web2x">...</a>
	 */
	public void setIO(IOInterface io) {
		this.io = io;
	}
}
