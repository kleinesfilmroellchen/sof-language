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

}
