package klfr.sof.lib;

import java.lang.annotation.*;

/**
 * A simple annotation that specifies that this class is a collection of native functions. This annotation is searched
 * for by {@link NativeFunctionRegistry#registerAllFromPackage(String)} to determine classes that are native function
 * collections.
 */
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Target(ElementType.TYPE)
public @interface NativeFunctionCollection {
}
