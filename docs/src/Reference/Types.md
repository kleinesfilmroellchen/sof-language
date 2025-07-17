r[type]
# Types

Any value that is contained on the stack and visible to the SOF program has one of a few types. Types form a hierarchy, with certain types being considered subtypes of others. This means that any operation allowed on the parent type is also allowed on the subtype, with possibly diverging behavior.

r[type.error]
Any operation may specify the operand types on which it is valid. Supplying any operation with an operand of a different type causes a [TypeError](Errors.md).

r[type.hidden]
Some types are considered **hidden**. This means the user cannot fully interact with them.

r[type.value]
## Value

**Value** or **Any** is used to refer to any type, or the union of all types.

r[type.callable]
## Callable

**Subtype of:** Value

Any type that can be operated on with the call operator `.`.

r[type.identifier]
### Identifier

**Subtype of:** Callable

A string-like type for textual identifiers used in name binding.

r[type.identifier.call]
Invoking a call on an identifier performs name lookup.

> [!NOTE]
> Identifiers provide almost none of the functionality of strings. They are not intended to be used as an alternative string type.

r[type.primitive]
### Primitive

**Subtype of:** Callable

Any type that can be specified with a simple literal.

r[type.primitive.call]
Invoking a call on a primitive performs the identity operation, i.e. it returns the primitive’s value. `Boolean` is an exception to this.

r[type.number]
### Number

**Subtype of:** Primitive

Any numeric type. Arithmetic operations only operate on numbers.

r[type.integer]
#### Integer

**Subtype of:** Number

Integral signed number type. MUST be represented in two’s-complement. All operations perform wrapping arithmetic by default, discarding any carries. Minimum representable range must be $\[ -2^{63} ; 2^{63}-1 \]$, i.e. 64 bits.

r[type.decimal]
#### Decimal

**Subtype of:** Number

Real number type. The precision required should either match DEC64[^dec64], or IEEE 754-2019[^float] double-precision floating-point. Higher precision is allowed.

r[type.decimal.nan]
A *NaN* value of unspecified representation MUST be available. Infinite values may be available and may have specific behavior in certain operations in interpreters that support them. Otherwise, infinities have the behavior of NaN.

r[type.boolean]
### Boolean

**Subtype of:** Primitive

Truth value, either `true` or `false`.

r[type.boolean.call]
When called, it takes two elements from the stack and returns the lower one if it is `true`, or the higher one if it is `false`.

r[type.string]
### String

**Subtype of:** Primitive

A string is a sequence of Unicode code points, represented in UTF-8. Its maximum length may be arbitrarily large.

r[type.codeblock]
## CodeBlock

**Subtype of:** Callable

A code block is a list of tokens, created by enclosing these tokens in a pair of braces `{` and `}`.

r[type.codeblock.call]
When called, it executes the list of tokens.

r[type.function]
## Function

**Subtype of:** Callable

A function is a combination of a list of tokens to be executed and a positive (or zero) integer amount of arguments.

r[type.function.call]
When a function is called, it retrieves a number of values from the stack equal to the number of arguments. Then, it places a Function Nametable on the stack. Then, it places the arguments back on the stack, in the same order.

r[type.object]
## Object

**Subtype of:** Value

An object is a key-value store that the user can freely modify. When a method is called on the object, the object is used as a nametable, allowing easy modification via normal definition operations.

r[type.constructor]
## Constructor

**Subtype of:** Callable

A constructor is a special kind of function responsible for object creation.

r[type.constructor.call]
When called, it creates a new object and is able to initialize that object.

---

[^dec64]: Douglas Crockford: *DEC64*. <https://www.crockford.com/dec64.html>
[^float]: "IEEE Standard for Floating-Point Arithmetic," in IEEE Std 754-2019 (Revision of IEEE 754-2008), vol., no., pp.1-84, 22 July 2019, doi: 10.1109/IEEESTD.2019.8766229.

