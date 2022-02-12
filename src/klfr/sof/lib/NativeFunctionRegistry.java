package klfr.sof.lib;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.nio.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.*;

import org.reflections.*;
import static org.reflections.scanners.Scanners.*;

import klfr.sof.lang.*;
import klfr.Tuple;
import klfr.sof.exceptions.*;

/**
 * Responsible for the native function system. Any SOF extension or native
 * function implementor uses this class to register native functions.
 */
public final class NativeFunctionRegistry {
	private static final Logger log = Logger.getLogger(NativeFunctionRegistry.class.getCanonicalName());

	/** The maximum number of parameters the native function system supports. */
	public static int MAX_PARAMETER_COUNT = 3;

	/**
	 * Dummy interface as a supertype for all the native function functional
	 * interfaces
	 */
	public static interface NativeNArgFunction {
		/**
		 * A dummy function that doesn't do anything.
		 */
		public static Native0ArgFunction dummy = () -> {
			return null;
		};
	}

	/**
	 * The signature for a native function of zero arguments.
	 */
	@FunctionalInterface
	public static interface Native0ArgFunction extends NativeNArgFunction {
		/**
		 * Call the native function.
		 * @return The result of the native function.
		 * @throws IncompleteCompilerException If the native function fails.
		 */
		public Stackable call() throws IncompleteCompilerException;
	}

	/**
	 * The signature for a native function of one argument.
	 */
	@FunctionalInterface
	public static interface Native1ArgFunction extends NativeNArgFunction {
		/**
		 * Call the native function.
		 * @param a The first argument to the native function.
		 * @return The result of the native function.
		 * @throws IncompleteCompilerException If the native function fails.
		 */
		public Stackable call(Stackable a) throws IncompleteCompilerException;
	}

	/**
	 * The signature for a native function of two arguments.
	 */
	@FunctionalInterface
	public static interface Native2ArgFunction extends NativeNArgFunction {
		/**
		 * Call the native function.
		 * @param a The first argument to the native function.
		 * @param b The second argument to the native function.
		 * @return The result of the native function.
		 * @throws IncompleteCompilerException If the native function fails.
		 */
		public Stackable call(Stackable a, Stackable b) throws IncompleteCompilerException;
	}

	/**
	 * The signature for a native function of three arguments.
	 */
	@FunctionalInterface
	public static interface Native3ArgFunction extends NativeNArgFunction {
		/**
		 * Call the native function.
		 * @param a The first argument to the native function.
		 * @param b The second argument to the native function.
		 * @param c The third argument to the native function.
		 * @return The result of the native function.
		 * @throws IncompleteCompilerException If the native function fails.
		 */
		public Stackable call(Stackable a, Stackable b, Stackable c) throws IncompleteCompilerException;
	}

	private TreeMap<String, NativeNArgFunction> nativeFunctions = new TreeMap<>();

	/**
	 * Loads all native functions in the specified package.<br/>
	 * <br/>
	 * 
	 * This method will search through all classes in this package (including
	 * subpackages) and load native functions from native function collection
	 * classes. These are classes annotated with
	 * {@link klfr.sof.lib.NativeFunctionCollection}.
	 * 
	 * @param packageName The package name. Its subpackages are searched as well.
	 * @throws IOException  If any exception occurs, also in the class discovery an
	 *                      reflection access process.
	 */
	public void registerAllFromPackage(String packageName) throws IOException {
		Reflections pakage = new Reflections(packageName);
		var classes = pakage.get(SubTypes.of(TypesAnnotated.with(NativeFunctionCollection.class)).asClass());;
		log.fine(String.format("In package %s found classes: %s", packageName, classes.toString()));

		for (var clazz : classes) {
			this.registerNativeFunctions(clazz);
		}
	}

