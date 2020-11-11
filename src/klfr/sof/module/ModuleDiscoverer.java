package klfr.sof.module;

import java.io.*;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import klfr.sof.CompilerException;
import klfr.sof.ast.Node;
import klfr.sof.*;

/**
 * The system for discovering modules and providing them to the interpreter.
 * Each ModuleDiscoverer has a {@link klfr.sof.module.ModuleRegistry} used to
 * store previously discovered modules. The ModuleDiscoverer uses the
 * {@link klfr.sof.Parser} system for parsing modules.
 */
public class ModuleDiscoverer {
	/** The default directory of the standard library. This value facilitates development. */
	public static final String DEFAULT_STDLIB_DIRECTORY = "./bin/klfr/sof/lib";
	/** Allowed file extensions, tried in the given order. */
	public static final List<String> EXTENSIONS = List.of("sof", "stackof");
	public static final Logger log = Logger.getLogger(ModuleDiscoverer.class.getCanonicalName());

	private final ModuleRegistry registry;
	private final File stdlibBaseDirectory;

	// #region Constructors
	// This would be so much easier with default method arguments...

	/**
	 * Full constructor that uses the given module registry and standard library
	 * directory.
	 * 
	 * @param registry            A module registry that is to be used by this
	 *                            discoverer internally.
	 * @param stdlibBaseDirectory The base directory of the standard library to be
	 *                            used by this discoverer.
	 */
	public ModuleDiscoverer(final ModuleRegistry registry, final File stdlibBaseDirectory) {
		this.registry = registry;
		this.stdlibBaseDirectory = stdlibBaseDirectory;
	}

	/**
	 * Uses the provided module registry and the default (relative) standard library
	 * directory.
	 * 
	 * @param registry A module registry that is to be used by this discoverer
	 *                 internally.
	 */
	public ModuleDiscoverer(final ModuleRegistry registry) {
		this.registry = registry;
		this.stdlibBaseDirectory = new File(DEFAULT_STDLIB_DIRECTORY);
	}

	/**
	 * Ûses the provided standard library directory and an empty module registry.
	 * 
	 * @param stdlibBaseDirectory The base directory of the standard library to be
	 *                            used by this discoverer.
	 */
	public ModuleDiscoverer(final File stdlibBaseDirectory) {
		this.stdlibBaseDirectory = stdlibBaseDirectory;
		this.registry = new ModuleRegistry();
	}

	/**
	 * Uses the default (relative) standard library directory and an empty module
	 * registry.
	 */
	public ModuleDiscoverer() {
		this.registry = new ModuleRegistry();
		this.stdlibBaseDirectory = new File(DEFAULT_STDLIB_DIRECTORY);
	}

	// #endregion Constructors

	/**
	 * Retrieves the specified module for the specified requesting source file
	 * 
	 * @param requestingSourceFile The source file that requested the module
	 *                             specified by the module specifier. This is very
	 *                             important for relative module requests. Of
	 *                             course, this file may be a nonexistent file or a
	 *                             directory, its contents are never read by this
	 *                             method.
	 * @param moduleSpecifier      The string given in SOF source code that
	 *                             specifies the module. Note that this is
	 *                             <strong>not</strong> the same as a module
	 *                             identifier. This module specifier may be relative
	 *                             to the requesting module, which means that
	 *                             depending on the source file which requests the
	 *                             module, different modules may be found by the
	 *                             same specifier string.
	 * @return The AST of the found module, if it exists or was previously
	 *         registered in the registry. If the module was not found, an empty
	 *         Optional is returned.
	 */
	public Optional<SOFFile> getModule(final File requestingSourceFile, final String moduleSpecifier)
			throws CompilerException {
		File fullPath_ = null;
		try {
			// Check whether the module is absolute or relative
			final var isRelative = moduleSpecifier.startsWith(".");
			// the parent directory depends on that
			File parentDirectory = null;
			if (isRelative) {
				// parent file is the containing source directory, which is the level in which the module is contained
				parentDirectory = requestingSourceFile.getParentFile() == null ? new File(".") : requestingSourceFile.getParentFile();
				final var _pd = parentDirectory;
				log.info(() -> String.format("Requesting relative module '%s' from source file '%s' parent dir '%s'", moduleSpecifier,
					requestingSourceFile.getAbsolutePath(), _pd.getAbsolutePath()));
			} else {
				parentDirectory = stdlibBaseDirectory;
				log.info(() -> String.format("Requesting absolute system module '%s'", moduleSpecifier));
			}
			File fullFile = null;
			var filename = moduleSpecifier.replace(".", File.separator);
			// check all file extensions, even the empty one
			for (var extension : EXTENSIONS) {
				var sourceFile = new File(parentDirectory, filename + (!extension.isEmpty() ? ("." + extension) : ""));
				if (sourceFile.exists()) fullFile = sourceFile;
			}
			if (fullFile == null) {
				throw new CompilerException.Incomplete("module", "module", moduleSpecifier);
			}

			var fullPath = fullFile.getCanonicalFile(); /*needed for error handling*/ fullPath_ = fullPath;

			// retrieve or compile module
			if (registry.hasModule(fullPath)) {
				log.fine(() -> String.format("Module %s found in registry.", fullPath));
				// ofNullable just to protect against silent null errors in ModuleRegistry
				return Optional.ofNullable(registry.getModule(fullPath));
			} else {
				// compile the module from source file
				log.fine(() -> String.format("Compiling module %s.", fullPath));
					final var modReader = new FileReader(fullPath, Charset.forName("utf-8"));
					final var modWriter = new StringWriter();
					modReader.transferTo(modWriter);
					modReader.close();
					var modCode = modWriter.toString();

					modCode = Preprocessor.preprocessCode(modCode);
					final var module = Parser.parse(fullPath, modCode);

					registry.storeModule(fullPath, module);
					return Optional.of(module);
			}	
		} catch (IOException e) {
			log.log(Level.SEVERE, String.format("Module not found: %s", fullPath_), e);
			return Optional.empty();
		}
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
