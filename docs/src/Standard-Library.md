# The SOF standard library

This is the official collection of library functions and classes provided by the SOF system.

<!-- ## How to write your own library

A library is simply a collection of related functionality, possibly implemented in a native Java class (explanation/implementation plan coming soon!). The tool `sof packlib` can pack a library by investigating the current or a given directory and combining all the found `.sof` source files into one `.soflib` file that is recognized by SOF's `use` PT. -->

## Methods on built-in types

When performing a field call with certain identifiers, certain methods can be retrieved. This allows invoking those methods on primitive types, for example:

```sof
# sine of 3.2
3.2 sin ;
# retrieve first element of list
0 [ 1 2 ] idx ;
```

Defined methods are described separately for each type in the sub-sections.

## Files in the standard library

- `math`: Usual mathematical operations.
- `op`: Built-in operations as callables.
- `io`: (Not implemented) File input/output.
- `fp`: (Not implemented) Helpers and tools for functional programming.
