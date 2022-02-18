package klfr;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Objects;

/**
 * Simple tuple type, nothing fancy.
 */
public final class Tuple<A, B> implements Serializable, Iterable<Object>, Cloneable {

   private static final long serialVersionUID = 1L;

   /** The left element of the Tuple. */
   private final A           left;
   /** The right element of the Tuple. */
   private final B           right;

   /**
    * Create a new tuple.
    * 
    * @param l The left part of the tuple.
    * @param r The right part of the tuple.
    */
   public Tuple(A l, B r) {
      left = l;
      right = r;
   }

   /**
    * Create a new tuple.
    * 
    * @param l   The left part of the tuple.
    * @param r   The right part of the tuple.
    * @param <D> The right type of the tuple.
    * @param <C> The left type of the tuple.
    * @return A new tuple.
    */
   public static <D extends Comparable<D>, C extends Comparable<C>> Tuple<C, D> t(C l, D r) {
      return new Tuple<C, D>(l, r);
   }

   // /**
   //  * The Tuple comparison first compares the left two elements and only if they
   //  * are equal, compares the right two elements.
   //  */
   // @Override
   // public int compareTo(Tuple<A, B> other) throws ClassCastException {
   //    var leftcomp = left.compareTo(other.left);
   //    return leftcomp == 0 ? right.compareTo(other.right) : leftcomp;
   // }

   /**
    * The Tuple iterator returns the left and the right value, in that order.
    */
   @Override
   public Iterator<Object> iterator() {
      return new Iterator<Object>() {

         final Object[] vals = new Object[] { left, right };
         byte           idx  = 0;

         @Override
         public boolean hasNext() {
            return idx < 2;
         }

         @Override
         public Object next() {
            return vals[idx++];
         }
      };
   }

   @SuppressWarnings("unchecked")
   @Override
   public boolean equals(Object other) {
      try {
         return this.equals((Tuple<A, B>) other);
      } catch (ClassCastException e) {
         return false;
      }
   }

   /**
    * Compare this tuple to another tuple for equality. Two tuples are considered equal if the left elements are equal and
    * the right elements are equal.
    * 
    * @param other The other tuple to compare to.
    * @return Whether the two tuples are equal.
    */
   public final boolean equals(Tuple<A, B> other) {
      return other.left.equals(this.left) && other.right.equals(this.right);
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.left.hashCode(), this.right.hashCode());
   }

   @Override
   public Tuple<A, B> clone() throws CloneNotSupportedException {
      try {
         return new Tuple<A, B>((A) (left), (B) (right));
      } catch (ClassCastException e) {
         throw new CloneNotSupportedException(String.format("Tuple parts %s <%s> and %s <%s> do not clone into their own type", left, right, left.getClass(), right.getClass()));
      }
   }

   @Override
   public String toString() {
      return String.format("(%s, %s)", left, right);
   }

   /**
    * Returns the left, or first element of the tuple.
    * 
    * @return the left, or first element of the tuple.
    */
   public final A getLeft() {
      return left;
   }

   /**
    * Returns the right, or second element of the tuple.
    * 
    * @return the right, or second element of the tuple.
    */
   public final B getRight() {
      return right;
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
