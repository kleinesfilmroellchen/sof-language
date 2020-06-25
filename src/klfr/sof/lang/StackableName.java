
package klfr.sof.lang;

import java.lang.annotation.*;

/**
 * This annotation gives each Stackable its user-visible name and is used in
 * error messages and other information.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Target(ElementType.TYPE)
public @interface StackableName {
   public String value();

}