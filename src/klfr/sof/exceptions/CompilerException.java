package klfr.sof.exceptions;

import java.io.File;
import java.util.Locale;

import klfr.sof.*;

/**
 * Compiler/interpreter exception class with fancy formatting.
 * 
 * @author klfr
 */
public class CompilerException extends SOFException {
	private static final long serialVersionUID = 1L;

	/**
	 * Primitive constructor that sets the final fields. This constructor is
	 * interfaced with via constructing static methods, so it is private.
	 */
	private CompilerException(SOFFile location, int index, String nameKey, String reasonKey, Object[] formatArguments) {
		// null checks on name and reason resource identifier
		super("");

		nameKey = nameKey == null ? "generic" : nameKey;
		reasonKey = reasonKey == null ? nameKey : reasonKey;

		this.location = location;
		this.index = index;
		this.nameKey = nameKey;
		this.reasonKey = reasonKey;
		this.formatArguments = formatArguments;
	}
	

	/**
	 * The code object where the exception occurred.
	 */
	private final SOFFile location;
	/**
	 * The index inside the code object's string code source where the exception
	 * occurred.
	 */
	private final int index;

	/**
	 * String that is used as a name for the 'sof.error.type.*' resource messages.
	 * This allows for localized error names.
	 */
	private final String nameKey;
	/**
	 * String that is used as a name for the 'sof.error.message.*' resource
	 * messages. This allows for localized error explanations.
	 */
	private final String reasonKey;

	/**
	 * The formatting arguments used in the error message.
	 */
	private final Object[] formatArguments;

	/**
	 * Makes a compiler exception that takes its positional information from a
	 * tokenizer.
	 * 
	 * @param expressionInfo A tokenizer pointing to the position where the
	 *                       exception occurred
	 * @param name           Name of the exception, as an accessor into the message
	 *                       resources 'sof.error.type'
	 * @param reason         Why the exception occurred, as an accessor into the
	 *                       message resources 'sof.error.message'
	 * @return nicely formatted multi-line string
	 */
	public static CompilerException from(String filename, Tokenizer expressionInfo, String name,
			String reason, final Object... formatArguments) {
		// setting the AST of this to null is fine because it is never read
		SOFFile file = new SOFFile(new File(filename), expressionInfo.getCode(), null);
		return new CompilerException(file, expressionInfo.start(), name, reason, formatArguments);
	}

	/**
	 * Makes a compiler exception that takes its positional information from
	 * tokenizer-like data (code, index inside code).
	 * 
	 * @param source The source file where the exception occurred.
	 * @param index  Index inside fullExpression where the exception occurred.
	 * @param name   Name of the exception, as an accessor into the message
	 *               resources 'sof.error.type'.
	 * @param reason Why the exception occurred, as an accessor into the message
	 *               resources 'sof.error.message'.
	 * @return nicely formatted multi-line string.
	 */
	public static CompilerException from(SOFFile source, int index, String name, String reason,
			final Object... formatArguments) {
		return new CompilerException(source, index, name, reason, formatArguments);
	}

	/**
	 * Constructs a compiler exception with the given base exception that points to
	 * the current place in code the tokenizer is looking at. This method is used
	 * with Incomplete compiler exceptions which were thrown by parts of the SOF
	 * system unaware of code positions.
	 * 
	 * @param expressionInfo The tokenizer that points to the location where the
	 *                       exception occured
	 * @param cause          The cause of this exception, an instance of the stub
	 *                       class {@link IncompleteCompilerException}
	 * @return The newly constructed compiler exception.
	 */
	public static CompilerException fromIncomplete(Tokenizer expressionInfo, IncompleteCompilerException cause) {
		final var exc = from("<unknown>", expressionInfo, cause.nameKey, cause.explanationKey,
				cause.formatArguments);
		exc.initCause(cause);
		return exc;
	}

	/**
	 * Constructs a compiler exception with the given base exception that points to
	 * the current place in code specified with index. This method is used with
	 * Incomplete compiler exceptions which were thrown by parts of the SOF system
	 * unaware of code positions.
	 * 
	 * @param cause The cause of this exception, an instance of the stub class
	 *              {@link IncompleteCompilerException}
	 * @return The newly constructed compiler exception.
	 */
	public static CompilerException fromIncomplete(SOFFile sofFile, int index, IncompleteCompilerException cause) {
		final var exc = from(sofFile, index, cause.nameKey, cause.explanationKey, cause.formatArguments);
		exc.initCause(cause);
		return exc;
	}

	@Override
	public String getLocalizedMessage() {
		final var formatter = new CompilerExceptionFormatter(this);
		return formatter.formatCLI(Locale.getDefault());
	}

	@Override
	public String getMessage() {
		final var formatter = new CompilerExceptionFormatter(this);
		return formatter.formatCLI(Locale.ENGLISH);
	}

	public SOFFile getLocation() {
		return location;
	}

	public int getIndex() {
		return index;
	}

	public String getNameKey() {
		return nameKey;
	}

	public String getReasonKey() {
		return reasonKey;
	}

	public Object[] getFormatArguments() {
		return formatArguments;
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
