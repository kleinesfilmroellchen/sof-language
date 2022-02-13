package klfr.sof.lang.primitive;

import klfr.sof.exceptions.IncompleteCompilerException;
import klfr.sof.lang.*;

/**
 * A string primitive of SOF.
 * 
 * @author klfr
 */
@StackableName("String")
public final class StringPrimitive extends Primitive {
   private static final long serialVersionUID = 1L;

   /** The string that is represented by this primitive. */
   private final String s;
   /**
    * The length of the string primitive.
    */
   public final long length;

   private StringPrimitive(String s) {
      this.s = s;
      length = s.length();
   }

   @Override
   public Object v() {
      return s;
   }

   /**
    * Returns the string that this primitive represents.
    * @return The string that this primitive represents.
    */
   public final String value() {
      return s;
   }

   /**
    * Create a new string primitive from the given string.
    * @param s The string that the primitive represents.
    * @return A new string primitive representing the given string.
    */
   public static StringPrimitive createStringPrimitive(String s) {
      return new StringPrimitive(s);
   }

   @Override
   public String toDebugString(DebugStringExtensiveness e) {
      return switch (e) {
         case Full -> String.format("s\"%s\"(%2d)", this.s.replace("\n", "\\n").replace("\t", "\\t").replace("\f", "\\f").replace("\r", "\\r"), this.length);
         case Compact -> '"' + s + '"';
         default -> super.toDebugString(e);
      };
   }

   @Override
   public String print() {
      return s;
   }

   @Override
   public int compareTo(Stackable o) {
      if (o instanceof StringPrimitive otherString) {
         return this.s.compareTo(otherString.s);
      }
      throw new RuntimeException(new IncompleteCompilerException("type", "type.compare", this.typename(), o.typename()));
   }

   @Override
   public boolean equals(Stackable other) {
      if (other instanceof StringPrimitive otherString)
         return this.s.equals(otherString.s);
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
