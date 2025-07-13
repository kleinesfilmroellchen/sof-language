# Functional

## `.` (call operator)

**Arguments** < 0 or more < tocall: Callable

**Return value** < any value or none

Calls the topmost element of the stack. Each type exhibits its own behavior when called, but the most basic are:

- Most primitives return themselves.
- Calling an identifier looks up that identifier's value in the innermost scope that has the identifier defined or throws a `NameError` if that fails.
- Calling a namespace looks up the next stack element, an identifier, in that namespace. This process is recursive, although currently namespaces cannot be nested.
- Calling a function executes it and consumes the specified number of arguments from the stack. The return value of the function is placed on the stack. An exception to this is when the function arguments are "blocked off" with a currying operator (see below). In that case, the function is not called but it and the curried arguments replaced by the curried function.
- Calling a code block executes it. There are neither arguments nor a return value.

## `:` (doublecall/function invoke operator)

**Arguments** < 0 or more < tocall: Callable

**Return value** < any value or none

Calls the topmost element of the stack twice. I.e., after the first call, the now topmost element is immediately called again. Therefore, this PT is a shortcut that is exactly equivalent to (and faster than) `. .`. This is intended for convenient use of named functions, i.e. functions defined into a namespace. The first call will retrieve the function itself onto the stack, and the second call will execute it. Therefore, the normal way you will see functions be used is with `:`, and it's an easy indicator of distinguishing a variable lookup from a function call.

## `|` (currying marker)

This is a pseudo-value on the stack that does nothing and is invisible to almost all operations. It is used to limit the number of arguments a function receives. If a function, while retrieving arguments from the stack for calling, encounters a currying marker before all necessary arguments are found, the function is not called but a curried function is created instead that has the specified number of arguments "pre-stored". The curried function can be called later, it takes the number of remaining arguments. These appear on the stack below the curried arguments, so that the actual argument order inside the function doesn't change.

## `function` (function definition operator)

**Arguments** < code: CodeBlock < argcount: Integer

Creates a function with `argcount` arguments. Usually, it is then `def`'d or `globaldef`'d with a name.

## `nativecall` (native function invocation operator)

**Arguments** < any number of arguments < native function identifier: String

**Return value** < any

Calls a function defined natively in the interpreter. [Learn more](Language-internals#native-calls).

## `return` (function return operator)

**Arguments** < any

Saves the topmost value on the stack as the current function’s return value, then returns from the function immediately, exiting all non-function scopes (like code blocks) and clearing the stack down the function’s scope.

## `return:0` (value-less function return operator)

Returns from the current function just like `return`, but does not return any value (and does not consume anything from the stack).
