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