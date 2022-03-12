# Primitive Tokens

Every primitive stack operation is called a primitive token. It is listed with its arguments, the stacklowest argument first, and its return value description. No return value section means that this operation places nothing on the stack. Get familiar with this argument and return type shorthand, it is used in all the documentation.

### Operation special cases for identifiers

In order to make in-place modification of defined values easier, it's possible to combine any of the binary operations `+`, `-`, `*`, `/`, `%`, `<<`, `>>`, `<`, `>`, `<=`, `>=`, `=`, `/=`, `and`, `or`, `xor` with an identifier as the first (right, lower) argument. This will retrieve the value from the identifier through a `.`-like call, perform the operation on that value as the right argument instead, and then store the result back into the identifier like normal `def`. The result does not remain on the stack. This is equivalent to the +=, -= etc. operator found in many programming languages.

## Miscellaneous tokens

### `input` (string input function)

**Return value** < the token read from stdin: String

Reads one word, i.e. everything in the standard input up to the first (Unicode) whitespace character, without trailing or leading whitespace characters.

### `inputln` (line string input function)

**Return value** < the line read from stdin without line terminator(s): String

Reads one line (any combination of line separators end one line) from standard input.

### `write` (output function)

**Arguments** < output: String

Writes the argument to standard output.

### `writeln` (output function w/ line break)

**Arguments** < output: String

Writes the argument to standard output and terminates the line.
