package klfr.sof.lib;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

import klfr.sof.lang.*;
import klfr.sof.*;

/**
 * Responsible for the native function system. Any SOF extension or native
 * function implementor uses this class to register native functions.
 */
public final class NativeFunctionRegistry {

	/** The maximum number of parameters the native function system supports. */
	public static int MAX_PARAMETER_COUNT = 3;

	@FunctionalInterface
	public interface Native0ArgFunction {
		public Stackable call();
	}

	@FunctionalInterface
	public interface Native1ArgFunction {
		public Stackable call(Stackable a);
	}

	@FunctionalInterface
	public interface Native2ArgFunction {
		public Stackable call(Stackable a, Stackable b);
	}

	@FunctionalInterface
	public interface Native3ArgFunction {
		public Stackable call(Stackable a, Stackable b, Stackable c);
	}

	private static TreeMap<String, Native0ArgFunction> functions0args = new TreeMap<>();
	private static TreeMap<String, Native1ArgFunction> functions1args = new TreeMap<>();
	private static TreeMap<String, Native2ArgFunction> functions2args = new TreeMap<>();
	private static TreeMap<String, Native3ArgFunction> functions3args = new TreeMap<>();

	public static void registerNativeFunctions(Class<?> clazz) {
		// FP for da win
		Arrays.stream(clazz.getMethods())
				// only static methods
				.filter(m -> Modifier.isStatic(m.getModifiers()) && Modifier.isPublic(m.getModifiers()))
				// only methods without too many parameters
				.filter(m -> m.getParameterCount() <= MAX_PARAMETER_COUNT)
				// only methods with only Stackable or Stackable subtype parameters
				.filter(
						m -> Arrays.stream(m.getParameterTypes()).allMatch(ptype -> Stackable.class.isAssignableFrom(ptype)))
				.filter(m -> Stackable.class.isAssignableFrom(m.getReturnType()))
				// group by parameter count
				.collect(Collectors.groupingByConcurrent(Method::getParameterCount))
				// add the methods to the function registry
				.forEach((argcount, functions) -> {
					switch (argcount) {
						case 0:
							functions.forEach(function -> functions0args.put(generateDescriptor(function), mcall0(function)));
							break;
					}
				});
	}

	private static Native0ArgFunction mcall0(Method function) {
		// TODO: COMEBAK
		return null;
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
		final var arguments = String.join( ",", (String[])Arrays.stream(function.getParameterTypes()).map(pt -> pt.getSimpleName()).toArray());
		return new StringBuilder(packageName).append(".").append(className).append("#").append(methodName).append("(").append(arguments).append(")").toString();
	}

}