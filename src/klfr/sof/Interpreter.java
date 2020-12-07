package klfr.sof;

import java.util.logging.*;
import java.io.Serializable;
import java.util.*;
import java.util.function.*;

import klfr.sof.ast.*;
import klfr.sof.cli.CLI;
import klfr.sof.lang.*;
// resolve ambiguity java.util.Stack <-> klfr.sof.lang.Stack
import klfr.sof.lang.Stack;
import klfr.sof.lang.Stackable.DebugStringExtensiveness;
import klfr.sof.lang.functional.*;
import klfr.sof.lang.oop.*;
import klfr.sof.lang.primitive.*;
import klfr.sof.lib.*;
import klfr.sof.lib.NativeFunctionRegistry.*;
import klfr.sof.module.*;

/**
 * 
 */
public class Interpreter implements Serializable {
	// #region Globals
	/** Major version of the interpreter. */
	public static final int MAJOR_VERSION = 0;
	/** Minor version of the interpreter. */
	public static final int MINOR_VERSION = 1;
	/** Bug &amp; security fix version of the interpreter. */
	public static final int BUG_VERSION = 0;
	/** Version of the interpreter. */
	public static final String VERSION = MAJOR_VERSION + "." + MINOR_VERSION
			+ (BUG_VERSION > 0 ? ("." + BUG_VERSION) : "");
	private static final long serialVersionUID = (MAJOR_VERSION << 12) | (MINOR_VERSION << 6) | BUG_VERSION;

	protected static final Logger log = Logger.getLogger(Interpreter.class.getCanonicalName());

	/**
	 * Resource bundle identifier for the main SOF message resource bundle. It
	 * contains all messages that the command line interpreter produces, as well as
	 * error messages and debug terms.
	 */
	public static final String MESSAGE_RESOURCE = "klfr.sof.SOFMessages";
	public static final ResourceBundle R = ResourceBundle.getBundle(MESSAGE_RESOURCE);

	// #endregion

	// #region Utility

	static String f(final String s, final Object... args) {
		return String.format(s, args);
	}

	/** Convenience constant for the 66-character line ─ */
	public static final String line66 = String.format("%66s", " ").replace(" ", "─");

	/**
	 * <a href=
	 * "https://www.reddit.com/r/ProgrammerHumor/comments/auz30h/when_you_make_documentation_for_a_settergetter/?utm_source=share&utm_medium=web2x">...</a>
	 */
	public IOInterface getIO() {
		return io;
	}

	// #endregion

	// #region Variables

	/**
	 * The I/O interface this interpreter uses for communication with the user.
	 */
	protected final IOInterface io;

	/**
	 * All of the program memory, which is only a simple stack due to SOF's strict
	 * stack-based nature.
	 */
	protected final Stack stack;

	protected int assertCount;

	protected ModuleDiscoverer moduleDiscoverer;

	/**
	 * Returns the number of asserts that were successfully performed by this
	 * interpreter.
	 */
	public int getAssertCount() {
		return assertCount;
	}

	// #endregion

	public Interpreter(IOInterface io) {
		this(io, new ModuleDiscoverer());
	}

	// for subclasses
	protected Interpreter(IOInterface io, ModuleDiscoverer md) {
		this.io = io;
		this.stack = new Stack();
		this.moduleDiscoverer = md;
		this.reset();
	}

	/**
	 * Reset the interpreter's stack and nametable
	 * 
	 * @return this interpreter.
	 */
	public Interpreter reset() {
		this.stack.clear();
		stack.push(new Nametable());
		return this;
	}

	// #region Execution

	/**
	 * Run a parsed SOF program without resetting state. This method is thread-safe
	 * and will only allow one execution on this interpreter at the same time.
	 * Therefore, it cannot be called recursively. This method does not reset the
	 * interpreter and is therefore suitable for interactive execution or any other
	 * kind of execution that is done in steps.
	 * 
	 * @return this interpreter.
	 */
	public Interpreter run(SOFFile sofProgram) throws CompilerException {
		synchronized (this) {
			log.entering(Interpreter.class.getCanonicalName(), "run # synchronized");
			sofProgram.ast().forEach((Function<Node, Boolean>) this::handle);
		}
		log.exiting(Interpreter.class.getCanonicalName(), "run");
		return this;
	}

