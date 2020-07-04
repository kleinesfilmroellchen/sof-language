package klfr.sof;

import static klfr.sof.Interpreter.InterpreterAction;

import java.util.Optional;

/**
 * This functional interface defines handlers for SOF tokens. It is used in
 * conjunction with the {@link Interpreter#registerTokenHandler(TokenHandler)}
 * interface method to define token handlers that are used by the Interpreter.
 * <br>
 * <br>
 * The functional interface's target method is
 * {@link TokenHandler#handle(String)}.
 */
@FunctionalInterface
public interface TokenHandler {
	/**
	 * Handle a given string token. This is where the token handler decides on
	 * whether the token needs to be handled, and if so, provide an interpreter
	 * action. If the handler decides that it cannot handle the token, it returns an
	 * empty optional. This method allows flexibility in token handling, such as
	 * providing different actions depending on the token, and the deferral of the
	 * actual interpreter-manipulating logic allows for its optimized execution by
	 * interpreters.
	 * 
	 * @param token The string token to be handled.
	 * @return Either an empty optional, if this token handler cannot or doesn't
	 *         want to handle the token; or an optional containing an
	 *         InterpreterAction to be executed on the interpreter that represents
	 *         the logic associated with the token. InterpreterAction callers
	 *         guarantee that in the first case, other TokenHandlers may recieve the
	 *         token in the context of the same Interpreter state, and in the second
	 *         case, no other TokenHandler recieves the token in the context of the
	 *         same Interpreter state.
	 * @see Interpreter.InterpreterAction
	 */
	public Optional<InterpreterAction> handle(final String token);
}