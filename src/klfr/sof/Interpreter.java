package klfr.sof;

import java.util.logging.*;
import java.io.Serializable;
import java.util.*;
import java.util.function.*;

import klfr.sof.ast.*;
import klfr.sof.lang.*;
import klfr.sof.lang.Stack;
import klfr.sof.lang.Stackable.DebugStringExtensiveness;
import klfr.sof.lib.*;
import klfr.sof.lib.NativeFunctionRegistry.*;

/**
 * 
 */
public class Interpreter implements Serializable {
	// #region Globals
	/** Major version of the interpreter. */
	public static final int MAJOR_VERSION = 0;
	/** Minor version of the interpreter. */
	public static final int MINOR_VERSION = 1;
	/** Bug & security fix version of the interpreter. */
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

	/**
	 * Returns the number of asserts that were successfully performed by this
	 * interpreter.
	 */
	public int getAssertCount() {
		return assertCount;
	}

	// #endregion

	public Interpreter(IOInterface io) {
		this.io = io;
		this.stack = new Stack();
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
	public Interpreter run(Node program, String currentCode) throws CompilerException {
		synchronized (this) {
			log.entering(Interpreter.class.getCanonicalName(), "run # synchronized");
			program.forEach((Function<Node, Boolean>) this::handle);
		}
		log.exiting(Interpreter.class.getCanonicalName(), "run");
		return this;
	}

	/**
	 * Callback for handling a node.
	 */
	private boolean handle(Node n) throws CompilerException {
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
			throw CompilerException.fromIncomplete(n.getCode(), n.getCodeIndex(), incomplete);
		}
	}

	private boolean handle(TokenListNode codeblock) throws CompilerException {
		this.stack.push(new CodeBlock(codeblock));
		return true;
	}

	private boolean handle(LiteralNode literal) throws CompilerException {
		this.stack.push(literal.getValue());
		return true;
	}

