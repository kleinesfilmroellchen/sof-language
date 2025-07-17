# Builtin functions

These functions are always available to the user, and part of the `prelude` file in the standard library. The difference to other modules is that the prelude file is executed as if its text was in the file itself, so normal module mechanisms don't apply unless you explicitly `"prelude" use`.

## `convert:bool` (Not implemented)

**Arguments** < toConvert

**Return value**: converted: Boolean

Converts the argument to a Boolean. If the value is not already a Boolean, it uses the "truthyness" of the argument, which is almost always true. When the argument is 0 or 0.0, it is false.

## `convert:decimal`

**Arguments** < toConvert

**Return value**: converted: Decimal

Converts the argument to a float. The argument can either be a string containing a valid SOF float literal (plus any leading/trailing whitespace), an integer or a float already. The result is the corresponding float value; the function fails with a TypeError if conversion fails, e.g. wrong number format, unsupported origin type.

## `convert:int`

**Arguments** < toConvert

**Return value**: converted: Integer

Converts the argument to an integer. The argument can either be a string containing a valid SOF integer literal (plus any leading/trailing whitespace), an integer already or a float to be rounded. The result is the corresponding integer value; the function fails with a TypeError if conversion fails, e.g. wrong number format, unsupported origin type.

## `convert:string`

**Arguments** < toConvert

**Return value**: converted: String

Converts the argument to its string representation. This is the same process used by the output methods. The argument can be of any type, as any SOF type has a string representation, but the result might not be beautiful.

## `convert:callable`

**Arguments** < toConvert

**Return value**: converted: Callable

Converts the argument to its callable equivalent. This has the following result:

- "Real" callables are unchanged. This affects functions, code blocks and identifiers.
- Primitives are converted to a [Church encoding](https://en.wikipedia.org/wiki/Church_encoding) version of themselves. (Not implemented) This means:
  - Natural numbers `n >= 0` are converted to a callable that when called with another callable `f`, will call `f` `n` times. If `f` returns a value and receives an argument, this is exactly equivalent to the notion of Church numerals.
  - Booleans are converted to a callable that when called with two arguments, will return the first (stack-lowest) argument if it is `true`, otherwise, it will return the second argument. This also means that `ca cb`**`cond if`** (`ca`, `cb` Callables, `cond` Boolean) is equivalent to `ca cb cond`**`convert:callable : .`**
  - Other Integers `x` are converted to a two-element list `[a, b]` where `a, b ∈ ℕ` are Church numerals as described above and `x = a - b`.
  - Decimals `x` are first converted to the most accurate rational representation. Then, a two-element list `[k, a]` is created where `k` is a Church numeral (integer), `a` is a Church-encoded natural number and `x = k / ( 1 + a )`.

The conversion fails on Strings and other more complex types and throws a TypeError.

## `random:01`

**Return value**: random number: Decimal

Generates a pseudo-random number between 0 (inclusive) and 1 (exclusive), optimally using a system-provided RNG (such as `/dev/urandom` on Linux). **THIS PSEUDO-RANDOM NUMBER GENERATOR IS NOT GUARANTEED TO BE CRYPTOGRAPHICALLY SAFE.**

## `random:int`

**Arguments** < start: Integer < end: Integer

**Return value**: random number: Integer

Generates a pseudo-random number between start and end, inclusive. Uses `random01` as the initial source of randomness (and, therefore, is NOT CRYPTOGRAPHICALLY SAFE).

## `random:decimal`

**Arguments** < start: Decimal < end: Decimal

**Return value**: random number: Decimal

Floating-point variant of `random:int` with equivalent behavior. Returns any floating point number between start and end, inclusive.

## `fmt'x`

**Arguments** < format: String < x format arguments

**Return value**: formatted: String

Formats a string with a number of format arguments. `fmt'x` is just a placeholder name; the actual functions are called `fmt'0` through `fmt'9` with 0-9 arguments, respectively. The exact format specificer format is not well-documented and can be found with the relevant native implementations. It's similar to Java's format string syntax though, and some tests exist for it.

## `pair`: Create a tuple

**Arguments** < a: Any value < b: Any value

**Return value** < \[ a, b \] : List

Creates a two-element list from the two arguments. Main function for creating tuple-like lists (short lists of known length) and returning two values.
