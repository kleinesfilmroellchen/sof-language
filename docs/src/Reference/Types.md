# Types

Any value that is contained on the stack and visible to the SOF program has one of a few types. Types form a hierarchy, with certain types being considered subtypes of others. This means that any operation allowed on the parent type is also allowed on the subtype, with possibly diverging behavior.

Some types are considered **hidden**. This means the user cannot fully interact with them.

## Value

Base type of all other types.

## Callable

Technically speaking: Any type that can be operated on with the call operator `.`. But as this applies to every type, Callable is mostly used as a term to mean more complex types that actually execute logic when called. The most important callables are Identifier (performs nametable lookup), CodeBlock, and Function.

## Identifier

A specialized string-like type that can only contain specific characters (mostly letters and numbers). Used for name binding, i.e. the def and call operator families.

## Primitive

Any type that returns itself when called and can be specified with a simple literal. All the following basic data types are primitives.

### Number

Base type for number types. Arithmetic operations only operate on Numbers.

#### Integer

Normal integral positive/negative Number. The reference implementation uses 64-bit storage, but this is not mandatory. Ideally, Integers have no limit on their size other than memory.

#### Float/Decimal

Floating-point Number, 64 bits in the reference implementation. May be indefinitely precise.

### Boolean

Truth value, either `true` or `false`, the basis of program control flow. When called (future) it takes two elements from the stack and returns the lower one if it is `true`, or the higher one if it is `false`.

### String

Piece of text, infinitely long (memory-limited), all Unicode characters/code points supported.

## CodeBlock

A code block created with curly braces; contains SOF code to be executed without a safe environment or arguments/return values. The most basic Callable that has user-definable behavior; therefore, it is often used to compose operations and Callables that require other Callables.

## Function

A combination of a CodeBlock to be executed and a positive integer amount of arguments, possibly 0. When called, protects its internals through the use of an FD and places its arguments above that on the stack to be used by its code.

## Object

A data collection like a nametable, with the difference that objects are user-creatable and do not serve the role of stack delineation or as targets for `def`.

## Constructor

A special kind of function that, when called, creates a new object and is able to initialize that object.

# Errors

SOF throws a variety of errors when you mess something up. Currently, errors cannot be caught; for the possibility of adding this feature later, the `except` PT is reserved.

## SyntaxError

The input is not correct SOF syntax. This will occur:

- on unclosed strings, block comments and braces
- on too many closing braces
- on wrong Integer, Boolean and Decimal literals
- on invalid identifiers

Syntax errors are unrecoverable, that is, they may never be caught.

## TypeError

An operation was attempted on incompatible or unsupported types. This will occur:

- on any native operation that has typed arguments.
- on PTs that require specific types. E.g.: `def`, call operator.

## NameError

It was attempted to retrieve the value of an identifier that is not defined in the current scope(s).

## ArithmeticError

An illegal mathematical operation was attempted: mostly divide by zero or its derivatives.

## StackAccessError

An operation attempted an illegal modification of the Stack:

- The end of the stack was reached; accessing the GNT/NNT is not allowed.
- An FD was reached; Stack access beyond it is not allowed.

## StackSizeError

(future) The Stack has reached the maximum feasible size. This will most likely only occur on recursive programs with deep recursion levels.
