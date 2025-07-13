package klfr.sof;

import java.util.logging.*;
import java.io.Serializable;
import java.util.*;

import klfr.sof.ast.*;
import klfr.sof.cli.CLI;
import klfr.sof.exceptions.CompilerException;
import klfr.sof.exceptions.IncompleteCompilerException;
import klfr.sof.lang.*;
// resolve ambiguity java.util.Stack <-> klfr.sof.lang.Stack
import klfr.sof.lang.Stack;
import klfr.sof.lang.Stackable.DebugStringExtensiveness;
import klfr.sof.lang.TransparentData.TransparentType;
import klfr.sof.lang.functional.*;
import klfr.sof.lang.oop.*;
import klfr.sof.lang.oop.Object;
import klfr.sof.lang.primitive.*;
import klfr.sof.lib.*;
import klfr.sof.module.*;

/**
 * The SOF main Interpreter. This executes SOF code (in AST form).
 */
public class Interpreter implements Serializable {

	// #region Globals
	/** Major version of the interpreter. */
	public static final int					MAJOR_VERSION		= 0;
	/** Minor version of the interpreter. */
	public static final int					MINOR_VERSION		= 2;
	/** Bug &amp; security fix version of the interpreter. */
	public static final int					BUG_VERSION			= 0;
	/** Version of the interpreter. */
	public static final String				VERSION				= MAJOR_VERSION + "." + MINOR_VERSION + (BUG_VERSION > 0 ? ("." + BUG_VERSION) : "");
	private static final long				serialVersionUID	= (MAJOR_VERSION << 12) | (MINOR_VERSION << 6) | BUG_VERSION;

	/** The Interpreter logger. */
	protected static final Logger			log					= Logger.getLogger(Interpreter.class.getCanonicalName());

	/**
	 * Resource bundle identifier for the main SOF message resource bundle. It contains all messages that the command line
	 * interpreter produces, as well as error messages and debug terms.
	 */
	public static final String				MESSAGE_RESOURCE	= "klfr.sof.SOFMessages";
	/**
	 * The resource bundle that contains the SOF messages. It contains all messages that the command line interpreter
	 * produces, as well as error messages and debug terms.
	 */
	public static final ResourceBundle	R						= ResourceBundle.getBundle(MESSAGE_RESOURCE);

	// #endregion

	// #region Utility

	static String f(final String s, final java.lang.Object... args) {
		return String.format(s, (java.lang.Object) args);
	}

	/** Convenience constant for the 66-character line ─ */
	public static final String line66 = String.format("%66s", " ").replace(" ", "─");

	/**
	 * <a href=
	 * "https://www.reddit.com/r/ProgrammerHumor/comments/auz30h/when_you_make_documentation_for_a_settergetter/?utm_source=share&utm_medium=web2x">...</a>
	 * 
	 * @return The I/O interface of this interpreter.
	 */
	public final IOInterface getIO() {
		return io;
	}

	/**
	 * <a href=
	 * "https://www.reddit.com/r/ProgrammerHumor/comments/auz30h/when_you_make_documentation_for_a_settergetter/?utm_source=share&utm_medium=web2x">...</a>
	 * 
	 * @return The stack of this interpreter.
	 */
	public final Stack getStack() {
		return stack;
	}

	// #endregion

	// #region Variables

	/**
	 * The I/O interface this interpreter uses for communication with the user.
	 */
	protected final IOInterface				io;

	/**
	 * All of the program memory, which is only a simple stack due to SOF's strict stack-based nature.
	 */
	protected final Stack						stack;

	/**
	 * The number of successful asserts executed by this interpreter.
	 */
	protected int									assertCount;

	/**
	 * The module discovery system that this interpreter uses.
	 */
	protected final ModuleDiscoverer			moduleDiscoverer;

	/**
	 * The native function registry that this interpreter uses.
	 */
	protected final NativeFunctionRegistry	nativeFunctionRegistry;

	/**
	 * Returns the number of asserts that were successfully performed by this interpreter.
	 * 
	 * @return The number of asserts that were successfully performed by this interpreter.
	 */
	public final int getAssertCount() {
		return assertCount;
	}

