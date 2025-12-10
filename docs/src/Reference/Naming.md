r[naming]
# Naming

r[naming.function]
## Function scoping

For the purposes of function scoping, constructors are also considered functions.

r[naming.function.nametable]
When a function is called, a nametable specific to this function invocation is placed just below the function arguments. This function nametable is initially empty. Once the function returns, this nametable is discarded.

r[naming.function.global-nametable]
Each function has a reference to the global nametable that was in effect during its creation with the [`function`] PT. During function execution, the global nametable is the same as the global nametable when the function was created.

> [!NOTE]
> This allows functions from any module to execute in any other module with the “obvious” behavior surrounding globals. The globals do not suddenly change just because the function is being executed elsewhere. Importantly, this means that functions have access to private (module-internal) functionality and data, supporting encapsulation.

[`function`]: PTs/Functional.md#r-pt.function
