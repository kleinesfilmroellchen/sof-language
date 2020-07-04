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
      throw CompilerException.makeIncomplete("Syntax",
            String.format("No boolean literal found in \"%s\"", booleanString));
   }

   public boolean equals(Stackable other) {
      if (other instanceof BoolPrimitive) {
         return ((BoolPrimitive) other).value == this.value;
      } else if (other instanceof IntPrimitive) {
         return this.value ? (((IntPrimitive) other).value() != 0) : (((IntPrimitive) other).value() == 0);
      } else if (other instanceof FloatPrimitive) {
         return this.value ? (((FloatPrimitive) other).value() != 0) : (((FloatPrimitive) other).value() == 0);
      }
      return false;
   }

   /**
    * Returns whether the value represented by this boolean primitive is true.
    * 
    * @return whether the value represented by this boolean primitive is true.
    */
   public boolean isTrue() {
      return this.value;
   }

   /**
    * Returns whether the value represented by this boolean primitive is false.
    * 
    * @return whether the value represented by this boolean primitive is false.
    */
   public boolean isFalse() {
      return !this.value;
   }

}
