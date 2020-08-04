package klfr.sof.lib;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import klfr.sof.lang.*;
import klfr.Tuple;
import klfr.sof.*;

/**
 * Responsible for the native function system. Any SOF extension or native
 * function implementor uses this class to register native functions.
 */
public final class NativeFunctionRegistry {

	/** The maximum number of parameters the native function system supports. */
	public static int MAX_PARAMETER_COUNT = 3;

	/**
	 * Dummy interface as a supertype for all the native function functional
	 * interfaces
	 */
	public static interface NativeNArgFunction {
		public static Native0ArgFunction dummy = () -> {
			return null;
		};
	}

	@FunctionalInterface
	public static interface Native0ArgFunction extends NativeNArgFunction {
		public Stackable call();
	}

	@FunctionalInterface
	public static interface Native1ArgFunction extends NativeNArgFunction {
		public Stackable call(Stackable a);
	}

	@FunctionalInterface
	public static interface Native2ArgFunction extends NativeNArgFunction {
		public Stackable call(Stackable a, Stackable b);
	}

	@FunctionalInterface
	public static interface Native3ArgFunction extends NativeNArgFunction {
		public Stackable call(Stackable a, Stackable b, Stackable c);
	}

	private static TreeMap<String, NativeNArgFunction> nativeFunctions = new TreeMap<>();

	/**
	 * Register all native functions of the provided class.
	 * <p>
	 * This method uses reflection to find all methods on the class that can be used
	 * as native functions. A method that is to be registered as a native function
	 * must satisfy the following conditions:
	 * <ul>
	 * <li>It must be public and static. An easy way of preventing a method from
	 * being registered is making it private or an instance method.</li>
	 * <li>Its argument types and its return type must be
	 * {@link klfr.sof.lang.Stackable} or a subclass thereof. It must not have a
	 * void return type.</li>
	 * <li>It must not have more than
	 * {@link NativeFunctionRegistry#MAX_PARAMETER_COUNT} arguments. It must also
	 * not have any vararg parameters.</li>
	 * </ul>
	 * </p>
	 * Classes which are not accessible to this class due to visibility restrictions
	 * will cause reflection errors to be thrown. Inacessible classes should not be
	 * passed to this method.
	 * 
	 * @param clazz The class to search for native functions. All suitable methods
	 *              of this class are registered, whether the caller intended them
	 *              to be registered or not.
	 */
	public static void registerNativeFunctions(Class<?> clazz) {
		// FP for da win
		Arrays.stream(clazz.getMethods())
				// only public static methods, only methods without too many parameters
				.filter(m -> Modifier.isStatic(m.getModifiers()) && Modifier.isPublic(m.getModifiers())
						&& m.getParameterCount() <= MAX_PARAMETER_COUNT)
				// only methods with only Stackable or Stackable subtype parameters
				.filter(
						m -> Arrays.stream(m.getParameterTypes()).allMatch(ptype -> Stackable.class.isAssignableFrom(ptype)))
				// only methods with Stackable return type
				.filter(m -> Stackable.class.isAssignableFrom(m.getReturnType()))
				// group by parameter count
				.collect(Collectors.groupingByConcurrent(Method::getParameterCount))
				// map the methods to one of the mcallN a proxies and flatten streams
				.entrySet().parallelStream().flatMap(NativeFunctionRegistry::methodRegistrationFlatMapFtor)
				// add the methods to the function registry
				.forEach(ftuple -> nativeFunctions.put(generateDescriptor(ftuple.getRight()), ftuple.getLeft()));
	}

	/**
	 * Return the native function associated with the given standard native function
	 * identifier string, which the SOF source code originally provided.
	 * 
	 * @param fidentifier The standard native function identifier string, as e.g.
	 *                    the nativecall SOF PT expects it.
	 * @return The corresponding native function, or a dummy zero argument function
	 *         does nothing and returns null if the specified native function was
	 *         not found.
	 */
	public static NativeNArgFunction getNativeFunction(String fidentifier) {
		return nativeFunctions.getOrDefault(fidentifier, NativeNArgFunction.dummy);
	}

	/**
	 * This lambda function is only externalized to make Java recognize the return
	 * type properly. -_-
	 */
	private static Stream<Tuple<NativeNArgFunction, Method>> methodRegistrationFlatMapFtor(
			final Map.Entry<Integer, List<Method>> entry) {
		final var argcount = entry.getKey();
		final var functions = entry.getValue().stream();
		switch (argcount) {
			case 0:
				return functions.map(m -> new Tuple<>(mcall0(m), m));
			case 1:
				return functions.map(m -> new Tuple<>(mcall1(m), m));
			case 2:
				return functions.map(m -> new Tuple<>(mcall2(m), m));
			case 3:
				return functions.map(m -> new Tuple<>(mcall3(m), m));
		}
		return Stream.<Tuple<NativeNArgFunction, Method>>empty();
	}

	/**
	 * Wrapper for 0 argument native function call that redirects all invocation
	 * errors to CompilerException.
	 */
	private static Native0ArgFunction mcall0(Method function) {
		return () -> {
			try {
				return (Stackable) function.invoke(null);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
					| ExceptionInInitializerError e) {
				throw new CompilerException.Incomplete(null, "native");
			}
		};
	}

	/**
	 * Wrapper for 1 argument native function call that redirects all invocation
	 * errors to CompilerException.
	 */
	private static Native1ArgFunction mcall1(Method function) {
		return (a) -> {
			try {
				return (Stackable) function.invoke(null, a);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
					| ExceptionInInitializerError e) {
				throw new CompilerException.Incomplete(null, "native");
			}
		};
	}

	/**
	 * Wrapper for 2 argument native function call that redirects all invocation
	 * errors to CompilerException.
	 */
	private static Native2ArgFunction mcall2(Method function) {
		return (a, b) -> {
			try {
				return (Stackable) function.invoke(null, a, b);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
					| ExceptionInInitializerError e) {
				throw new CompilerException.Incomplete(null, "native");
			}
		};
	}

	/**
	 * Wrapper for 3 argument native function call that redirects all invocation
	 * errors to CompilerException.
	 */
	private static Native3ArgFunction mcall3(Method function) {
		return (a, b, c) -> {
			try {
				return (Stackable) function.invoke(null, a, b, c);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
					| ExceptionInInitializerError e) {
				throw new CompilerException.Incomplete(null, "native");
			}
		};
	}

	/**
	 * Generates a standard SOF native function descriptor for the given method,
	 * depending on its location in the package and class hierarchy, its name and
	 * its argument types and count.
	 * 
	 * @param function The Method runtime reference which to generate the descriptor
	 *                 for.
	 * @return A string that contains the standard SOF descriptor uniquely
	 *         referencing the method.
	 */
	public static String generateDescriptor(Method function) {
		final String className = function.getDeclaringClass().getSimpleName(), methodName = function.getName(),
				packageName = function.getDeclaringClass().getPackageName();
		final var arguments = String.join(",",
				(String[]) Arrays.stream(function.getParameterTypes()).map(pt -> pt.getSimpleName()).toArray());
		return new StringBuilder(packageName).append(".").append(className).append("#").append(methodName).append("(")
				.append(arguments).append(")").toString();
	}

}