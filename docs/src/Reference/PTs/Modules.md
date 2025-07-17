# Modules

## `dexport` (definition+export operator)

**Arguments** < value: Value < name: Identifier

`name dexport` is syntactic sugar for `name globaldef name export`. This operator simply binds the value to name in the GNT and also exports it.

## `export`

**Arguments** < name: Identifier

Exports the value bound to name in the LNT. Exporting is the method of making data visible to other SOF modules that import this module. Only exported names, not all names in the GNT, will be available to the module after import.

## `use` (import module)

**Arguments** < module name: String

This PT is part of the module system, documented [here](../Module-System.md). It executes modules and imports their exported definitions into the global namespace.
