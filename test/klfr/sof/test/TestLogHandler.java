package klfr.sof.test;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Arrays;
import java.util.MissingResourceException;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

/**
 * Simple logging handler similar to ConsoleHandler that logs test results
 */
public class TestLogHandler extends StreamHandler {

   public TestLogHandler() {
      super(System.out,
            // formatter based on SOF's REPL debug mode logger formatter
            new Formatter() {
               @Override
               public String format(LogRecord record) {
                  final var msg = record.getMessage();
                  try {
                     record.getResourceBundle().getString(record.getMessage());
                  } catch (MissingResourceException | NullPointerException e) {
                     // do nothing
                  }
                  final var time = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM)
                        .format(record.getInstant().atZone(ZoneId.systemDefault()));
                  final var level = record.getLevel().getLocalizedName().substring(0,
                        Math.min(record.getLevel().getLocalizedName().length(), 6));
                  final var logName = record.getLoggerName().replace("klfr.sof", "~");

                  return String.format("[%s %-20s |%6s] %s%n", time, logName, level, msg) + (record.getThrown() == null
                        ? ""
                        : String.format("EXCEPTION: %s | Stack trace:%n%s", record.getThrown().toString(),
                              Arrays.asList(record.getThrown().getStackTrace()).stream().map(x -> x.toString()).collect(
                                    () -> new StringBuilder(),
                                    (builder, str) -> builder.append("in ").append(str).append(System.lineSeparator()),
                                    (b1, b2) -> b1.append(b2))));
               }
            });
      this.setLevel(Level.FINER);
   }

   /// -----------------------------------------------------------------------------------------------------
   /// Following code adapted from ConsoleHandler.java (Standard library jdk-12.0.2, licensed under GPL 2.0)
   /// Documentation was slightly modified to fit this class's changed behavior.

   /**
    * Publish a {@code LogRecord}.
    * <p>
    * The logging request was made initially to a {@code Logger} object, which
    * initialized the {@code LogRecord} and forwarded it here.
    *
    * @param record description of the log event. A null record is silently ignored
    *               and is not published
    */
   @Override
   public void publish(LogRecord record) {
      super.publish(record);
      flush();
   }

   /**
    * Override {@code StreamHandler.close} to do a flush but not to close the
    * output stream. That is, we do <b>not</b> close {@code System.out}.
    */
   @Override
   public void close() {
      flush();
   }
}