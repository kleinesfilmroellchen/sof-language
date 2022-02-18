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
import klfr.sof.Interpreter;
import klfr.sof.exceptions.*;

/**
 * Responsible for the native function system. Any SOF extension or native function implementor uses this class to
 * register native functions.
 */
public final class NativeFunctionRegistry {

	private static final Logger log = Logger.getLogger(NativeFunctionRegistry.class.getCanonicalName());

	/**
	 * Functional interface for native functions.
	 */
	@FunctionalInterface
	public static interface NativeNArgFunction {

		/**
		 * Call the native function.
		 * 
		 * @param interpreter The interpreter that is calling this function.
		 * @return The result of the native function.
		 * @throws IncompleteCompilerException If the native function fails.
		 */
		public Stackable call(Interpreter interpreter) throws IncompleteCompilerException;
	}

	private final TreeMap<String, NativeNArgFunction> nativeFunctions = new TreeMap<>();

	/**
	 * Loads all native functions in the specified package.<br/>
	 * <br/>
	 * 
	 * This method will search through all classes in this package (including subpackages) and load native functions from
	 * native function collection classes. These are classes annotated with {@link klfr.sof.lib.NativeFunctionCollection}.
	 * 
	 * @param packageName The package name. Its subpackages are searched as well.
	 * @throws IOException If any exception occurs, also in the class discovery an reflection access process.
	 */
	public final void registerAllFromPackage(String packageName) throws IOException {
		Reflections pakage = new Reflections(packageName);
		var classes = pakage.get(SubTypes.of(TypesAnnotated.with(NativeFunctionCollection.class)).asClass());
		;
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
	 * This method uses reflection to find all methods on the class that can be used as native functions. A method that is
	 * to be registered as a native function must satisfy the following conditions:
	 * </p>
	 * <ul>
	 * <li>It must be public and static. An easy way of preventing a method from being registered is making it private or an
	 * instance method.</li>
	 * <li>Its argument types and its return type must be {@link klfr.sof.lang.Stackable} or a subclass thereof. It must not
	 * have a void return type.</li>
	 * </ul>
	 * Classes which are not accessible to this class due to visibility restrictions will cause reflection errors to be
	 * thrown. Inacessible classes should not be passed to this method.
	 * 
	 * @param clazz The class to search for native functions. All suitable methods of this class are registered, whether the
	 *                 caller intended them to be registered or not.
	 */
	public final void registerNativeFunctions(Class<?> clazz) {
		// FP for da win
		Arrays.stream(clazz.getDeclaredMethods())
				// only public static methods, only methods with only Stackable or Stackable
				// subtype parameters, only methods with Stackable return type (or no return
				// value)
				.filter(m -> Modifier.isStatic(m.getModifiers()) && Modifier.isPublic(m.getModifiers()) && Arrays.stream(m.getParameterTypes()).allMatch(ptype -> Stackable.class.isAssignableFrom(ptype))
						&& Stackable.class.isAssignableFrom(m.getReturnType()) || (m.getReturnType() == void.class))
				// map the methods to one of the mcallN a proxies and flatten streams
				.map(NativeFunctionRegistry::createNativeFunctionWrapper)
				// add the methods to the function registry
				.forEach(ftuple -> nativeFunctions.put(generateDescriptor(ftuple.getRight()), ftuple.getLeft()));
	}

	/**
	 * Return the native function associated with the given standard native function identifier string, which the SOF source
	 * code originally provided.
	 * 
	 * @param fidentifier The standard native function identifier string, as e.g. the nativecall SOF PT expects it.
	 * @return The corresponding native function, or a dummy zero argument function does nothing and returns null if the
	 *         specified native function was not found.
	 */
	public final Optional<NativeNArgFunction> getNativeFunction(String fidentifier) {
		return Optional.ofNullable(nativeFunctions.getOrDefault(fidentifier, null));
	}

	/**
	 * This lambda function is only externalized to make Java recognize the return type properly. -_-
	 * 
	 * Maps a given map entry with a number of arguments and its respective methods to tuples that contain the method call
	 * wrapper to the left and the original method to the right.
	 */
	private static Tuple<NativeNArgFunction, Method> createNativeFunctionWrapper(final Method method) {
		final var argcount = method.getParameterCount();
		return new Tuple<NativeNArgFunction, Method>(interpreter -> {
			final var list = interpreter.getStack().popSafe(argcount);
			try {
				return (Stackable) method.invoke(null, list.toArray());
			} catch (IllegalAccessException | IllegalArgumentException | ExceptionInInitializerError e) {
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
		}, method);
	}

	/**
	 * Generates a standard SOF native function descriptor for the given method, depending on its location in the package
	 * and class hierarchy, its name and its argument types and count.
	 * 
	 * @param function The Method runtime reference which to generate the descriptor for.
	 * @return A string that contains the standard SOF descriptor uniquely referencing the method.
	 */
	public static String generateDescriptor(Method function) {
		final String className = function.getDeclaringClass().getSimpleName(), methodName = function.getName(), packageName = function.getDeclaringClass().getPackageName();
		final var arguments = Arrays.stream(function.getParameterTypes()).map(pt -> pt.getSimpleName()).collect(Collectors.joining(","));
		return new StringBuilder(packageName).append(".").append(className).append("#").append(methodName).append("(").append(arguments).append(")").toString();
	}

}

/*
 * The SOF programming language interpreter. Copyright (C) 2019-2020
 * kleinesfilmröllchen
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 */
