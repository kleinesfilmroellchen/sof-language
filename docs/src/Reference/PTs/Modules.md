# Modules

r[pt.dexport]

## `dexport` (definition+export operator)

r[pt.dexport.arguments]
**Arguments** < value: Value < name: Identifier

The `dexport` primitive token binds and exports a name-value pair. It is a convenience feature that has identical behavior to `<name> globaldef <name> export`.

r[pt.dexport.opsem]
The runtime behavior of `dexport` is as follows:

1. [Store the (name, value) binding in the global nametable](../Naming.md#r-naming.global-bind).
2. [Export] the (name, value) binding from the current module.

r[pt.export]

## `export`

r[pt.export.arguments]
**Arguments** < name: Identifier

The `export` primitive token exports the value bound to the name in the module namespace.

r[pt.export.opsem]
The runtime behavior of `export` is as follows:

1. [Lookup] the value associated with the given name in the current scope.
2. [Export] the (name, value) binding obtained in the previous step from the current module.

r[pt.use]

## `use` (import module)

r[pt.use.arguments]
**Arguments** < module name: String

The `use` primitive token is used to import a module.

r[pt.use.opsem]
The runtime behavior of `use` is as follows. For the purposes of this description, “source module” refers to the module that contains the `use` PT in question, and “target module” refers to the module being imported.

1. Resolve the target module named by the given module name, using the [module resolution algorithm]. The result of module name resolution is an unambiguous file system path.
2. Load the SOF [source file] from the file system path determined in the previous step, and propagate any errors. This step MAY be cached.
3. Place a [nametable] on top of the stack.
   - This nametable will be referred to as the “module” nametable.
   - For the remainder of the execution of the target module, the module nametable functions as the global nametable. Any specification referencing the global nametable therefore references the module nametable while the module is being executed.
4. [Execute] the token list of the target module’s source file.
5. Remove the module nametable from the stack, as well as all tokens above it.
6. [Import] all names into the source module which were [exported] from the target module.

[module resolution algorithm]: ../Module-System.md#r-modules.resolution
[source file]: ../Language-Specification.md#r-source-file
[nametable]: ../Naming.md#r-naming.nametable
[execute]: ../Language-Specification.md#r-execution
[Import]: ../Module-System.md#r-modules.import
[exported]: ../Module-System.md#r-modules.export
[Export]: ../Module-System.md#r-modules.export
[Lookup]: ../Naming.md#r-naming.lookup
