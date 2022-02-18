package klfr.sof.cli;

import java.time.ZoneId;
import java.time.format.*;
import java.util.Arrays;
import java.util.MissingResourceException;
import java.util.logging.*;

/**
 * The formatter that SOF uses for outputting logging information to the user and log files.
 * 
 * @author klfr
 */
public class DebugFormatter extends Formatter {

	@Override
	public String format(LogRecord record) {
		final var msg = record.getMessage();
		try {
			record.getResourceBundle().getString(record.getMessage());
		} catch (MissingResourceException | NullPointerException e) {
			// do nothing
		}
		final var time = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM).format(record.getInstant().atZone(ZoneId.systemDefault()));
		final var level = record.getLevel().getLocalizedName().substring(0, Math.min(record.getLevel().getLocalizedName().length(), 6));
		final var logName = record.getLoggerName().replace("klfr.sof", "~");

		return String.format("[%s %-20s |%6s] %s%n", time, logName, level, msg) + (record.getThrown() == null ? "" : formatException(record.getThrown()));
	}

	/** Helper to format an exception and its causality chain. */
	private String formatException(final Throwable exc) {
		final var sb = new StringBuilder(512);
		sb.append(String.format("EXCEPTION: %s | Stack trace:%n", exc.toString()));

		var currentExc = exc;
		int level = 0;
		while (currentExc != null) {
			sb.append(Arrays.asList(currentExc.getStackTrace()).stream().map(x -> x.toString()).collect(() -> new StringBuilder(), (builder, str) -> builder.append(" in ").append(str).append(System.lineSeparator()), (b1, b2) -> b1.append(b2))
					.toString().indent(level * 2));

			currentExc = currentExc.getCause();
			++level;
			if (currentExc != null)
				sb.append("Caused by: ").append(currentExc.toString()).append("\n");
		}
		return sb.toString();
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
