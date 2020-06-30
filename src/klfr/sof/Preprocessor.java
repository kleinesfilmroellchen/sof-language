package klfr.sof;

import java.util.Scanner;
import java.util.logging.Level;

/**
 * The SOF preprocessor is a collection of static methods that normalize and
 * clean code before it can be executed. Unlike normal preprocessors, this
 * preprocessor does little to no logical work on the source code it is given
 * and only detects the most basic of syntax errors.
 */
public class Preprocessor {

   /**
    * Preprocesses an SOF literal string token that has already been processed by
    * {@link Preprocessor#preprocessCode(String)}. The given token must still
    * contain the leading and trailing double quote, and the
    * {@link klfr.sof.Interpreter#stringPattern} is used to match the string
    * contents and replace the escape sequences.
    * 
    * @param sofString The SOF literal string token to be processed.
    * @return A string that can be used directly to construct a
    *         {@link klfr.sof.lang.StringPrimitive}.
    */
   public static String preprocessSofString(final String sofString) {
      final var strm = Interpreter.stringPattern.matcher(sofString);
      if (!strm.matches()) {
         throw CompilerException.makeIncomplete("Syntax",
               String.format("`%s´ is not a valid string literal.", sofString));
      }
      final var str =  strm.group(1);
      return str.replace("\\\"", "\"").replace("\\n", System.lineSeparator());
   }

   /**
    * Runs the preprocessing actions on the code. These are:
    * <ul>
    * <li>Removing any comments in the code while keeping all line numbers
    * correct.</li>
    * <li>Replacing the escape sequences in strings with their actual
    * characters.</li>
    * </ul>
    * The preprocessor, therefore, guarantees that
    * <ul>
    * <li>No comments of any kind exist in the returned code; all non-whitespace
    * characters are important;</li>
    * <li>No unclosed strings exist;</li>
    * <li>The only escape sequences remaining in strings are the quote escape
    * <code>\"</code> and the newline escape <code>\n</code>;</li>
    * <li>All line numbers match up with the line numbers in the original code;
    * however, the indices inside lines might <b>not</b> match up if comments were
    * removed on that particular line.</li>
    * <li>All code blocks match up, i.e. there are exactly as many <code>{</code>
    * as <code>}</code> tokens.
    * </ul>
    * 
    * @throws CompilerException if block comments, code blocks or string literals
    *                           are not closed properly.
    */
   public static String preprocessCode(final String code) throws CompilerException {
      final StringBuilder newCode = new StringBuilder();
      @SuppressWarnings("resource")
      final Scanner scanner = new Scanner(code);
      String line = "";
      boolean insideBlockComment = false;
      int codeBlockDepth = 0;
      int lineIdx = 0;
      while (scanner.hasNextLine()) {
         line = scanner.nextLine();
         ++lineIdx;
         if (line.length() == 0)
            continue;
         char c;
         for (int i = 0; i < line.length(); ++i) {
            c = line.charAt(i);

            if (insideBlockComment) {
               // if we have the ending character
               if (c == '*' && i < line.length() - 1)
                  if (line.charAt(i + 1) == '#') {
                     ++i;
                     insideBlockComment = false;
                  }
            } // end of block comment check
            else {
               if (c == '{')
                  ++codeBlockDepth;
               else if (c == '}')
                  --codeBlockDepth;
               if (c == '"') {
                  // use string pattern to ensure valid string literal
                  final var toSearch = line.substring(i);
                  final var m = Interpreter.stringPattern.matcher(toSearch);
                  if (!m.find()) {
                     Interpreter.log.log(Level.INFO,
                           String.format("Invalid string literal in '%s' at %d line %d%n", line, i, lineIdx));
                     throw CompilerException.fromCurrentPosition(line, i, "Syntax",
                           "Invalid string literal, maybe a missing \" or wrong escapes?");
                  }
                  final var escapedString = Interpreter.escapeSequencePattern.matcher(m.group()).replaceAll(match -> {
                     if (match.group(2) != null) {
                        // unicode escape sequence
                        final int codepoint = Integer.parseInt(match.group(2), 16);
                        return String.valueOf(Character.toChars(codepoint));
                     }
                     switch (match.group(1)) {
                        case "t":
                           return "\t";
                        case "f":
                           return "\f";
                        default:
                           return "";
                     }
                  });
                  newCode.append(escapedString);
                  i = m.end() + i - 1;
               } // end of string match
               else if (c == '#') {
                  if (i < line.length() - 1)
                     if (line.charAt(i + 1) == '*') {
                        ++i;
                        // we found a block comment
                        insideBlockComment = true;
                     } else {
                        // skip the single-line comment
                        i = line.length();
                     }
               } else {
                  newCode.append(c);
               }
            } // end of non-block comment
         } // end of single line
         newCode.append(System.lineSeparator());
      } // end of scan

      scanner.close();
      if (codeBlockDepth > 0) {
         throw CompilerException.fromCurrentPosition(newCode.toString(), newCode.lastIndexOf("{"), "Syntax",
               "Unclosed code block");
      } else if (codeBlockDepth < 0) {
         throw CompilerException.fromCurrentPosition(newCode.toString(), newCode.lastIndexOf("}"), "Syntax",
               "Too many closing '}'");
      }
      return newCode.toString();
   }

}