	/**
	 * Returns the module discovery system that this interpreter uses.
	 * 
	 * @return The module discovery system that this interpreter uses.
	 */
	public ModuleDiscoverer getModuleDiscoverer() {
		return moduleDiscoverer;
	}

	// #endregion

	/**
	 * Create a new interpreter.
	 * 
	 * @param io                     The I/O interface of this interpreter.
	 * @param nativeFunctionRegistry The native function registry that is used to execute native functions.
	 */
	public Interpreter(IOInterface io, NativeFunctionRegistry nativeFunctionRegistry) {
		this(io, new ModuleDiscoverer(), nativeFunctionRegistry);
	}

	/**
	 * Create a new interpreter. This is intended to be used by subclasses.
	 * 
	 * @param md       The module discoverer that is used to load and execute SOF modules.
	 * @param io       The I/O interface of this interpreter.
	 * @param registry The native function registry that is used to execute native functions.
	 */
	public Interpreter(IOInterface io, ModuleDiscoverer md, NativeFunctionRegistry registry) {
		this.io = io;
		this.stack = new Stack();
		this.moduleDiscoverer = md;
		this.nativeFunctionRegistry = registry;
		this.reset();
	}

	/**
	 * Reset the interpreter's stack and nametable
	 * 
	 * @return This interpreter.
	 */
	public final Interpreter reset() {
		this.stack.clear();
		stack.push(new Nametable());
		return this;
	}

	// #region Execution

	/**
	 * Run a parsed SOF program without resetting state. This method is thread-safe and will only allow one execution on
	 * this interpreter at the same time. Therefore, it cannot be called recursively. This method does not reset the
	 * interpreter and is therefore suitable for interactive execution or any other kind of execution that is done in steps.
	 * 
	 * @param sofProgram The parsed program to run.
	 * @return This interpreter.
	 * @throws CompilerException If the execution fails with an error.
	 */
	public final Interpreter run(SOFFile sofProgram) throws CompilerException {
		synchronized (this) {
			log.entering(Interpreter.class.getCanonicalName(), "run # synchronized");
			try {
				sofProgram.ast().forEach((Node.ForEachType) this::handle);
			} catch (IncompleteCompilerException e) {
				throw new RuntimeException("Incomplete compiler exception escaped, this shouldn't happen.", e);
			}
		}
		log.exiting(Interpreter.class.getCanonicalName(), "run");
		return this;
	}

	/**
	 * Callback for handling a node.
	 * 
	 * @param n The node to be handled.
	 * @return Whether the current scope should be continued to be executed.
	 * @throws CompilerException If an error in the execution occurred.
	 * @see klfr.sof.ast.Node.ForEachType#exec(Node)
	 */
	protected boolean handle(Node n) throws CompilerException {
		try {
			// manual dynamic dispatch -- there isn't a better reflection-free way
			if (n instanceof TokenListNode tln)
				return handle(tln);
			else if (n instanceof LiteralNode ln)
				return handle(ln);
			else if (n instanceof PrimitiveTokenNode ptn)
				return handle(ptn);
			else
				throw new RuntimeException("Unknown node type.");
		} catch (IncompleteCompilerException incomplete) {
			throw CompilerException.fromIncomplete(n.getSource(), n.getCodeIndex(), incomplete);
		}
	}

	/**
	 * Callback for handling a token list node. This method pushes a new code block to the stack.
	 * 
	 * @param codeblock The token list node to be handled.
	 * @return Whether the current scope should be continued to be executed.
	 * @see klfr.sof.ast.Node.ForEachType#exec(Node)
	 */
	protected boolean handle(TokenListNode codeblock) {
		this.stack.push(new CodeBlock(codeblock));
		return true;
	}

	/**
	 * Callback for handling a literal node. This method pushes the value of the literal node to the stack.
	 * 
	 * @param literal The literal node to be handled.
	 * @return Whether the current scope should be continued to be executed.
	 * @see klfr.sof.ast.Node.ForEachType#exec(Node)
	 */
	protected boolean handle(LiteralNode literal) {
		this.stack.push(literal.getValue());
		return true;
	}

