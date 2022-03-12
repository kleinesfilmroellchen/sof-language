# How does SOF actually work?

This page shall describe the way that SOF works internally, while staying language-independent, as to accomodate other implementations of SOF compilers and interpreters. Nevertheless, Examples of the reference Java implementation shall be given, as it currently is the only existing implementation of SOF.

## Data Structures

SOF is a pure stack-based language. That means: All data always resides on the Stack, a linear unit of memory cells that contain data. Although we know a stack as being only LIFO and having one single visible element (the top, head or first element of the stack), in practice the SOF stack should be a **Deque**. If it wasn't, one would need two independent stacks for many operations (but both of them could be real, pure stacks).

But what is a Deque? This term, pronounced "deck", is short for "double ended queue" and describes a data structure with arbitrary access on both ends. The top of the deque is accessed with peek, pop and push, while the bottom of the deque is accessed with peekLast, popLast, pushLast. Java provides not only a Deque in its Collection framework, but also many specialized implementations, such as the currently used `ConcurrentLinkedDeque`, which is a double-linked-list implementation with threadsafety.

All data lives on the stack, this was already stated. But how does this allow for named variables, namespaces and function calling? The answer is the second most important data structure of SOF, the **Nametable**.

A Nametable is simply a list of key-value mappings (`Map` in Java and JavaScript, `dict` in python) that maps identifiers to any SOF data. Pretty simple, but this powers all of SOF. All defintions made by `def` are simply entries into nametables, the Call operator `.` simply accesses entries in nametables.

### The Global Nametable

The global nametable (GNT) is always the lowest element on the stack; when it is missing, something serious has gone wrong. The SOF programmer can never inspect, modify or remove the GNT, but it is being used all the time:

- All defintions made on a global level enter the GNT
- All imported NNTs (see below) are placed in the GNT

The GNT will be discussed in further detail with its use cases.

## Scoping, the Call and Def operators

A Scope is created whenever a function starts. The scope is signaled by a special NT on the stack, called a function delimiter (FD). FDs hold special information on where to return execution when the function ends and what the return value is. Also, the FD cannot be taken off the stack by the program in any other way than returning from executing the current code block or function.

As FDs are NTs, this means that at any point in the program there could be many NTs on the stack at once. To figure out which NT is to be used for the call operator `.` with an Identifier, the following simple rule is applied: **Walk down the stack from top (last) to bottom (first). Whenever a Nametable is encountered, determine whether it contains the identifier that `.` wants to call. If so, retrieve this identifier's value from this Nametable, if not, continue the search all the way to the NNT/GNT. If nothing is found, throw a NameError**. This ensures that definitions made in a "more local" NT are more important and hide those in a "more global" NT.

Normally, `def` operates on the **Local Nametable** (LNT), which is simply the highest NT on the stack. This may be an FD, or the GNT if there is no FD. This importantly means that **code inside a function cannot modify nametables outside unless using the `globaldef` operator**:

Sometimes, the user wants to define into the GNT. For this, the operator `globaldef` is provided, which exhibits the same behavior as `def` except for always defining into the GNT. This is useful for defining functions in an enclosed scope, modifying global variables and so on. It is also convention to always `globaldef` global functions, constructors, etc.

## Native calls

The PT `nativecall` executes a call to a natively implemented function. The only explicit argument of the native call is a string identifying the native function to call. Because the reference implementation is Java-based, the way in which native functions are identified is very similar to Java method signatures. The general form is `NativeFunction = Package { "." Package } "." Class "#" Method "(" [ ArgumentType ] { "," ArgumentType } ")"`, where Package represents a legal Java package name, Class represents a legal Java class name and Method represents a legal Java method name. The argument types must be the internal SOF type names that the reference implementation uses, like StringPrimitive, FloatPrimitive etc. There may be any number of arguments separated by a comma (but no spaces!), and possibly none. The arguments are taken from the stack when `nativecall` runs, where the last argument taken from the stack is the first argument passed to the native function. The native function may return an SOF typed result, in which case it is placed on the stack, or it may return nothing (void), in which case the stack is not modified. Native functions may throw (incomplete) compiler exceptions, in which case they propagate from the `nativecall` as normal SOF errors of type `Native`.
