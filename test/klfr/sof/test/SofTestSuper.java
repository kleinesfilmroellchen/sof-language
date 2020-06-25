package klfr.sof.test;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.junit.BeforeClass;

public class SofTestSuper {

   @BeforeClass
   public static void hyperSetup() throws Exception {
      
      LogManager.getLogManager().reset();
      var rootLog = Logger.getLogger("");
      rootLog.setLevel(Level.ALL);
      final var log = Logger.getLogger("");
      log.setLevel(Level.ALL);
      for (final var h : log.getHandlers()) {
         log.removeHandler(h);
      }
      final var ch = new ConsoleHandler();
      ch.setLevel(Level.FINER);
      log.addHandler(ch);
   }
}