	/**
	 * Primitive token handler; takes care of much of the central logic. Executes the action of the given primitive token.
	 * 
	 * @param pt The primitive token to handle.
	 * @return Whether the current scope should be continued to be executed.
	 * @see klfr.sof.ast.Node.ForEachType#exec(Node)
	 * @throws CompilerException           If an error occurred while executing.
	 * @throws IncompleteCompilerException If a non-locatable error occurred while executing.
	 */
	protected boolean handle(PrimitiveTokenNode pt) throws CompilerException, IncompleteCompilerException {
		// BEHOLD THE SWITCH CASE OF DOOM
		// if the switch case is optimized to jump table lookups, this may be the
		// fastest way of handling all of the primitive tokens selectively
		switch (pt.symbol()) {
		// builtin functions
		// -- repetitive code, I know. It's fasther though.
		case Add: {
			// type-checking inside builtin, same story below
			doBinaryOperation(BuiltinOperations::add);
			return true;
		}
		case Subtract: {
			doBinaryOperation(BuiltinOperations::subtract);
			return true;
		}
		case Multiply: {
			doBinaryOperation(BuiltinOperations::multiply);
			return true;
		}
		case Divide: {
			doBinaryOperation(BuiltinOperations::divide);
			return true;
		}
		case Modulus: {
			doBinaryOperation(BuiltinOperations::modulus);
			return true;
		}
		case BitShiftLeft: {
			doBinaryOperation(BuiltinOperations::bitShiftLeft);
			return true;
		}
		case BitShiftRight: {
			doBinaryOperation(BuiltinOperations::bitShiftRight);
			return true;
		}
		case Concatenate: {
			doBinaryOperation((a, b) -> StringPrimitive.createStringPrimitive(a.print() + b.print()));
			return true;
		}
		case And: {
			doBinaryOperation(BuiltinOperations::logicalAnd);
			return true;
		}
		case Or: {
			doBinaryOperation(BuiltinOperations::logicalOr);
			return true;
		}
		case ExclusiveOr: {
			doBinaryOperation(BuiltinOperations::logicalXor);
			return true;
		}
		case Not: {
			final var val = this.stack.popSafe();
			this.stack.push(BoolPrimitive.createBoolPrimitive(val.isFalse()));
			return true;
		}
		// comparison and equality
		case Equals: {
			doBinaryOperation(BuiltinOperations::equals);
			return true;
		}
		case NotEquals: {
			doBinaryOperation(BuiltinOperations::notEquals);
			return true;
		}
		case GreaterThan: {
			doBinaryOperation(BuiltinOperations::greaterThan);
			return true;
		}
		case GreaterThanEquals: {
			doBinaryOperation(BuiltinOperations::greaterEqualThan);
			return true;
		}
		case LessThan: {
			doBinaryOperation(BuiltinOperations::lessThan);
			return true;
		}
		case LessThanEquals: {
			doBinaryOperation(BuiltinOperations::lessEqualThan);
			return true;
		}
		// stack operations
		case Discard: {
			this.stack.popSafe();
			return true;
		}
		case Duplicate: {
			final var a = this.stack.popSafe();
			this.stack.push(a);
			this.stack.push(a);
			return true;
		}
		case Swap: {
			final var eltop = this.stack.popSafe();
			final var elbot = this.stack.popSafe();
			this.stack.push(eltop);
			this.stack.push(elbot);
			return true;
		}
		// conditionals and loops
		case If: {
			final var cond = this.stack.popSafe();
			final var callable = this.stack.popSafe();
			if (cond.isTrue())
				return this.doCall(callable);
			return true;
		}
		case IfElse: {
			final var elseCallable = this.stack.popSafe();
			final var cond = this.stack.popSafe();
			final var callable = this.stack.popSafe();
			return this.doCall(cond.isTrue() ? callable : elseCallable);
		}
		case Switch: {
			// first argument is the default action callable
			final var defaultCallable = this.stack.popSafe();
			// loop until throw or switch end marker
			while (true) {
				// get case and corresponding body
				final Stackable _case = this.stack.popSafe();
				// if this is the end marker already, we only have a default case, so run that
				if (_case instanceof Identifier maybeSwitchEnd && maybeSwitchEnd.getValue().equals("switch::")) {
					return this.doCall(defaultCallable);
				}
				final Stackable body = this.stack.popSafe();
				// execute case
				this.doCall(_case);
				final var result = this.stack.popSafe();
				// ... and check if successful; if so, run body and exit
				if (result.isTrue()) {
					// remove elements until identifier "switch"
					var elt = this.stack.popSafe();
					while (!(elt instanceof Identifier && ((Identifier) elt).getValue().equals("switch::")))
						elt = this.stack.popSafe();
					// call body after removing remainder data, to allow bodies to modify the stack as usual
					return this.doCall(body);
				} else {
					final var elt = this.stack.popSafe();
					if (elt instanceof Identifier maybeSwitchEnd && maybeSwitchEnd.getValue().equals("switch::")) {
						// switch end was reached without executing any case: execute default callable
						return this.doCall(defaultCallable);
					} else {
						// just another pair of case and body; do that in the next loop
						this.stack.push(elt);
					}
				}
			}
		}
		case While: {
			final var condCallable = this.stack.popSafe();
			final var bodyCallable = this.stack.popSafe();
			while (true) {
				// execute the condition
				this.doCall(condCallable);
				var preContinue = this.stack.popSafe();
				if (preContinue.isTrue()) {
					// abort if the return flag was set (false)
					var retflag = this.doCall(bodyCallable);
					if (!retflag)
						return retflag;
				} else
					// end normally when condition doesn't hold anymore
					return true;
			}
		}
		case DoWhile: {
			final var condCallable = this.stack.popSafe();
			final var bodyCallable = this.stack.popSafe();

			// run initial body
			var retflag = this.doCall(bodyCallable);
			if (!retflag)
				return retflag;

			while (true) {
				// execute the condition
				this.doCall(condCallable);
				var preContinue = this.stack.popSafe();
				if (preContinue.isTrue()) {
					// abort if the return flag was set (false)
					retflag = this.doCall(bodyCallable);
					if (!retflag)
						return retflag;
				} else
					// end normally when condition doesn't hold anymore
					return true;
			}
		}
		// naming and calling
		case Call: {
			final Stackable toCall = this.stack.popSafe();
			return this.doCall(toCall);
		}
		case DoubleCall: {
			this.doCall(this.stack.popSafe());
			return this.doCall(this.stack.popSafe());
		}
		case ObjectCall: {
			final Stackable toCall = this.stack.popSafe();
			final Object object = this.stack.popTyped(Object.class);
			this.stack.push(object.getAttributes());

			// note that because we may execute a function, a normal function delimiter is used as the scope
			final var result = this.doCall(toCall);

			// there may be a return value we want to preserve
			final var returnValue = this.stack.forcePop();
			// in this case, a return value is present
			if (returnValue != object.getAttributes()) {
				// remove like with local scopes
				this.stack.popFirstNametable();
				// push object first so that order is preserved
				this.stack.push(object);
				this.stack.push(returnValue);
			} else {
				// attribute table was already removed, just add back object
				this.stack.push(object);
			}
			object.getAttributes().setReturn(null);
			return result;
		}
		case ObjectMethodCall: {
			final var methodName = this.stack.popSafe();
			// the name of the method to call is not resolved yet, resolve it first like with double calls
			this.doCall(methodName);
			final var method = this.stack.popTyped(Function.class);

			// Because the method is known, its arguments can be removed temporarily to recieve the target object underneath.
			final var arguments = this.stack.popSafe((int) method.arguments);
			final var object = this.stack.popTyped(Object.class);

			// for catching the return value of the method after doCall has finished
			final var returnSafetyNT = new FunctionDelimiter();
			this.stack.push(returnSafetyNT);
			// re-push the arguments after the safety nametable
			this.stack.pushAll(arguments);

			this.doCall(method, object.getAttributes());

			if (this.stack.peekSafe() == returnSafetyNT) {
				// no return value
				this.stack.popFirstNametable();
				// push the object immediately to restore stack state
				this.stack.push(object);
			} else {
				// remove safety NT, swap return value & object order
				final var retval = this.stack.popSafe();
				this.stack.popFirstNametable();
				this.stack.push(object);
				this.stack.push(retval);
			}

			// deletes the return value to not fuck up future method calls on this object
			object.getAttributes().setReturn(null);
			return true;
		}
		case NativeCall: {
			this.doNativeCall(this.stack.popSafe());
			return true;
		}
		case Define: {
			final var id = this.stack.popTyped(Identifier.class);
			final var value = this.stack.popSafe();
			this.stack.localScope().put(id, value);
			return true;
		}
		// the default interpreter rerouts this to globaldefine behavior, because export is noop
		case DefineExport_Sugar:
		case GlobalDefine: {
			final var id = this.stack.popTyped(Identifier.class);
			final var value = this.stack.popSafe();
			final var gnt = this.stack.globalNametable();
			gnt.put(id, value);
			return true;
		}
		// functions
		case Function: {
			final var argcount = this.stack.popTyped(IntPrimitive.class);
			final var code = this.stack.popTyped(CodeBlock.class).code;
			this.stack.push(new Function(code, argcount.value(), this.stack.globalNametable()));
			return true;
		}
		case Constructor: {
			// similar to normal function code above
			final var argcount = this.stack.popTyped(IntPrimitive.class);
			final var code = this.stack.popTyped(CodeBlock.class).code;
			this.stack.push(new ConstructorFunction(code, argcount.value(), this.stack.globalNametable()));
			return true;
		}
		case Return: {
			final var retval = this.stack.popSafe();
			this.stack.localScope().setReturn(retval);
			return false;
		}
		case ReturnNothing: {
			// no return value assignment
			return false;
		}
		// module system
		case Use: {
			final var moduleSpecifier = this.stack.popTyped(StringPrimitive.class).value();
			final var maybeModule = moduleDiscoverer.getModule(pt.getSource().sourceFile(), moduleSpecifier);
			if (maybeModule.isEmpty())
				throw CompilerException.from(pt.getSource(), pt.getCodeIndex(), "module", null, moduleSpecifier);
			final var module = maybeModule.get();

			// dispatch module to a new interpreter that can handle `export` keywords
			final var moduleRunner = new ModuleInterpreter(this.io, this.moduleDiscoverer, nativeFunctionRegistry);
			CLI.runPreamble(moduleRunner);
			moduleRunner.run(module);

			// retrieve module exports and add them to this global nametable
			final var exports = moduleRunner.getExports();
			final var gnt = this.stack.globalNametable();
			gnt.putAll(exports);

			return true;
		}
		case Export: {
			// the default interpreter noops the export keyword so that module-like files can still be run normally
			// pop the identifier that is also used by proper `export`
			this.stack.popSafe();
			return true;
		}
		// i/o
		case Input: {
			final String input = this.io.nextInputSequence();
			this.stack.push(StringPrimitive.createStringPrimitive(input));
			return true;
		}
		case InputLine: {
			final String input = this.io.nextInputLine();
			this.stack.push(StringPrimitive.createStringPrimitive(input));
			return true;
		}
		case Write: {
			this.io.print(this.stack.popSafe().print());
			return true;
		}
		case WriteLine: {
			this.io.println(this.stack.popSafe().print());
			return true;
		}
		// debug
		case DescribeElement: {
			this.io.debug(this.stack.peekSafe().toDebugString(DebugStringExtensiveness.Full));
			return true;
		}
		case DescribeStack: {
			this.io.describeStack(this.stack);
			return true;
		}
		case Assert: {
			if (this.stack.popSafe().isFalse())
				throw CompilerException.from(pt.getSource(), pt.getCodeIndex(), "assert", null);
			++this.assertCount;
			return true;
		}
		case CreateList: {
			final var listElements = new LinkedList<Stackable>();
			var currentElement = this.stack.popSafe(false);
			var listEndMarker = new TransparentData(TransparentType.ListStart);
			while (!currentElement.equals(listEndMarker)) {
				listElements.addFirst(currentElement);
				currentElement = this.stack.popSafe(false);
			}
			this.stack.push(new ListPrimitive(listElements));
			return true;
		}
		default:
			throw new RuntimeException("Unhandled primitive token.");
		}
	}