	/**
	 * <p>
	 * Register all native functions of the provided class.
	 * </p>
	 * 
	 * <p>
	 * This method uses reflection to find all methods on the class that can be used
	 * as native functions. A method that is to be registered as a native function
	 * must satisfy the following conditions:
	 * </p>
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
	 * Classes which are not accessible to this class due to visibility restrictions
	 * will cause reflection errors to be thrown. Inacessible classes should not be
	 * passed to this method.
	 * 
	 * @param clazz The class to search for native functions. All suitable methods
	 *              of this class are registered, whether the caller intended them
	 *              to be registered or not.
	 */
	public void registerNativeFunctions(Class<?> clazz) {
		// FP for da win
		Arrays.stream(clazz.getDeclaredMethods())
				// only public static methods, only methods without too many parameters
				.filter(m -> Modifier.isStatic(m.getModifiers()) && Modifier.isPublic(m.getModifiers())
						&& m.getParameterCount() <= MAX_PARAMETER_COUNT)
				// only methods with only Stackable or Stackable subtype parameters
				.filter(
						m -> Arrays.stream(m.getParameterTypes()).allMatch(ptype -> Stackable.class.isAssignableFrom(ptype)))
				// only methods with Stackable return type (or no return value)
				.filter(m -> Stackable.class.isAssignableFrom(m.getReturnType()) || (m.getReturnType() == void.class))
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
	public Optional<NativeNArgFunction> getNativeFunction(String fidentifier) {
		return Optional.ofNullable(nativeFunctions.getOrDefault(fidentifier, null));
	}

	/**
	 * This lambda function is only externalized to make Java recognize the return
	 * type properly. -_-
	 * 
	 * Maps a given map entry with a number of arguments and its respective methods
	 * to tuples that contain the method call wrapper to the left and the original method to the right.
	 */
	private static Stream<Tuple<NativeNArgFunction, Method>> methodRegistrationFlatMapFtor(
			final Map.Entry<Integer, List<Method>> entry) {
		final var argcount = entry.getKey();
		final var functions = entry.getValue().stream();
		// Yes.
		return switch (argcount) {
			case 0 -> functions.map(m -> new Tuple<>(mcall0(m), m));
			case 1 -> functions.map(m -> new Tuple<>(mcall1(m), m));
			case 2 -> functions.map(m -> new Tuple<>(mcall2(m), m));
			case 3 -> functions.map(m -> new Tuple<>(mcall3(m), m));
			default -> Stream.<Tuple<NativeNArgFunction, Method>>empty();
		};
		
	}

	/**
	 * Wrapper for 0 argument native function call that redirects all invocation
	 * errors to CompilerException.
	 */
	private static Native0ArgFunction mcall0(Method function) {
		return () -> {
			try {
				return (Stackable) function.invoke(null);
			} catch (IllegalAccessException | IllegalArgumentException
					| ExceptionInInitializerError e) {
				final var ce = new IncompleteCompilerException("native");
				ce.initCause(e);
				throw ce;
			} catch (InvocationTargetException e) {
				final var cause = e.getCause();
				if (cause instanceof IncompleteCompilerException compilerException)
					throw compilerException;
				final var ce = new IncompleteCompilerException("native");
				ce.initCause(e);
				throw ce;
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
			} catch (IllegalAccessException | IllegalArgumentException
					| ExceptionInInitializerError e) {
				final var ce = new IncompleteCompilerException("native");
				ce.initCause(e);
				throw ce;
			} catch (InvocationTargetException e) {
				final var cause = e.getCause();
				if (cause instanceof IncompleteCompilerException compilerException)
					throw compilerException;
				final var ce = new IncompleteCompilerException("native");
				ce.initCause(e);
				throw ce;
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
			} catch (IllegalAccessException | IllegalArgumentException
					| ExceptionInInitializerError e) {
				final var ce = new IncompleteCompilerException("native");
				ce.initCause(e);
				throw ce;
			} catch (InvocationTargetException e) {
				final var cause = e.getCause();
				if (cause instanceof IncompleteCompilerException compilerException)
					throw compilerException;
				final var ce = new IncompleteCompilerException("native");
				ce.initCause(e);
				throw ce;
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
			} catch (IllegalAccessException | IllegalArgumentException
					| ExceptionInInitializerError e) {
				final var ce = new IncompleteCompilerException("native");
				ce.initCause(e);
				throw ce;
			} catch (InvocationTargetException e) {
				final var cause = e.getCause();
				if (cause instanceof IncompleteCompilerException compilerException)
					throw compilerException;
				final var ce = new IncompleteCompilerException("native");
				ce.initCause(e);
				throw ce;
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
		final var arguments = Arrays.stream(function.getParameterTypes()).map(pt -> pt.getSimpleName()).collect(Collectors.joining(","));
		return new StringBuilder(packageName).append(".").append(className).append("#").append(methodName).append("(")
				.append(arguments).append(")").toString();
	}

}

/*
The SOF programming language interpreter. Copyright (C) 2019-2020
kleinesfilmröllchen

This program is free software: you can redistribute it and/or modify it under
the terms of the GNU General Public License as published by the Free Software
Foundation, either version 3 of the License, or (at your option) any later
version.

This program is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
details.

You should have received a copy of the GNU General Public License along with
this program. If not, see <https://www.gnu.org/licenses/>.
*/
