package klfr.sof.lang;

import java.util.*;
import java.util.stream.Stream;

import klfr.sof.*;

/**
 * A nametable is the second basic structure in SOF and contains values
 * identified by identifiers (duh)
 * 
 * @author klfr
 */
public class Nametable implements Stackable {
	private static final long serialVersionUID = 1L;

	/**
	 * Returns a stream over all the identifier-value mappings that this nametable
	 * contains. Useful for operating on and/or traversing the entire nametable.
	 */
	public Stream<Map.Entry<Identifier, Stackable>> mappingStream() {
		return entries.entrySet().parallelStream();
	}

	public int size() {
		return entries.size();
	}

	/**
	 * Checks whether the identifier is already defined.
	 */
	public boolean hasMapping(Identifier key) {
		return entries.containsKey(key);
	}

	/**
	 * Returns the value associated with the identifier, or null if there is none.
	 */
	public Stackable get(Identifier key) {
		return entries.get(key);
	}

	/**
	 * Binds the given value to the identifier.
	 */
	public Stackable put(Identifier key, Stackable value) {
		return entries.put(key, value);
	}

	/**
	 * Sets the return value for the nametable. The default implementation does
	 * nothing and it is to be overridden by subclasses who specify the behavior of
	 * the {@code return} PT.
	 * 
	 * @param returnValue The value to return in SOF.
	 * @return This nametable.
	 */
	public Nametable setReturn(Stackable returnValue) {
		return this;
	}

	/**
	 * Returns all identifiers present in the nametable.
	 */
	public Set<Identifier> identifiers() {
		return entries.keySet();
	}

	private final Map<Identifier, Stackable> entries = new TreeMap<>();

	@Override
	public String toDebugString(DebugStringExtensiveness e) {
		switch (e) {
			case Compact:
				return "NT[" + entries.size() + "]";
			case Full:
				return "┌" + Interpreter.line66.substring(2) + "┐" + System.lineSeparator() + // top of the table
						mappingStream().collect( // the stream is parallel b/c order does not exist
								() -> new StringBuilder(66), // create a new string builder as the starting point
								(strb, entry) -> // for each new entry, append its formatted string to the string builder
								strb.append(String.format("╞%20s ->%40s ╡%n", entry.getKey().getValue(),
										DebugStringExtensiveness.Compact
												.ensureLength(entry.getValue().toDebugString(DebugStringExtensiveness.Compact)))),
								(strb1, strb2) -> strb1.append(strb2)) // combine stringbuilders with a newline
						+ "└" + Interpreter.line66.substring(2) + "┘"; // bottom of the table
			default:
				return Stackable.toDebugString(this, e);
		}
	}

	public Stackable copy() {
		// hell fucking yes 'functional' programming
		return mappingStream().map(x -> Map.entry(x.getKey().copy(), x.getValue().copy())).collect(() -> new Nametable(),
				(nt, entry) -> nt.put((Identifier) entry.getKey(), entry.getValue()),
				(nt1, nt2) -> nt1.mappingStream().forEach(x -> nt2.put(x.getKey(), x.getValue())));
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