	/**
	 * Executes the given binary operation on the stack. Two operands are pulled of the stack and passed to the binary
	 * operation. The first operand is the lower one on the stack.
	 * 
	 * @param operation The binary operation function that shall be executed. Most of these are defined in
	 *                     {@link klfr.sof.lang.BuiltinOperations}.
	 * @throws IncompleteCompilerException If the binary operation or the stack manipulation fails.
	 * @throws CompilerException           If the binary operation or the stack manipulation fails.
	 */
	protected final void doBinaryOperation(BuiltinOperations.BinaryOperation operation) throws IncompleteCompilerException, CompilerException {
		final Stackable rhs = this.stack.popSafe();
		Optional<Identifier> lhsName = Optional.empty();
		if (this.stack.peek() instanceof Identifier) {
			lhsName = Optional.of((Identifier) this.stack.popSafe());
			// do a call on the identifier to replace it with its value
			this.doCall(lhsName.get());
		}
		final Stackable lhs = this.stack.popSafe();
		final Stackable result = operation.apply(lhs, rhs);

		if (lhsName.isEmpty()) {
			stack.push(result);
		} else {
			// if there is a name, i.e. we need to rebind an identifier
			stack.localScope().put(lhsName.get(), result);
		}
	}

	/**
	 * Executes a native call on this interpreter, using the given native function name as an SOF string. This may modify
	 * the stack.
	 * 
	 * @param _fname The native function name, as an SOF string.
	 * @throws IncompleteCompilerException If the native call fails internally or externally (arguments etc.).
	 */
	protected final void doNativeCall(Stackable _fname) throws IncompleteCompilerException {
		// typecheck and retrieve function
		if (!(_fname instanceof StringPrimitive))
			throw new IncompleteCompilerException("type");
		final var fname = ((StringPrimitive) _fname).value();
		log.fine(() -> String.format("Native call function '%s'", fname));
		final var nativeFunc_ = nativeFunctionRegistry.getNativeFunction(fname);
		if (nativeFunc_.isEmpty())
			throw new IncompleteCompilerException("native", "native.unknown", fname);
		final var nativeFunc = nativeFunc_.get();
		final var result = nativeFunc.call(this);
		if (result != null) {
			log.finer(() -> String.format("Native call function returned %s", result.toDebugString(DebugStringExtensiveness.Compact)));
			this.stack.push(result);
		} else {
			log.finer("Native call function returned null");
		}
	}

