# The SOF Module System

SOF's module system is intended to be simple, but flexible and practical. It is very reminiscent of Python's module system.

## Modules, files, and folders

Each SOF source code file is a separate module. Folders are not special, they can just serve to group modules and avoid naming conflicts. There are no special module names such as __init__ or __main__ in Python, all files ending in `.sof` are accessible equivalent modules.

Modules are named hierarchically with familiar dot syntax. Modules starting with a dot `.` are relative modules, and modules starting with any other character are absolute modules.

Relative modules import relative to the file location. Single dots (except the leading dot) are used to import one directory lower, i.e. the name between this dot and the one before it is considered a directory in with to look for the module. Double dots `..` are used to import a directory higher (cf. directory navigation in all major operating systems). The highest directory possible is the directory of the base module of the program if the relative import chain originates from the base module, or the directory of the libraries if the relative import chain originates from a library module that was imported absolutely. This distinction prevents nonsensical and dangerous "upwards" imports while allowing for useful features like sibling folder importing.

Absolute modules import in the library directory. This is a runtime-constant directory which will later be accessible with command-line arguments and/or environment variables. It usually sits in a related directory to the SOF executable itself. The library directory contains not only the SOF standard library modules but also any modules added manually by the user or by package managers. Modules imported absolutely can import relatively themselves, which again allows for submodule structures even in the libraries. Within an absolute module, single dots can also be used to import in sub-directories of the library directory.

The module name, i.e. the name after the final dot, never contains the `.sof` ending. This allows for the alternative endings and special file formats which are treated specially by the module system, like `.soflib`.

Each naming segment in any module specification, which represents either folders or the final file, can contain all characters except the two slash characters (used by the operating systems for directory structure) and dots, of course.

Given this detailed description, the method of resolving modules is unambiguous and straightforward. Modules are always treated with UTF-8 encoding, just as all SOF files are.

## Names

As SOF has no namespaces like C, care needs to be taken when naming functions and other exports of a module. As they overwrite all GNT entries of the same name upon import, duplicate definitions are technically allowed (though the interpreter might issue a warning). The convention [as outlined in the programming conventions](../Programming-conventions.md) is to use underscores for separating pseudo-namespaces where necessary.

## The `use`, `export` and `dexport` primitive tokens

The `use` primitive token is used to import a module. The module specification, its behavior explained above in detail, is given by a string. The SOF module system imports the specified module, which may come from the internal cache if it was already imported. Then, all of the bindings defined by `export` or `dexport` are imported into the importing file's global namespace. This means that you don't have to worry about cluttering global namespaces with unnecessary names: only the names you export in a module are visible to `use`rs of that module.

Note that of course, `use` is recursive. SOF code that is currently executed as part of a module import can `use` other modules without any different rules or exceptions. The only impossible module connection is any sort of circular import. The reason is equivalent to Python's reason: Because module importing always involves executing the entire imported module's source code. However, given the huge ecosystem of Python libraries, it is clear that this is not a limitation and all circular dependencies can be reworked to strict hierarchical dependencies.

## Running functions in other modules

There is special treatment given to exported functions, which technically is a special rule about all functions but only becomes relevant with cross-module functions. All functions store the global nametable at the time of definition; i.e. the global nametable of their module or file. As each module gets its own global nametable, this means that functions in different modules refer to different GNTs, but functions in the same module refer to the same GNT. When a function is run, the global nametable is in fact temporarily replaced with the function's global nametable at defintion time (if necessary) and restored afterwards. This means that a function can access global values of its module like one would expect. To keep the orthagonality, the global nametable exchanging can be thought of as a stack of global nametables at the very bottom of the real stack. The actual global nametable is just the top of this sub-stack, and global nametables are pushed to and popped from the stack on function entry and exit.