	/**
	 * Primitive token handler; takes care of much of the central logic.
	 * 
	 * @param pt The primitive token to execute.
	 */
	private boolean handle(PrimitiveTokenNode pt) throws CompilerException {
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
				break;
			}
			case Subtract: {
				final Stackable rhs = this.stack.pop(), lhs = this.stack.pop();
				this.stack.push(BuiltinPTs.subtract(lhs, rhs));
				break;
			}
			case Multiply: {
				final Stackable rhs = this.stack.pop(), lhs = this.stack.pop();
				this.stack.push(BuiltinPTs.multiply(lhs, rhs));
				break;
			}
			case Divide: {
				final Stackable rhs = this.stack.pop(), lhs = this.stack.pop();
				this.stack.push(BuiltinPTs.divide(lhs, rhs));
				break;
			}
			case And: {
				final Stackable rhs = this.stack.pop(), lhs = this.stack.pop();
				this.stack.push(BuiltinPTs.logicalAnd(lhs, rhs));
				break;
			}
			case Or: {
				final Stackable rhs = this.stack.pop(), lhs = this.stack.pop();
				this.stack.push(BuiltinPTs.logicalOr(lhs, rhs));
				break;
			}
			case ExclusiveOr: {
				final Stackable rhs = this.stack.pop(), lhs = this.stack.pop();
				this.stack.push(BuiltinPTs.logicalXor(lhs, rhs));
				break;
			}
			case Not: {
				final var val = this.stack.pop();
				this.stack.push(BoolPrimitive.createBoolPrimitive(val.isFalse()));
				break;
			}
			// comparison and equality
			case Equals: {
				final Stackable rhs = this.stack.pop(), lhs = this.stack.pop();
				this.stack.push(BuiltinPTs.equals(lhs, rhs));
				break;
			}
			case NotEquals: {
				final Stackable rhs = this.stack.pop(), lhs = this.stack.pop();
				this.stack.push(BuiltinPTs.notEquals(lhs, rhs));
				break;
			}
			case GreaterThan: {
				final Stackable rhs = this.stack.pop(), lhs = this.stack.pop();
				this.stack.push(BuiltinPTs.greaterThan(lhs, rhs));
				break;
			}
			case GreaterThanEquals: {
				final Stackable rhs = this.stack.pop(), lhs = this.stack.pop();
				this.stack.push(BuiltinPTs.greaterEqualThan(lhs, rhs));
				break;
			}
			case LessThan: {
				final Stackable rhs = this.stack.pop(), lhs = this.stack.pop();
				this.stack.push(BuiltinPTs.lessThan(lhs, rhs));
				break;
			}
			case LessThanEquals: {
				final Stackable rhs = this.stack.pop(), lhs = this.stack.pop();
				this.stack.push(BuiltinPTs.lessEqualThan(lhs, rhs));
				break;
			}
			// stack operations
			case Discard: {
				this.stack.pop();
				break;
			}
			case Duplicate: {
				final var a = this.stack.pop();
				this.stack.push(a);
				this.stack.push(a);
				break;
			}
			case Swap: {
				final var eltop = this.stack.pop();
				final var elbot = this.stack.pop();
				this.stack.push(eltop);
				this.stack.push(elbot);
				break;
			}
			// conditionals and loops
			case If: {
				final var cond = this.stack.pop();
				final var callable = this.stack.pop();
				if (cond.isTrue())
					this.doCall(callable);
				break;
			}
			case IfElse: {
				final var elseCallable = this.stack.pop();
				final var cond = this.stack.pop();
				final var callable = this.stack.pop();
				this.doCall(cond.isTrue() ? callable : elseCallable);
				break;
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
						this.doCall(body);
						// remove elements until identifier "switch"
						var elt = this.stack.pop();
						while (!(elt instanceof Identifier && ((Identifier) elt).getValue().equals("switch::")))
							elt = this.stack.pop();
						break;
					} else {
						final var elt = this.stack.pop();
						if (elt instanceof Identifier && ((Identifier) elt).getValue().equals("switch::")) {
							// switch end was reached without executing any case: execute default callable
							this.doCall(defaultCallable);
							break;
						} else {
							// just another pair of case and body; do that in the next loop
							this.stack.push(elt);
						}
					}
				}
				break;
			}
			case While: {
				final var condCallable = this.stack.pop();
				final var bodyCallable = this.stack.pop();
				while (true) {
					// execute the condition
					this.doCall(condCallable);
					var preContinue = this.stack.pop();
					if (preContinue.isTrue())
						this.doCall(bodyCallable);
					else
						break;
				}
				break;
			}
			// naming and calling
			case Call: {
				final Stackable toCall = this.stack.pop();
				this.doCall(toCall);
				break;
			}
			case DoubleCall: {
				this.doCall(this.stack.pop());
				this.doCall(this.stack.pop());
				break;
			}
			case NativeCall: {
				this.doNativeCall(this.stack.pop());
				break;
			}
			case Define: {
				final var id = this.stack.popTyped(Identifier.class);
				final var value = this.stack.pop();
				this.stack.localScope().put(id, value);
				break;
			}
			case GlobalDefine: {
				final var id = this.stack.popTyped(Identifier.class);
				final var value = this.stack.pop();
				final var gnt = this.stack.globalNametable();
				gnt.put(id, value);
				break;
			}
			// functions
			case Function: {
				final var argcount = this.stack.popTyped(IntPrimitive.class);
				final var code = this.stack.popTyped(CodeBlock.class).code;
				this.stack.push(new SOFunction(code, argcount.value()));
				break;
			}
			case Return: {
				final var retval = this.stack.pop();
				this.stack.localScope().setReturn(retval);
				return false;
			}
			// i/o
			case Input: {
				final String input = this.io.nextInputSequence();
				this.stack.push(StringPrimitive.createStringPrimitive(input));
				break;
			}
			case InputLine: {
				final String input = this.io.nextInputLine();
				this.stack.push(StringPrimitive.createStringPrimitive(input));
				break;
			}
			case Write: {
				this.io.print(this.stack.pop().print());
				break;
			}
			case WriteLine: {
				this.io.println(this.stack.pop().print());
				break;
			}
			// debug
			case DescribeElement: {
				this.io.debug(this.stack.peek().toDebugString(DebugStringExtensiveness.Full));
				break;
			}
			case DescribeStack: {
				this.io.describeStack(this.stack);
				break;
			}
			case Assert: {
				if (this.stack.pop().isFalse())
					throw CompilerException.fromCurrentPosition(pt.getCode(), pt.getCodeIndex(), "assert", null);
				++this.assertCount;
				break;
			}
			default:
				throw new RuntimeException("Unhandled primitive token.");
		}
		return true;
	}

	/**
	 * Executes a native call on this interpreter, using the given native function
	 * name as an SOF string. This may modify the stack.
	 * @param _fname The native function name, as an SOF string.
	 */
	private void doNativeCall(Stackable _fname) {
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
	 */
	private void doCall(final Stackable toCall) throws CompilerException.Incomplete {
		if (toCall instanceof Identifier) {
			final var id = (Identifier) toCall;
			final var val = this.stack.lookup(id);
			if (val == null)
				throw new CompilerException.Incomplete("name", id);
			this.stack.push(val);
		} else if (toCall instanceof Primitive) {
			this.stack.push(toCall);
		} else if (toCall instanceof SOFunction) {
			// HINT: handle the function before the codeblock because it inherits from it
			final var function = (SOFunction) toCall;
			final var subProgram = function.code;

			// setup stack
			// causes overflow issues with extremely large (> 2.5 million) argcounts, which
			// shouldn't happen.
			final var args = this.stack.pop((int) function.arguments);
			this.stack.push(new FunctionDelimiter());
			if (function.arguments > 0)
				this.stack.pushAll(args);

			// run
			subProgram.forEach((Function<Node, Boolean>) this::handle);

			// get return value through nametable
			final var table = this.stack.popFirstNametable()
					.orElseThrow(() -> new RuntimeException("Local nametable was removed unexpectedly."));
			if (!(table instanceof FunctionDelimiter))
				throw new RuntimeException("Unexpected nametable type " + table.getClass().toString());
			((FunctionDelimiter) table).pushReturnValue(this.stack);
		} else if (toCall instanceof CodeBlock) {
			final var subProgram = ((CodeBlock) toCall).code;
			// just run, no return value, no stack protect
			subProgram.forEach((Function<Node, Boolean>) this::handle);
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
