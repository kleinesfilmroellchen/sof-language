package klfr.sof;

import java.util.logging.*;
import java.io.Serializable;
import java.util.*;

import klfr.sof.ast.*;
import klfr.sof.lang.*;
import klfr.sof.lang.Stack;

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
	public static final ResourceBundle R = ResourceBundle.getBundle(MESSAGE_RESOURCE, Locale.GERMAN);

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
	protected IOInterface io;

	/**
	 * All of the program memory, which is only a simple stack due to SOF's strict
	 * stack-based nature.
	 */
	protected Stack stack;

	// #endregion

	/**
	 * Run a parsed SOF program.
	 */
	public void run(TokenListNode program) throws CompilerException {
		program.forEach(this::handle);
	}

	/**
	 * Callback for handling a node.
	 */
	private void handle(Node n) throws CompilerException {
		// manual dynamic dispatch -- there isn't a better reflection-free way
		if (n instanceof TokenListNode)
			handle((TokenListNode) n);
		else if (n instanceof LiteralNode)
			handle((LiteralNode) n);
		else if (n instanceof PrimitiveTokenNode)
			handle((PrimitiveTokenNode) n);
		else
			throw new RuntimeException("Unknown node type.");
	}

	private void handle(TokenListNode codeblock) throws CompilerException {
		this.stack.push(new CodeBlock(codeblock));
	}

	private void handle(LiteralNode literal) throws CompilerException {
		this.stack.push(literal.getValue());
	}

	/**
	 * Primitive token handler; takes care of much of the central logic.
	 * 
	 * @param pt The primitive token to execute.
	 */
	private void handle(PrimitiveTokenNode pt) throws CompilerException {
		// BEHOLD THE SWITCH CASE OF DOOM
		// if the switch case is optimized to jump table lookups, this may be the
		// fastest way of handling all of the primitive tokens selectively
		switch (pt) {
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
				final var val = this.stack.popTyped(BoolPrimitive.class);
				this.stack.push(BoolPrimitive.createBoolPrimitive(!val.value()));
				break;
			}
			case Assert: {
				break;
			}
			case Call: {
				break;
			}
			case Define: {
				break;
			}
			case DescribeElement: {
				break;
			}
			case DescribeStack: {
				break;
			}
			case Discard: {
				break;
			}
			case DoubleCall: {
				break;
			}
			case Duplicate: {
				break;
			}
			case Equals: {
				break;
			}
			case GlobalDefine: {
				break;
			}
			case GreaterThan: {
				break;
			}
			case GreaterThanEquals: {
				break;
			}
			case If: {
				break;
			}
			case IfElse: {
				break;
			}
			case Input: {
				break;
			}
			case InputLine: {
				break;
			}
			case LessThan: {
				break;
			}
			case LessThanEquals: {
				break;
			}
			case NotEquals: {
				break;
			}
			case Swap: {
				break;
			}
			case Switch: {
				break;
			}
			case While: {
				break;
			}
			case Write: {
				break;
			}
			case WriteLine: {
				break;
			}
			default:
				throw new RuntimeException("Unhandled primitive token.");
		}
	}

	/**
	 * Helper function to execute call and push return value of call to stack if
	 * possible.
	 */
	private void doCall(final Stackable reference) {
		
		final Stackable retval = null;
		if (retval != null)
			this.stack.push(retval);
	}

}