	/**
	 * Callback for handling a node.
	 */
	protected boolean handle(Node n) throws CompilerException {
		try {
			// manual dynamic dispatch -- there isn't a better reflection-free way
			if (n instanceof TokenListNode)
				return handle((TokenListNode) n);
			else if (n instanceof LiteralNode)
				return handle((LiteralNode) n);
			else if (n instanceof PrimitiveTokenNode)
				return handle((PrimitiveTokenNode) n);
			else
				throw new RuntimeException("Unknown node type.");
		} catch (CompilerException.Incomplete incomplete) {
			throw CompilerException.fromIncomplete(n.getSource(), n.getCodeIndex(), incomplete);
		}
	}

	protected boolean handle(TokenListNode codeblock) throws CompilerException {
		this.stack.push(new CodeBlock(codeblock));
		return true;
	}

	protected boolean handle(LiteralNode literal) throws CompilerException {
		this.stack.push(literal.getValue());
		return true;
	}

	/**
	 * Primitive token handler; takes care of much of the central logic.
	 * 
	 * @param pt The primitive token to execute.
	 */
	protected boolean handle(PrimitiveTokenNode pt) throws CompilerException {
		// BEHOLD THE SWITCH CASE OF DOOM
		// if the switch case is optimized to jump table lookups, this may be the
		// fastest way of handling all of the primitive tokens selectively
		switch (pt.symbol()) {
			// builtin functions
			// -- repetitive code, I know. It's fasther though.
			case Add: {
				// type-checking inside builtin, same story below
				final Stackable rhs = this.stack.pop(), lhs = this.stack.pop();
				this.stack.push(BuiltinPTs.add(lhs, rhs));
				return true;
			}
			case Subtract: {
				final Stackable rhs = this.stack.pop(), lhs = this.stack.pop();
				this.stack.push(BuiltinPTs.subtract(lhs, rhs));
				return true;
			}
			case Multiply: {
				final Stackable rhs = this.stack.pop(), lhs = this.stack.pop();
				this.stack.push(BuiltinPTs.multiply(lhs, rhs));
				return true;
			}
			case Divide: {
				final Stackable rhs = this.stack.pop(), lhs = this.stack.pop();
				this.stack.push(BuiltinPTs.divide(lhs, rhs));
				return true;
			}
			case Concatenate: {
				final Stackable rhs = this.stack.pop(), lhs = this.stack.pop();
				this.stack.push(StringPrimitive.createStringPrimitive(lhs.print() + rhs.print()));
				return true;
			}
			case And: {
				final Stackable rhs = this.stack.pop(), lhs = this.stack.pop();
				this.stack.push(BuiltinPTs.logicalAnd(lhs, rhs));
				return true;
			}
			case Or: {
				final Stackable rhs = this.stack.pop(), lhs = this.stack.pop();
				this.stack.push(BuiltinPTs.logicalOr(lhs, rhs));
				return true;
			}
			case ExclusiveOr: {
				final Stackable rhs = this.stack.pop(), lhs = this.stack.pop();
				this.stack.push(BuiltinPTs.logicalXor(lhs, rhs));
				return true;
			}
			case Not: {
				final var val = this.stack.pop();
				this.stack.push(BoolPrimitive.createBoolPrimitive(val.isFalse()));
				return true;
			}
			// comparison and equality
			case Equals: {
				final Stackable rhs = this.stack.pop(), lhs = this.stack.pop();
				this.stack.push(BuiltinPTs.equals(lhs, rhs));
				return true;
			}
			case NotEquals: {
				final Stackable rhs = this.stack.pop(), lhs = this.stack.pop();
				this.stack.push(BuiltinPTs.notEquals(lhs, rhs));
				return true;
			}
			case GreaterThan: {
				final Stackable rhs = this.stack.pop(), lhs = this.stack.pop();
				this.stack.push(BuiltinPTs.greaterThan(lhs, rhs));
				return true;
			}
			case GreaterThanEquals: {
				final Stackable rhs = this.stack.pop(), lhs = this.stack.pop();
				this.stack.push(BuiltinPTs.greaterEqualThan(lhs, rhs));
				return true;
			}
			case LessThan: {
				final Stackable rhs = this.stack.pop(), lhs = this.stack.pop();
				this.stack.push(BuiltinPTs.lessThan(lhs, rhs));
				return true;
			}
			case LessThanEquals: {
				final Stackable rhs = this.stack.pop(), lhs = this.stack.pop();
				this.stack.push(BuiltinPTs.lessEqualThan(lhs, rhs));
				return true;
			}
			// stack operations
			case Discard: {
				this.stack.pop();
				return true;
			}
			case Duplicate: {
				final var a = this.stack.pop();
				this.stack.push(a);
				this.stack.push(a);
				return true;
			}
			case Swap: {
				final var eltop = this.stack.pop();
				final var elbot = this.stack.pop();
				this.stack.push(eltop);
				this.stack.push(elbot);
				return true;
			}
			// conditionals and loops
			case If: {
				final var cond = this.stack.pop();
				final var callable = this.stack.pop();
				if (cond.isTrue())
					return this.doCall(callable);
				return true;
			}
			case IfElse: {
				final var elseCallable = this.stack.pop();
				final var cond = this.stack.pop();
				final var callable = this.stack.pop();
				return this.doCall(cond.isTrue() ? callable : elseCallable);
			}
			case Switch: {
				// first argument is the default action callable
				final var defaultCallable = this.stack.pop();
				// loop until throw or switch end marker
				while (true) {
					// get case and corresponding body
					final Stackable _case = this.stack.pop(), body = this.stack.pop();
					// execute case
					this.doCall(_case);
					final var result = this.stack.pop();
					// ... and check if successful; if so, run body and exit
					if (result.isTrue()) {
						// properly propagate the return flag
						final var retflag = this.doCall(body);
						// remove elements until identifier "switch"
						var elt = this.stack.pop();
						while (!(elt instanceof Identifier && ((Identifier) elt).getValue().equals("switch::")))
							elt = this.stack.pop();
						return retflag;
					} else {
						final var elt = this.stack.pop();
						if (elt instanceof Identifier && ((Identifier) elt).getValue().equals("switch::")) {
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
				final var condCallable = this.stack.pop();
				final var bodyCallable = this.stack.pop();
				while (true) {
					// execute the condition
					this.doCall(condCallable);
					var preContinue = this.stack.pop();
					if (preContinue.isTrue()) {
						// abort if the return flag was set (false)
						var retflag = this.doCall(bodyCallable);
						if (!retflag) return retflag;
					}
					else
						// end normally when condition doesn't hold anymore
						return true;
				}
			}
			// naming and calling
			case Call: {
				final Stackable toCall = this.stack.pop();
				return this.doCall(toCall);
			}
			case DoubleCall: {
				this.doCall(this.stack.pop());
				return this.doCall(this.stack.pop());
			}
			case ObjectCall: {
				final Stackable toCall = this.stack.pop();
				final SObject object = this.stack.popTyped(SObject.class);
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
				return result;
			}
			case ObjectMethodCall: {
				final var toCall = this.stack.pop();
				final var object = this.stack.popTyped(SObject.class);
				this.stack.push(object);
				return this.doCall(toCall, object.getAttributes());
			}
			case NativeCall: {
				this.doNativeCall(this.stack.pop());
				return true;
			}
			case Define: {
				final var id = this.stack.popTyped(Identifier.class);
				final var value = this.stack.pop();
				this.stack.localScope().put(id, value);
				return true;
			}
			// the default interpreter rerouts this to globaldefine behavior, because export is noop
			case DefineExport_Sugar:
			case GlobalDefine: {
				final var id = this.stack.popTyped(Identifier.class);
				final var value = this.stack.pop();
				final var gnt = this.stack.globalNametable();
				gnt.put(id, value);
				return true;
			}
			// functions
			case Function: {
				final var argcount = this.stack.popTyped(IntPrimitive.class);
				final var code = this.stack.popTyped(CodeBlock.class).code;
				this.stack.push(new SOFunction(code, argcount.value()));
				return true;
			}
			case Constructor: {
				// similar to normal function code above
				final var argcount = this.stack.popTyped(IntPrimitive.class);
				final var code = this.stack.popTyped(CodeBlock.class).code;
				this.stack.push(new ConstructorFunction(code, argcount.value()));
				return true;
			}
			case Return: {
				final var retval = this.stack.pop();
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
					throw CompilerException.fromCurrentPosition(pt.getSource(), pt.getCodeIndex(), "module", null, moduleSpecifier);
				final var module = maybeModule.get();
				
				// dispatch module to a new interpreter that can handle `export` keywords
				final var moduleRunner = new ModuleInterpreter(this.io, this.moduleDiscoverer);
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
				this.stack.pop();
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
				this.io.print(this.stack.pop().print());
				return true;
			}
			case WriteLine: {
				this.io.println(this.stack.pop().print());
				return true;
			}
			// debug
			case DescribeElement: {
				this.io.debug(this.stack.peek().toDebugString(DebugStringExtensiveness.Full));
				return true;
			}
			case DescribeStack: {
				this.io.describeStack(this.stack);
				return true;
			}
			case Assert: {
				if (this.stack.pop().isFalse())
					throw CompilerException.fromCurrentPosition(pt.getSource(), pt.getCodeIndex(), "assert", null);
				++this.assertCount;
				return true;
			}
			default:
				throw new RuntimeException("Unhandled primitive token.");
		}
	}

	/**
	 * Executes a native call on this interpreter, using the given native function
	 * name as an SOF string. This may modify the stack.
	 * @param _fname The native function name, as an SOF string.
	 */
	protected void doNativeCall(Stackable _fname) {
		// typecheck and retrieve function
		if (!(_fname instanceof StringPrimitive))
			throw new CompilerException.Incomplete("type");
		final var fname = ((StringPrimitive) _fname).value();
		log.fine(() -> String.format("Native call function '%s'", fname));
		final var nativeFunc_ = NativeFunctionRegistry.getNativeFunction(fname);
		if (nativeFunc_.isEmpty())
			throw new CompilerException.Incomplete("native", "native.unknown", fname);
		final var nativeFunc = nativeFunc_.get();
		
		// switch over type
		if (nativeFunc instanceof Native0ArgFunction) {
			final var func = (Native0ArgFunction) nativeFunc;
			final var res = func.call();
			log.finer(() -> String.format("Native call 0 arg function returned %s", res.toDebugString(DebugStringExtensiveness.Compact)));
			if (res != null) this.stack.push(res);
		} else if (nativeFunc instanceof Native1ArgFunction) {
			final var func = (Native1ArgFunction) nativeFunc;
			final var arg0 = this.stack.pop();
			final var res = func.call(arg0);
			log.finer(() -> String.format("Native call 1 arg function returned %s", res.toDebugString(DebugStringExtensiveness.Compact)));
			if (res != null) this.stack.push(res);
		} else if (nativeFunc instanceof Native2ArgFunction) {
			final var func = (Native2ArgFunction) nativeFunc;
			final var arg1 = this.stack.pop();
			final var arg0 = this.stack.pop();
			final var res = func.call(arg0, arg1);
			log.finer(() -> String.format("Native call 2 arg function returned %s", res.toDebugString(DebugStringExtensiveness.Compact)));
			if (res != null) this.stack.push(res);
		} else if (nativeFunc instanceof Native3ArgFunction) {
			final var func = (Native3ArgFunction) nativeFunc;
			final var arg2 = this.stack.pop();
			final var arg1 = this.stack.pop();
			final var arg0 = this.stack.pop();
			final var res = func.call(arg0, arg1, arg2);
			log.finer(() -> String.format("Native call 3 arg function returned %s", res.toDebugString(DebugStringExtensiveness.Compact)));
			if (res != null) this.stack.push(res);
		}
	}

	/**
	 * Helper function to execute the call operation on the stackable, depending on
	 * the type. This function may modify the current interpreter state.
	 * 
	 * @param toCall The stackable that is to be called.
	 * @return Whether any of the subcalls encountered a return statement.
	 * This is necessary so that return statements propagate through CodeBlocks and are only caught by Functions.
	 */
	protected boolean doCall(final Stackable toCall) throws CompilerException.Incomplete {
		return this.doCall(toCall, new FunctionDelimiter());
	}

	/**
	 * Helper function to execute the call operation on the stackable, depending on
	 * the type. This function may modify the current interpreter state.
	 * 
	 * @param toCall The stackable that is to be called.
	 * @param scope  The nametable that should act as the surrounding scope for the call.
	 *                Note that some call types, such as identifiers, do not use a scope.
	 * @return Whether any of the subcalls encountered a return statement.
	 * This is necessary so that return statements propagate through CodeBlocks and are only caught by Functions.
	 */
	protected boolean doCall(final Stackable toCall, final Nametable scope) throws CompilerException.Incomplete {
		if (toCall instanceof Identifier) {
			final var id = (Identifier) toCall;
			final var val = this.stack.lookup(id);
			if (val == null)
				throw new CompilerException.Incomplete("name", id);
			this.stack.push(val);
			return true;
		} else if (toCall instanceof Primitive) {
			this.stack.push(toCall);
			return true;
		} else if (toCall instanceof ConstructorFunction) {
			final var constructor = (ConstructorFunction)toCall;
			final var newObject = new SObject();
			// push the object nametable as a delimiter, then the object itself as a "self" argument to the method
			this.stack.push(newObject.getAttributes());
			this.stack.push(newObject);
			
			// run method and ignore state
			constructor.code.forEach((Function<Node, Boolean>) this::handle);
			
			// ignore return value. constructors can still return stuff, so that the user may define multi-purpose functions/constructors.
			final var table = this.stack.popFirstNametable()
					.orElseThrow(() -> new RuntimeException("Local nametable was removed unexpectedly."));
			
			// re-push the object (was removed by above operation)
			this.stack.push(newObject);
			return true;
		} else if (toCall instanceof SOFunction) {
			// HINT: handle the function before the codeblock because it inherits from it
			final var function = (SOFunction) toCall;
			final var subProgram = function.code;

			// setup stack
			// causes overflow issues with extremely large (> 2.5 million) argcounts, which
			// shouldn't happen.
			final var args = this.stack.pop((int) function.arguments);
			this.stack.push(scope);
			if (function.arguments > 0)
				this.stack.pushAll(args);

			// run and ignore return state
			subProgram.forEach((Function<Node, Boolean>) this::handle);

			// get return value through nametable
			final var table = this.stack.popFirstNametable()
					.orElseThrow(() -> new RuntimeException("Local nametable was removed unexpectedly."));
			if (!(table instanceof FunctionDelimiter))
				throw new RuntimeException("Unexpected nametable type " + table.getClass().toString());
			((FunctionDelimiter) table).pushReturnValue(this.stack);
			return true;
		} else if (toCall instanceof CodeBlock) {
			final var subProgram = ((CodeBlock) toCall).code;
			// just run, no return value, no stack protect
			return subProgram.forEach((Function<Node, Boolean>) this::handle);
		} else
			throw new CompilerException.Incomplete("call", "type.call", toCall.typename());
	}

	// #endregion Execution

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
