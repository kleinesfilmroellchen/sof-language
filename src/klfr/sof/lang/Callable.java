package klfr.sof.lang;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import klfr.sof.Interpreter;

/**
 * A special type of Stackable that is also callable and provides a CallProvider
 * Object which executes the action that the callable represents. Common
 * implementors include functions, code blocks and primitive values (which have
 * constant-value call methods)
 * 
 * @author klfr
 */
@StackableName("Callable")
public interface Callable extends Stackable {

	/**
	 * This functional interface provides the actual method to be called when
	 * executing the callable. This makes it possible for internal and external
	 * users to construct Callables with Lambda expressions.
	 * 
	 * @author klfr
	 */
	@FunctionalInterface
	public static interface CallProvider {
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

	public default Stackable copy() {
		return this;
	}

	/**
	 * Create a simple Callable from a Stackable -> Stackable function. The callable
	 * pops one argument and pushes the result.
	 * 
	 * @param f The function to construct the callable from.
	 * @return A callable whose call provider pops one argument, passes that to the
	 *         function and pushes the result of the function.
	 */
	public static Callable fromFunction(Function<? super Stackable, ? extends Stackable> f) {
		return new Callable() {
			private static final long serialVersionUID = 1L;

			@Override
			public String toDebugString(DebugStringExtensiveness e) {
				switch (e) {
					case Type:
						return "NativCbl'1";
					default:
						return "NativeCallable'1:" + f.toString();
				}
			}

			@Override
			public CallProvider getCallProvider() {
				return self -> {
					final var stack = self.internal.stack();
					final var arg = stack.pop();
					return f.apply(arg);
				};
			}
		};
	}

	/**
	 * Create a simple Callable from a Stackable -> Stackable -> Stackable function.
	 * The callable pops two arguments and pushes the result.
	 * 
	 * @param f The function to construct the callable from. First argument is lower
	 *          on the stack.
	 * @return A callable whose call provider pops two arguments, passes them to the
	 *         function and pushes the result of the function.
	 */
	public static Callable fromFunction(BiFunction<? super Stackable, ? super Stackable, ? extends Stackable> f) {
		return new Callable() {
			private static final long serialVersionUID = 1L;

			@Override
			public String toDebugString(DebugStringExtensiveness e) {
				switch (e) {
					case Type:
						return "NativCbl'2";
					default:
						return "NativeCallable'2:" + f.toString();
				}
			}

			@Override
			public CallProvider getCallProvider() {
				return self -> {
					final var stack = self.internal.stack();
					final var arg1 = stack.pop();
					final var arg2 = stack.pop();
					return f.apply(arg2, arg1);
				};
			}
		};
	}

	/**
	 * Create a simple Callable from a Stackable -> Stackable -> Stackable ->
	 * Stackable function. The callable pops three arguments and pushes the result.
	 * 
	 * @param f The function to construct the callable from. First argument is
	 *          lowest on the stack.
	 * @return A callable whose call provider pops three arguments, passes them to
	 *         the function and pushes the result of the function.
	 */
	public static Callable fromFunction(
			TriFunction<? super Stackable, ? super Stackable, ? super Stackable, ? extends Stackable> f) {
		return new Callable() {
			private static final long serialVersionUID = 1L;

			@Override
			public String toDebugString(DebugStringExtensiveness e) {
				switch (e) {
					case Type:
						return "NativCbl'3";
					default:
						return "NativeCallable'3:" + f.toString();
				}
			}

			@Override
			public CallProvider getCallProvider() {
				return self -> {
					final var stack = self.internal.stack();
					final var arg1 = stack.pop();
					final var arg2 = stack.pop();
					final var arg3 = stack.pop();
					return f.apply(arg3, arg2, arg1);
				};
			}
		};
	}

	/**
	 * Create a simple Callable from a Stackable supplier function. The callable
	 * pushes the result.
	 * 
	 * @param f The function to construct the callable from.
	 * @return A callable whose call provider calls the function and pushes its
	 *         result.
	 */
	public static Callable fromFunction(Supplier<? extends Stackable> f) {
		return new Callable() {
			private static final long serialVersionUID = 1L;

			@Override
			public String toDebugString(DebugStringExtensiveness e) {
				switch (e) {
					case Type:
						return "NativCbl'0";
					default:
						return "NativeCallable'0:" + f.toString();
				}
			}

			@Override
			public CallProvider getCallProvider() {
				return self -> {
					return f.get();
				};
			}
		};
	}

	@FunctionalInterface
	public static interface TriFunction<T, U, V, R> {
		public R apply(T t, U u, V v);

		public default <Q> TriFunction<T, U, V, Q> andThen(Function<? super R, ? extends Q> f) {
			return (a, b, c) -> f.apply(this.apply(a, b, c));
		}
	}
}
