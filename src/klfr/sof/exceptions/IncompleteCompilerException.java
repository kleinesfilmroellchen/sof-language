package klfr.sof.exceptions;

/**
 * A pseudo- compiler exception that does not have information about its
 * occurance in SOF source code, because it was thrown from some subclass
 * without interpreter access.
 * 
 * This class is immutable.
 */
public final class IncompleteCompilerException extends SOFException {
	private static final long serialVersionUID = 1L;
	/**
	 * String that is used as a name for the 'sof.error.type.*' resource messages.
	 * This allows for localized error names.
	 */
	public final String nameKey;
	/**
	 * String that is used as a name for the 'sof.error.message.*' resource
	 * messages. This allows for localized error explanations.
	 */
	public final String explanationKey;

	/**
	 * The formatting arguments used in the error message.
	 */
	public final Object[] formatArguments;

	/**
	 * Constructs a compiler exception that does not have all information for nice
	 * formatting.
	 * 
	 * @param nameKey         Name of the exception, as an accessor into the message
	 *                        resources 'sof.error.type'
	 * @param explanationKey  Why the exception occurred, as an accessor into the
	 *                        message resources 'sof.error.message'
	 * @param formatArguments Arguments for formatting the explanation message with.
	 */
	public IncompleteCompilerException(String nameKey, String explanationKey, Object... formatArguments) {
		super("");
		// null checks on name and explanation resource identifier
		nameKey = nameKey == null ? "generic" : nameKey;
		explanationKey = explanationKey == null ? nameKey : explanationKey;
		this.nameKey = nameKey;
		this.explanationKey = explanationKey;
		this.formatArguments = formatArguments;
	}

	/**
	 * Constructs a compiler exception that does not have all information for nice
	 * formatting.
	 * 
	 * @param nameKey         Name and explanation of the exception, as an accessor
	 *                        into the message resources 'sof.error.type'
	 * @param formatArguments Arguments for formatting the explanation string.
	 */
	public IncompleteCompilerException(String nameKey, Object... formatArguments) {
		this(nameKey, null, formatArguments);
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
