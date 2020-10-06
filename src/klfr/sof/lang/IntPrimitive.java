package klfr.sof.lang;

import klfr.sof.CompilerException;

/**
 * Integer, i.e. whole positive or negative number primitive type.
 * 
 * @author klfr
 */
@StackableName("Integer")
public class IntPrimitive extends Primitive {

   private static final long serialVersionUID = 1L;

   private long value;

   private IntPrimitive(Long v) {
      this.value = v;
   }

   public static IntPrimitive createIntPrimitive(Long value) {
      return new IntPrimitive(value);
   }

   @Override
   public Object v() {
      return value;
   }

   public Long value() {
      return value;
   }

   /**
    * Execute optimized arithmetic add.
    */
   public IntPrimitive add(IntPrimitive other) {
      if (this.value == 0)
         return other;
      if (other.value == 0)
         return this;
      return createIntPrimitive(this.value + other.value);
   }

   /**
    * Helper method to create an integer primitive from a string that is a valid
    * integer literal according to SOF specs.
    * 
    * @param integerString The string that only contains the integer in text
    *                      format, to be converted.
    * @return a new IntPrimitive with the parsed integer value.
    */
   public static IntPrimitive createIntegerFromString(String integerString) throws CompilerException {
      integerString = integerString.strip();
      int radix = 10;
      long sign = 1;
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
         switch (base) {
            case 'b':
               radix = 2;
               break;
            case 'o':
               radix = 8;
               break;
            case 'd':
               radix = 10;
               break;
            case 'h':
            case 'x':
               radix = 16;
               break;
            default:
               throw new CompilerException.Incomplete("syntax", "syntax.integer", integerString);
         }
         integerString = integerString.substring(2);
      }

      String reverseInt = new StringBuilder(integerString).reverse().toString();
      long value = 0;
      for (int place = 0; place < reverseInt.length(); ++place) {
         char magnitude = reverseInt.charAt(place);
         if (!numberChars.containsKey(magnitude) || numberChars.get(magnitude) >= radix) {
            throw new CompilerException.Incomplete("syntax", "syntax.integer.base", magnitude, radix);
         }
         value += numberChars.get(magnitude) * (long) (Math.pow(radix, place));
      }
      return new IntPrimitive(value * sign);
   }

   public IntPrimitive divide(IntPrimitive other) throws ArithmeticException {
      return createIntPrimitive(this.value / other.value);
   }

   public IntPrimitive multiply(IntPrimitive other) {
      return this.value == 1 ? other : (other.value == 1 ? this : createIntPrimitive(this.value * other.value));
   }

   public IntPrimitive subtract(IntPrimitive other) {
      if (other.value == 0)
         return this;
      return createIntPrimitive(this.value - other.value);
   }

   @Override
   public int compareTo(Stackable o) {
      if (o instanceof IntPrimitive) {
         return this.value().compareTo(((IntPrimitive) o).value);
      } else if (o instanceof FloatPrimitive) {
         // invert the comparison result, therefore effectively switching sides
         return -((FloatPrimitive) o).compareTo(this);
      }
      throw new CompilerException.Incomplete("type", "type.compare", this.typename(), o.typename());
   }

   @Override
   public boolean equals(Stackable other) {
      if (other instanceof IntPrimitive) {
         return ((IntPrimitive) other).value == this.value;
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
