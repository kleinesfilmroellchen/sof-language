# `math`

## `abs`: Absolute value

**Arguments** < a: Number

**Return value** < `|a|`: Number

Returns the absolute value of the given number.

## `sin`: Sine

**Arguments** < a: Float

**Return value** < `sin(a)`: Float

Returns the mathematical sine of the input. The input angle is treated as radians.

## `cos`: Cosine

**Arguments** < a: Float

**Return value** < `cos(a)`: Float

Returns the mathematical cosine of the input. The input angle is treated as radians.

## `tan`: Tangent

**Arguments** < a: Float

**Return value** < `tan(a)`: Float

Returns the mathematical tangent of the input. The input angle is treated as radians.

## `exp`: Exponent

**Arguments** < a: Float

**Return value** < `e^a`: Float

Returns e (Euler's constant, approximately 2.718281) to the power of a. This is the most accurate power function.

## `ln`: Natural logarithm

**Arguments** < a: Float

**Return value** < `ln(a)`: Float

Returns the natural logarithm of a. This is the most accurate logarithm function.

## `log`: Logarithm

**Arguments** < n: Float < a: Float

**Return value** < `log_n(a)`: Float

Returns the logarithm with the base of n of a. This is mathematically equivalent to `ln(a) / ln(n)`.

## `hypot`: Hypotenuse

**Arguments** < a: Float < b: Float

**Return value** < `hypot(a, b)`: Float

Returns the size of the hypotenuse with the adjacent and opposite being a and b. This is the value `sqrt(a*a + b*b)` as calculated by Pythagoras' formula, but it avoids overflows and imprecisions caused by large intermediary values.

