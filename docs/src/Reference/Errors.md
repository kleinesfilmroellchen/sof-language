r[error]
# Errors

r[error.intro]
When a problem is encountered with the user-supplied SOF programs or their state during execution, the interpreter MUST throw an error.

> [!NOTE]
> Currently, errors cannot be caught. A feature for catching errors may be added later via the `except` token.

Some errors may not happen in certain interpreters, as there are no strict conditions causing them. These errors MAY be not implemented in the interpreter.

r[error.termination]
The action of throwing an error terminates the program.

r[error.order]
When, during execution, any operation fulfils multiple error conditions, and therefore many different errors may be thrown, the exact error thrown (first) is unspecified.

r[error.ui]
Interpreters MUST inform the user of the fact that an error has occurred. They SHOULD provide further information, such as (but not limited to):

- Where the error occurred in the program. This includes (if applicable) the current token that was executed when the error occurred, the call stack of functions and other callables that were in progress at the time, and the state of the main stack itself.
- The exact condition which caused the error. For example, for an ArithmeticError, the interpreter SHOULD report what operation was invoked with which exact operands.
- Resources to learn about the error and how to prevent it, such as documentation links.

r[error.kinds]
Several kinds of errors exist. The following listing contains all the errors defined by and used in the reference. Implementations MAY add additional errors as subtypes of the predefined ones. The conditions under which errors are throw are defined throughout the reference in the context of specific operations and descriptions of SOF execution.

- SyntaxError
- TypeError
- NameError
- ArithmeticError
- StackAccessError
- StackSizeError
