# Control flow

## `dowhile` (loop with at least one iteration)

Identical to `while`, but will execute the body callable before checking the condition, which results in at least one call to the body.

## `if` (conditional execution operator)

**Arguments** < to execute: Callable < condition: Boolean

Executes the callable if `condition` is true.

## `ifelse` (conditional execution operator with alternative)

**Arguments** < to execute: Callable < condition: Boolean < to execute otherwise: Callable

Executes the first callable if `condition` is true. Otherwise, executes the second callable.

## `switch` (multi-ifelse conditional execution operator)

**Arguments** < "`switch::`" (Identifier) < ( case body: Callable < case condition: Callable ) * any number of times < default body: Callable

Compact alternative to nested `ifelse`'s. The behavior of this is as follows:

The default body, the element last on the stack, is stored for later use. Then, the entire stack is traversed two elements at a time. If the first element is the identifier "`switch::`", the beginning/end of the switch has been reached; this special identifier serves as a sort of label to delineate the statement from the other, likely important stuff on the stack. As no case has been executed yet, the default body is executed.

If, however, the first element is a Callable, it is executed and the algorithm expects a Boolean value to be situated on top of the stack afterward. If this Boolean is true, the second element, the corresponding case body, is executed. Otherwise, the search continues.

## `while` (loop function)

**Arguments** < body: Callable < condition: Callable

For every iteration of executing the body, executes the condition callable, which should place a Boolean value onto the stack. If the boolean value is true, the body will be executed once and the cycle repeats, if it is false, the loop will end.
