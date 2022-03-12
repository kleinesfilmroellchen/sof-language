# Stack & Naming

## `def` (definition operator)

**Arguments** < value: Value < name: Identifier

Modifies the LNT by setting the key-value pair `name: value`. This means that now the value of the identifier `name` is "defined" to be the value `value`, hence the name. Will overwrite any existing binding to `name`.

## `dup`

**Arguments** < elmt: Value

**Return value** < elmt < elmt

Duplicates the topmost element on the stack.

## `globaldef` (global definition operator)

The same as `def`, but always defines into the GNT.

## `pop` (stack remove operator)

**Arguments** < any

Removes the topmost element from the stack and discards it.

## `swap` (stack exchange operator)

**Arguments** < x < y

**Return value** < y < x

Exchanges the position of the two top-most elements on the stack.

