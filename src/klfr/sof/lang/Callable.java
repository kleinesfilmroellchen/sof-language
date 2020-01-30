package klfr.sof.lang;

import klfr.sof.Interpreter;

/**
 * A special type of Stackable that is also callable and provides a CallProvider
 * Object which executes the action that the callable represents. Common
 * implementors include functions, code blocks and primitive values (which have
 * constant-value call methods)
 * 
 * @author klfr
 */
public interface Callable extends Stackable {

	/**
	 * This functional interface provides the actual method to be called when
	 * executing the callable. This makes it possible for internal and external
	 * users to construct Callables with Lambda expressions.
	 * 
	 * @author klfr
	 */
	@FunctionalInterface
	public interface CallProvider {
		/**
		 * Executes the callable's action. As many callables depend on SOF language
		 * interpretation or access to SOF code, the calling interpreter needs to
		 * provide itself as a tool for executing the callable.
		 * 
		 * @return The stackable that is the result of the call, as callables are mainly
		 *         used to retrieve values.
		 */
		public Stackable call(Interpreter parent);
	}

	/**
	 * Accessor for the call provider function that executes the actual action. This
	 * interface allows for the CallProvider to be created dynamically upon
	 * invocation of this method. The implementor should, however, be aware that
	 * this method is usually only invoked once when using the Callable.
	 */
	public CallProvider getCallProvider();
}
