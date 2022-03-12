# How to write readable SOF code

This page will outline the conventions, idioms and common practices used when writing SOF code. Following this guide leads to good, idiomatic SOF code and APIs that other developers can use with ease. Most sections are in no particular order.

## Whitespace use

As SOF is very whitespace insensitive, good code uses whitespace to logically structure the otherwise pretty one-dimensional series of tokens.

#### Token separation

Tokens on one line are separated with one single space. An exception is made when you want to align groups of tokens in multiple sequential lines: then, use of multiple spaces between tokens for alignment is encouraged.

*Example*: Defining a number of variables in sequence: Don't do this:

```sof
3 x def
"string" msg def
2 4 * eight def
```

Do this:

```sof
3        x     def
"string" msg   def
2 4 *    eight def
```

#### Line breaks and token grouping

Each line should contain one single action, or a logical unit of actions, which might require multiple tokens. In general, PTs should appear on the same line as their arguments, except when these arguments are already on the stack or require a lot of steps to be prepared.

For PTs that take code blocks, such as `if`, the last closing brace and the PT itself should be on the same line, except when line length would be a problem.

Code blocks should be split up into lines, where the opening and closing brace are on their own line, mimicking the "braces on next line" code style found in C-like languages. Exceptions are when the code block is very short (1-2 tokens) or contains only a single logical action (such as a function call with many parameters).

*Example*: Don't do this:

```sof
# define function
{ pop 15 + return } 2 function someFunction globaldef
# take user input, process it, store it, store a modified version, print one of two messages
input convert:int : true someFunction : dup x def 3 + y def { "large" } { "small" } y . 33 < ifelse
```

Do this:

```sof
# define function
{
    # discard first argument
    pop
    # compute something
    15 + return
} 2 function someFunction globaldef

# take user input
input convert:int :
# process it
true someFunction : dup
# store it
x def
# store a modified version
3 + y def
# print one of two messages
{ "large" } { "small" } y . 33 < ifelse
```

#### Line indentation

Indenting one level should only be done inside code blocks; this also includes functions and methods. The braces of the code blocks themselves should be on the original indentation level, mimicking the "braces on next line" code style found in C-like languages. Whole-line comments are indented as code would be.

*Example*: Don't do this:

```sof
3 x def
    {
# a comment
    3 someComputation :
 2 someComputation :
      4 writeln
 } 3 4 < if
```

Do this:

```sof
3 x def
{
    # a comment
    3 someComputation :
    2 someComputation :
    4 writeln
} 3 4 < if
```

An exception to the indentation rule are methods: They, together with the constructor, should be aligned one level further than the surrounding code. The constructor defining calls themselves ( `3 constructor <classname> globaldef <classname> :` ) should be on the same indentation level as the surrounding code.

## Naming conventions

General naming in SOF is done with CamelCase. All names except for constructors (classes) should be lowercase, constructors are uppercase. *Examples*: `fooFunc`, `connectToWebservice`, `doCoolComputation`, `myVariable`, `vector1`, `MyClass`, `Circle`, `FileCommunicator`. Names in general should be self-explanatory and human-readable, avoid abbreviations and name collisions. (There is no significant speed benefit on shorter names, as all names are identified through some sort of hash)

Special naming conventions are used for functions that provide similar functionality (there is, of course, no function overloading in SOF): functions of this sort should be of the form `fname'args:variant`. `fname` is the general name of the function collection. `args` is either the number of arguments or the argument's types, separated by additional `'`. `variant` is either the variation on the base functionality or the return type.

*Examples*: The functions `random:01`, `random:int` and `random:float` all provide randomness, but `:01` returns values between 0 and 1, `:int` returns integers in a range and `:float` returns floats in a range. Similarly, the function collection `convert:<type>` includes a lot of functions that convert to a specific type from other supported types. The `writef'<argc>` functions all provide formatted standard output writing, but with a different number of formatting arguments specified by the `argc`.

### Cross-module naming

Names in modules can use an underscore to separate pseudo-namespaces (identical to the module name) from actual names. If the function names are reasonably generic and the number of exports in a module is small, this can be omitted.

For example, all the list functions in standard library `list` have a `list_` prefix, such as `list_elem`
