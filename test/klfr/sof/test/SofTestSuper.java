package klfr.sof.test;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.junit.jupiter.api.*;

/**
 * Basic superclass of all tests. Sets a logger up that logs messages from this
 * module to the console.
 */
public class SofTestSuper {

   /**
    * Handler that logs messages from the packages klfr.sof and below into the
    * console. By reusing the exact same handler each time, it cannot be added
    * twice to the handlers of the klfr.sof logger hierarchy.
    */
   private static final Handler ch = new TestLogHandler();

   private static boolean hasRunInit = false;

   @BeforeAll
   public static void hyperSetup() throws Exception {
      if (hasRunInit)
         return;

      // reset and unlock the root logger
      LogManager.getLogManager().reset();
      final var rootLog = Logger.getLogger("");
      rootLog.setLevel(Level.ALL);

      // log the module-internal stuff to the console
      Logger.getLogger("klfr.sof").addHandler(ch);

      // prevent initializer from running a second time
      hasRunInit = true;
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
