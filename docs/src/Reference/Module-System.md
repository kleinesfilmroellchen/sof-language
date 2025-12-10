r[modules]
# The SOF Module System

r[modules.files]
Each SOF source code file ending in `.sof` is a separate module.

r[modules.nesting]
Sub-folders can be used to define sub-modules, where all the modules in the folder are considered children of the module with the same name as the folder.

r[modules.errors]
## Errors

r[modules.errors.circular-import]
It is an error to import a module that is already currently executing. In other words, the module import graph must be acyclic.

r[modules.naming]
## Module names

The name of a module can consist of any Unicode code points from the following set:
- ASCII Alphabetic (code points `U+0041`-`U+005A` and `U+0061`-`U+007A`)
- ASCII Numeric (code points `U+0030`-`U+0039`)
- The characters `_` and `.`.

r[modules.naming.absolute]
Absolute modules do not start with a dot `.`. They are resolved using [library module resolution].

r[modules.naming.relative]
Relative modules start with a `.`. They are resolved using relative resolution.

r[modules.resolution]
## Module resolution

The purpose of module resolution is to resolve a module name that has been specified in a source module to a path which should be loaded as the corresponding target module. The result of module resolution depends only on the interpreter’s library configuration, the current module, and the target module name. Interpreters MAY cache previously-loaded modules by path.

r[modules.resolution.common]
A module name can be resolved to a path by replacing all single dots except leading dots with the system path separator, and appending the `.sof` extension. Double dots are not replaced.

> [!NOTE]
> The system path separator is `/` on Unix-like systems, and `\` on Microsoft Windows. Other systems may have other separators, and `/` is used as a fallback if there is no concept of files, such as in WebAssembly.

Interpreters MAY allow alternative extensions for modules, but files with the `.sof` extension MUST take priority over any other files with the same base name during module resolution.

The resulting path is a relative path. Further resolution to a canonical absolute path depends on whether the module is relative or absolute.

r[modules.resolution.relative]
### Relative modules

Relative module names are resolved relative to the current module’s location. For this purpose, the relative path formed by the common module resolution procedure is appended to the directory of the current module.

r[modules.resolution.relative.up]
In relative module imports only, double dots `..` are used to import a directory higher. Each `..` causes the remaining module path to be interpreted at the next-higher directory instead.

r[modules.resolution.relative.up.limits]
The highest directory possible is the directory of the root module of the program if the relative import chain originates from the root module, or the directory of the libraries if the relative import chain originates from a library module that was imported absolutely.

r[modules.resolution.absolute]
### Absolute modules

Absolute modules import modules in the library directory. This is a runtime-constant directory. Interpreters SHOULD allow the user to change this directory at startup, but MUST NOT allow it to be changed during runtime.

r[modules.resolution.absolute.contents]
The library directory contains the SOF standard library. It MAY also contain modules added manually by the user or by package managers.

r[modules.resolution.absolute.use-relative]
Modules imported absolutely are allowed to perform relative imports. 

r[modules.pts]
## Module primitive tokens

[`use`], [`export`], and [`dexport`] are the only primitive tokens that directly interact with the module system.

r[modules.export]
## Exporting bindings from modules

Each module maintains an associated list, similar to a [nametable], of **exports**.

When exporting a (name, value) binding from a module, this binding is stored in that list.

If a binding for the same name already exists, that binding is overwritten.

r[modules.import]
## Importing module bindings

After a target module finishes executing, its exported bindings are **imported** into the source module that loaded it.

r[modules.import.opsem]
To import module bindings, all exported bindings are individually stored the source module’s global nametable. Existing bindings in the global nametable are overwritten with new bindings from the target module’s exports.

> [!NOTE]
> This behavior is exactly the same as if every exported item was `globaldef`’d in the source module.

[library module resolution]: #r-modules.resolution.absolute
[`use`]: PTs/Modules.md#r-pt.use
[`export`]: PTs/Modules.md#r-pt.export
[`dexport`]: PTs/Modules.md#r-pt.dexport
[nametable]: ../Naming.md#r-naming.nametable