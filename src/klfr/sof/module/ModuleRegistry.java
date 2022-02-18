package klfr.sof.module;

import java.io.File;
import java.io.Serializable;
import java.util.*;

import klfr.sof.SOFFile;

/**
 * The module registry stores all discovered modules and their AST. Any module is identified by its file path, which
 * tells the module discovery system where to search for the module.
 */
public final class ModuleRegistry implements Serializable {

	private static final long			serialVersionUID	= 1L;

	/** The SOF modules cached by this registry. */
	private final Map<File, SOFFile>	modules				= new TreeMap<>();

	/**
	 * Create a new module registry.
	 */
	public ModuleRegistry() {
	}

	/**
	 * Adds a new Module to this module registry.
	 * 
	 * @param moduleName The identifier of the module.
	 * @param module     The module to be stored.
	 * @return Whether the module did already exist. If this method returns true, another module with the same identifier
	 *         was already registered and replaced by this module.
	 */
	public final boolean storeModule(File moduleName, SOFFile module) {
		final var prev = this.modules.put(moduleName, module);
		return prev != null;
	}

	/**
	 * Returns whether this module registry has the specified module identifier registered.
	 * 
	 * @param moduleName The module identifier to be checked.
	 * @return whether this module registry has the specified module identifier registered.
	 */
	public final boolean hasModule(File moduleName) {
		return this.modules.containsKey(moduleName);
	}

	/**
	 * Returns the module associated with the specified module identifier.
	 * 
	 * @param moduleName The identifier of the module to be retrieved.
	 * @return the module associated with the specified module identifier.
	 */
	public final SOFFile getModule(File moduleName) {
		return this.modules.get(moduleName);
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
