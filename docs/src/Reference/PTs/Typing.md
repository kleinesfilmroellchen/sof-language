# Typing-related PTs

This includes PTs that create or handle complex types.

## `[` (list marker)

This is a pseudo-value on the stack that does nothing and is invisible to almost all operations. It is used as a delimiter for the start of a list when the list creator `]` is used. 

## `]` (list creator)

**Arguments** < `[` < any number of elements (< `]`)

**Return value** < literal: List

Creates a new list from a number of literal values. This operation traverses the stack downwards until it hits the list start marker `[`; this means that the list creator operation is the only one that actually handles the start marker and doesn't just ignore it. All the elements in between are used as the initial values of the list, and they are ordered such that the lowest values are the first in the list. For example, the code `[ 1 2 3 ]` creates a list with the Integer elements 1, 2, and 3, in that exact order. This entire literal list creation system is therefore very intuitive while still respecting SOF's orthagonality.

## `,` (fieldcall operator)

**Arguments** < object: Object < field: Callable

**Return value** < object < value

Executes a call on the object's nametable with the given field as a Callable to execute. The field is usually an identifier identifying a field value on the object in question. It can however be any sort of callable data, including callables that do not interact with their nametable. The object is left on the stack below the return value, making it easily available for further processing.

## `;` (methodcall operator)

**Arguments** < object: Object < 0 or more < tocall: Callable

**Return value** object < any value or none

Calls the topmost element of the stack twice, like `:`. Additionally, remembers the object before the calls occur and places it back onto the stack below the return value of the second call. Furthermore, the function call actually happens with the object nametable as the function nametable, so the function can use object attributes as variables and modify the object with `def`'s. It is also possible to add new attributes to an object; for this reason, if you need named temporaries you should use an inner dummy function.

This operator is intended to be used with method-like named functions, functions that expect an object to operate on. SOF technically has no bound functions (you can emulate them by attaching function values to object attributes, but using those is a lot more cumbersome), so all functions that act like methods are free functions expecting some object to operate on. The big advantage is that as long as you pass an object that *behaves* like the one the function expects, the function will operate just fine. The method call operation will leave the program with the result of the operation (possibly none), and the original object below it, which allows for it to be used further.

## `constructor` (constructor creation operator)

**Arguments** < code: CodeBlock < args: Integer

**Return value** < constructor: Constructor

Creates a new object creation template by turning the code block into a constructor function. Inside the constructor's code block, `def`s can be used to initialize fields on the object's nametable. A new object can be created by executing the constructor; this is the earliest time that the code body is executed. Just like other functions, the constructor can obtain any number of arguments when called.