	/**
	 * Helper function to execute the call operation on the stackable, depending on the type. This function may modify the
	 * current interpreter state.
	 * 
	 * @param toCall The stackable that is to be called.
	 * @return Whether any of the subcalls encountered a return statement. This is necessary so that return statements
	 *         propagate through CodeBlocks and are only caught by Functions.
	 * @throws CompilerException           If the call fails with a specified location.
	 * @throws IncompleteCompilerException If the call fails with no specified location.
	 */
	protected final boolean doCall(final Stackable toCall) throws IncompleteCompilerException, CompilerException {
		return this.doCall(toCall, new FunctionDelimiter());
	}

	/**
	 * Helper function to execute the call operation on the stackable, depending on the type. This function may modify the
	 * current interpreter state.
	 * 
	 * @param toCall The stackable that is to be called.
	 * @param scope  The nametable that should act as the surrounding scope for the call. Note that some call types, such as
	 *                  identifiers, do not use a scope.
	 * @return Whether execution of the current function should continue. This is necessary so that return statements
	 *         propagate through CodeBlocks and are only caught by Functions.
	 * @throws CompilerException           If the call fails with a specified location.
	 * @throws IncompleteCompilerException If the call fails with no specified location.
	 */
	protected final boolean doCall(final Stackable toCall, final Nametable scope) throws IncompleteCompilerException, CompilerException {
		if (toCall instanceof Identifier id) {
			final var val = this.stack.lookup(id);
			if (val == null)
				throw new IncompleteCompilerException("name", id);
			this.stack.push(val);
			return true;
		} else if (toCall instanceof Primitive) {
			this.stack.push(toCall);
			return true;
		} else if (toCall instanceof ConstructorFunction constructor) {
			var pushedGlobalNametable = false;
			if (this.stack.globalNametable() != constructor.getGlobalNametable()) {
				this.stack.pushGlobalNametable(constructor.getGlobalNametable());
				pushedGlobalNametable = true;
			}

			final var arguments = this.stack.popSafe((int) constructor.arguments);

			final var newObject = new Object();
			// push the object nametable as a delimiter, then the object itself as a "self" argument to the method
			this.stack.push(newObject.getAttributes());
			this.stack.push(newObject);
			this.stack.pushAll(arguments);

			// run method and ignore state
			constructor.code.forEach((Node.ForEachType) this::handle);

			// ignore return value. constructors can still return stuff, so that the user may define multi-purpose functions/constructors.
			final var table = this.stack.popFirstNametable().orElseThrow(() -> new RuntimeException("Local nametable was removed unexpectedly."));

			// re-push the object (was removed by above operation)
			this.stack.push(newObject);

			if (pushedGlobalNametable)
				this.stack.popGlobalNametable();
			return true;
		} else if (toCall instanceof CurriedFunction function) {
			this.stack.pushAll(function.getCurriedArguments());
			return doCall(function.getRegularFunction(), scope);
		} else if (toCall instanceof Function function) {
			var pushedGlobalNametable = false;
			if (this.stack.globalNametable() != function.getGlobalNametable()) {
				this.stack.pushGlobalNametable(function.getGlobalNametable());
				pushedGlobalNametable = true;
			}

			// HINT: handle the function before the codeblock because it inherits from it
			final var subProgram = function.code;

			// setup stack with arguments
			// at the same time, detect a curried function
			final var args = new LinkedList<Stackable>();
			var remainingArguments = function.arguments;
			log.fine(stack.toStringExtended());
			while (remainingArguments > 0) {
				final var argumentOrCurryDelimiter = this.stack.popSafe(false);
				if (argumentOrCurryDelimiter instanceof TransparentData transparentData) {
					// Any other transparent data is skipped as normally.
					if (transparentData.getType() == TransparentData.TransparentType.CurryPipe)
						break;
				} else {
					args.addFirst(argumentOrCurryDelimiter);
					remainingArguments--;
				}
			}

			// This function is not curried; we can just execute it.
			if (remainingArguments == 0) {
				this.stack.push(scope);
				if (function.arguments > 0)
					this.stack.pushAll(args);

				// run and ignore return state
				subProgram.forEach((Node.ForEachType) this::handle);

				// get return value through nametable
				final var table = this.stack.popFirstNametable().orElseThrow(() -> new RuntimeException("Local nametable was removed unexpectedly."));
				if (!(table instanceof FunctionDelimiter))
					throw new RuntimeException("Unexpected nametable type " + table.getClass().toString());
				((FunctionDelimiter) table).pushReturnValue(this.stack);

				if (pushedGlobalNametable)
					this.stack.popGlobalNametable();
				return true;
			}
			// This function is curried; we create a proxy for it.
			else {
				final var curriedFunction = new CurriedFunction(function, args, this.stack.globalNametable());
				this.stack.push(curriedFunction);

				if (pushedGlobalNametable)
					this.stack.popGlobalNametable();
				return true;
			}
		} else if (toCall instanceof CodeBlock codeblock) {
			final var subProgram = codeblock.code;
			// just run, no return value, no stack protect
			return subProgram.forEach((Node.ForEachType) this::handle);
		} else if (toCall instanceof ChurchNumeral numeral) {
			final var callCount = numeral.value();
			final var toCallRepeatedly = this.stack.pop();
			for (int i = 0; i < callCount; ++i) {
				final var result = this.doCall(toCallRepeatedly, scope);
				if (!result)
					return false;
			}
			return true;
		} else
			throw new IncompleteCompilerException("call", "type.call", toCall.typename());
	}

	// #endregion Execution

}

/*  
The SOF programming language interpreter.
Copyright (C) 2019-2022  kleinesfilmröllchen

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
