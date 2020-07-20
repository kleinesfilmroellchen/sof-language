package klfr.sof.lang;

import klfr.sof.CompilerException;

@StackableName("Boolean")
public class BoolPrimitive extends Primitive {
   private static final long serialVersionUID = 1L;
   private final Boolean value;

   private BoolPrimitive(Boolean value) {
      this.value = value;
   }

   @Override
   public Object v() {
      return value;
   }

   public Boolean value() {
      return value;
   }

   public static BoolPrimitive createBoolPrimitive(Boolean value) {
      return new BoolPrimitive(value);
   }

   public static BoolPrimitive createBoolFromString(String booleanString) throws CompilerException {
      if (booleanString.toLowerCase().equals("true"))
         return new BoolPrimitive(true);
      if (booleanString.toLowerCase().equals("false"))
         return new BoolPrimitive(false);
      throw new CompilerException.Incomplete("syntax", "boolean.syntax", booleanString);
   }

   public boolean equals(Stackable other) {
      if (other instanceof BoolPrimitive) {
         return ((BoolPrimitive) other).value == this.value;
      }
      return super.equals(other);
   }

   /**
    * Returns whether the value represented by this boolean primitive is true.
    * 
    * @return whether the value represented by this boolean primitive is true.
    */
   @Override
   public boolean isTrue() {
      return this.value;
   }

   /**
    * Returns whether the value represented by this boolean primitive is false.
    * 
    * @return whether the value represented by this boolean primitive is false.
    */
   @Override
   public boolean isFalse() {
      return !this.value;
   }

}
