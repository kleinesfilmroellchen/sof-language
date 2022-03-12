# Arithmetic and Logic PTs

## `+` (add operator)

**Arguments** < left: Number < right: Number

**Return value** < Mathematically: `left + right`: Number

Computes the sum of the two arguments. The result is an Integer if both arguments are Integers and a Decimal if any argument is a Decimal. Throws `TypeError` if any of the arguments has a non-number type.

## `-` (subtract operator)

**Arguments** < left: Number < right: Number

**Return value** < Mathematically: `left - right`: Number

Computes the difference between the two arguments. The result is an Integer if both arguments are Integers and a Decimal if any argument is a Decimal. Throws `TypeError` if any of the arguments has a non-number type.

## `*` (multiply operator)

**Arguments** < left: Number < right: Number

**Return value** < Mathematically: `left · right`: Number

Computes the product of the two arguments. The result is an Integer if both arguments are Integers and a Decimal if any argument is a Decimal. Throws `TypeError` if any of the arguments has a non-number type.

## `/` (divide operator)

**Arguments** < left: Number < right: Number

**Return value** < Mathematically: `left ÷ right`: Number

Computes the result of the first argument divided by the second argument. The result is an Integer (division with remainder) if both arguments are Integers and a Decimal (division) if any argument is a Decimal. Throws `TypeError` if any of the arguments has a non-number type. Throws `ArithmeticError` if the right argument is zero.

## `%` (modulus operator)

**Arguments** < left: Number < right: Number

**Return value** < Mathematically: `left mod right`: Number

Computes the result of the first argument modulus by the second argument. First, any Decimals are converted to Integers. Then, the remainder of the integer division of the two arguments is computed and returned. Throws `TypeError` if any of the arguments has a non-number type. Throws `ArithmeticError` if the right argument is zero.

## `<<`, `>>` (logical bit shift operators)

**Arguments** < base: Number < amount: Number

**Return value** < base (`<<` or `>>`) amount: Integer

Computes the logical bit shift, that is the base (first argument) shifted left (<<) or right (>>) amount number of bits. Because this is a logical shift, it does not sign-extend the base. If this operation receives Floats as arguments, it truncates them to Integers.

## `<`, `>`, `>=`, `<=` (comparison operators)

**Arguments** < left: Number < right: Number

**Return value** < Result of the comparison: Boolean

Compares the two arguments, always in the form `left <comp> right`. The operators are less than, greater than, less than or equal, greater than or equal, respectively. This operation throws a `TypeError` if any of the arguments is not a Number.

## `=`, `/=` (equality operators)

**Arguments** < left < right

**Return value** < Whether the values are equal/unequal: Boolean

Checks whether the two arguments are equal or not equal, respectively. Two arguments are compared using the following algorithm:

- If both arguments are Numbers: Check whether their numeric value is equal. Integers are converted to Floats if at least one of the arguments is a Float.
- If both arguments are Booleans: Check whether they represent the same truth value.
- If both arguments are Strings: Check if every single one of their characters matches in order.
- If both arguments are Objects: Check if their nametables contain the same value for each key and whether they contain the same list of keys. The values are checked with this same algorithm.
- If both arguments are any other builtin value: Return false. This applies most importantly to CodeBlocks and Functions, as there is no simple way of determining their equality.

If the arguments aren't of the same type, upcasting is done, where Booleans upcast to Numbers and all other types upcast to Strings. This means that, for example, `"2" 2 =` holds true. For stricter equality, check the types first.

## `and`, `or`, `xor` (binary logic operators)

**Arguments** < left < right

**Return value** < result of the operation: Boolean

Compares the two operators according to their Boolean value. The algorithm of finding the boolean value is the exact same as [`convert:bool`](Builtin-functions) uses.

## `not` (negation operator)

**Arguments** < arg

**Return value** < result of the negation: Boolean

Negates `arg`'s value; if it was true, it is now false, if it was false, it is now true. If the argument is not a Boolean, its truthiness value is determined according to `convert:bool`.
