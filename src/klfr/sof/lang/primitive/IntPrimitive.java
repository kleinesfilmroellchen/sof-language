package klfr.sof.lang.primitive;

import klfr.sof.exceptions.*;
import klfr.sof.lang.*;

/**
 * Integer, i.e. whole positive or negative number primitive type.
 * 
 * @author klfr
 */
@StackableName("Integer")
public final class IntPrimitive extends Primitive {

   private static final long serialVersionUID = 1L;

   /** The long that is represented by this primitive. */
   private long              value;

   private IntPrimitive(Long v) {
      this.value = v;
   }

   /**
    * Create a new integer primitive.
    * 
    * @param value The integer to create the primitive from.
    * @return A new integer primitive with the given value.
    */
   public static IntPrimitive createIntPrimitive(Long value) {
      return new IntPrimitive(value);
   }

   @Override
   public Object v() {
      return value;
   }

   /**
    * Returns the value represented by this primitive.
    * 
    * @return the value represented by this primitive.
    */
   public final Long value() {
      return value;
   }

   /**
    * Helper method to create an integer primitive from a string that is a valid integer literal according to SOF specs.
    * 
    * @param integerString The string that only contains the integer in text format, to be converted.
    * @return a new IntPrimitive with the parsed integer value.
    * @throws IncompleteCompilerException If the string does not represent a valid integer.
    */
   public static IntPrimitive createIntegerFromString(String integerString) throws IncompleteCompilerException {
      integerString = integerString.strip();
      long sign = 1;
      int radix = 10;
      // check zero
      if (integerString.matches("[\\+\\-]?0+")) {
         return new IntPrimitive(0l);
      }
      // check sign
      if (integerString.charAt(0) == '+') {
         integerString = integerString.substring(1);
      } else if (integerString.charAt(0) == '-') {
         integerString = integerString.substring(1);
         sign = -1;
      }
      // check radix
      if (integerString.charAt(0) == '0') {
         char base = integerString.charAt(1);
         radix = switch (base) {
         case 'b' -> 2;
         case 'o' -> 8;
         case 'd' -> 10;
         case 'h', 'x' -> 16;
         default -> throw new IncompleteCompilerException("syntax", "syntax.integer", integerString);
         };
         integerString = integerString.substring(2);
      }

      String reverseInt = new StringBuilder(integerString).reverse().toString();
      long value = 0;
      for (int place = 0; place < reverseInt.length(); ++place) {
         char magnitude = reverseInt.charAt(place);
         if (!numberChars.containsKey(magnitude) || numberChars.get(magnitude) >= radix) {
            throw new IncompleteCompilerException("syntax", "syntax.integer.base", magnitude, radix);
         }
         value += numberChars.get(magnitude) * (long) (Math.pow(radix, place));
      }
      return new IntPrimitive(value * sign);
   }

   /**
    * Execute optimized arithmetic add.
    * 
    * @param other The int to add.
    * @return The sum of this int and the other.
    * @see BuiltinOperations#add(Stackable, Stackable)
    */
   public final IntPrimitive add(IntPrimitive other) {
      if (this.value == 0)
         return other;
      if (other.value == 0)
         return this;
      return createIntPrimitive(this.value + other.value);
   }

   /**
    * Execute optimized arithmetic divide.
    * 
    * @param other The int to divide.
    * @return {@code this / other}
    * @see BuiltinOperations#divide(Stackable, Stackable)
    */
   public final IntPrimitive divide(IntPrimitive other) throws ArithmeticException {
      return createIntPrimitive(this.value / other.value);
   }

   /**
    * Execute optimized arithmetic modulus.
    * 
    * @param other The int to modulus with.
    * @return {@code this % other}
    * @see BuiltinOperations#modulus(Stackable, Stackable)
    */
   public final IntPrimitive modulus(IntPrimitive other) throws ArithmeticException {
      if (other.value == 0) {
         throw new ArithmeticException(String.format("Modulus by zero: %d mod %d", this.value, other.value));
      }
      return this.value == 0 ? this : createIntPrimitive(this.value % other.value);
   }

   /**
    * Execute optimized arithmetic multiply.
    * 
    * @param other The int to multiply.
    * @return {@code this * other}
    * @see BuiltinOperations#multiply(Stackable, Stackable)
    */
   public final IntPrimitive multiply(IntPrimitive other) {
      return this.value == 1 ? other : (other.value == 1 ? this : createIntPrimitive(this.value * other.value));
   }

   /**
    * Execute optimized arithmetic subtract.
    * 
    * @param other The int to subtract.
    * @return {@code this - other}
    * @see BuiltinOperations#subtract(Stackable, Stackable)
    */
   public final IntPrimitive subtract(IntPrimitive other) {
      if (other.value == 0)
         return this;
      return createIntPrimitive(this.value - other.value);
   }

   @Override
   public int compareTo(Stackable o) {
      if (o instanceof IntPrimitive otherInt) {
         return this.value().compareTo(otherInt.value);
      } else if (o instanceof FloatPrimitive otherFloat) {
         // invert the comparison result, therefore effectively switching sides
         return -otherFloat.compareTo(this);
      }
      throw new RuntimeException(new IncompleteCompilerException("type", "type.compare", this.typename(), o.typename()));
   }

   @Override
   public boolean equals(Stackable other) {
      if (other instanceof IntPrimitive otherInt) {
         return otherInt.value == this.value;
      }
      return false;
   }

}

/*  
The SOF programming language interpreter.
Copyright (C) 2019-2020  kleinesfilmröllchen

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
