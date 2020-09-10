package klfr.sof.lib;

import java.util.Random;

import klfr.sof.lang.*;

/**
 * Container for most of the builtin functions, i.e. functions that are
 * available in the global namespace by default. These native operations are
 * bound to normal SOF functions in the preamble.sof file, the only SOF source
 * code file that is included in every execution of every program without
 * external control.
 */
public final class Builtins {

   /**
    * A small java.util.Random wrapper which makes the next(int) method visible.
    */
   private static class Random extends java.util.Random {
      private static final long serialVersionUID = 1L;

      public int next(int bits) {
         return super.next(bits);
      }

      public long nextL(int bits) {
         return bits <= 32 ? super.next(bits) : (((long) (super.next(bits - 32)) << 32) + super.next(32));
      }
   }

   /** The RNG used by SOF's builtin random functions. */
   private static final Random sofRandom = new Random();

   /** Find the most significant bit number of the number that is set. */
   public static int mostSignificantSetBit(long n) {
      if (n == 0)
         return 0;
      int msb = 0;
      while ((n & (0xffffffffffffffffl << ++msb)) != 0 && msb < 64)
         ;
      return msb;
   }

   /**
    * Implements SOF's random:int builtin function.
    * 
    * @param from
    * @param to
    * @return
    */
   public static IntPrimitive random(IntPrimitive from, IntPrimitive to) {
      final long start = from.value(), end = to.value(), range = end - start + 1;
      // to arrive at a suitable number with less RNG calls, determine the msb of the
      // range. This way, only rng values with these bits may be computed.
      int rangeMsb = mostSignificantSetBit(range) + 1;
      long rnumber = 0;
      do {
         rnumber = sofRandom.nextL(rangeMsb);
      } while (rnumber < 0 || rnumber >= range);
      return IntPrimitive.createIntPrimitive(rnumber + start);
   }
}

/*
The SOF programming language interpreter. Copyright (C) 2019-2020
kleinesfilmröllchen

This program is free software: you can redistribute it and/or modify it under
the terms of the GNU General Public License as published by the Free Software
Foundation, either version 3 of the License, or (at your option) any later
version.

This program is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
details.

You should have received a copy of the GNU General Public License along with
this program. If not, see <https://www.gnu.org/licenses/>.
*/
