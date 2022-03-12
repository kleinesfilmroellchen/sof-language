# SOF Language Specification

This section specifies the Stack with Objects and Functions programming language.

## The SOF source file

An SOF source file is a program or part of a program in the SOF programming language. The source files shall end with `.sof` or `.stackof`; the latter mainly for extension collisions. Every SOF source file must adhere to the following Extended Backus-Naur form specification:

```ebnf
(* A program is a series of tokens and comments, where tokens MUST be separated by whitespace. *)
SofProgram = [Token] { [Comments] ?Whitespace? [Comments] Token } { [Comments] ?Whitespace? } ;

Token = "def" | "globaldef" | "dexport" | "use" | "export"
      | "dup" | "pop" | "swap"
      | "write" | "writeln" | "input" | "inputln"
      | "if" | "ifelse" | "while" | "dowhile" | "switch"
      | "function" | "constructor"
      | "+" | "-" | "*" | "/" | "%" | "<<" | ">>" | "cat"
      | "and" | "or" | "xor" | "not"
      | "<" | "<=" | ">" | ">=" | "=" | "/="
      | "." | ":" | "," | ";" | "nativecall"
      | "[" | "]" | "|"
      | "describe" | "describes" | "assert"
      | Number | String | Boolean
      | Identifier | CodeBlock ;

Identifier = ?Unicode Letter? { ?Unicode Letter? | DecimalDigits | "_" | "'" | ":" } ;
(* A code block recursively contains SOF code, i.e. an SofProgram. *)
CodeBlock = "{" SofProgram "}" ;

(* Literals *)
String = '"' { ?any character except "? '\"' } '"' ;
Boolean = "true" | "false" | "True" | "False" ;
Number = [ "+" | "-" ] ( Integer | Decimal ) ;
Integer = "0" ( "h" | "x" ) HexDigits { HexDigits }
        | [ "0d" ] DecimalDigits { DecimalDigits }
	| "0o" OctalDigits { DecimalDigits }
	| "0b" BinaryDigits { BinaryDigits } ;
Decimal = DecimalDigits { DecimalDigits } "." DecimalDigits { DecimalDigits }
          [ ("e" | "E") ( "+" | "-" ) DecimalDigits { DecimalDigits } ] ;
BinaryDigits = "0" | "1" ;
OctalDigits = BinaryDigits | "2" | "3" | "4" | "5" | "6" | "7" ;
DecimalDigits = OctalDigits | "8" | "9" ;
HexDigits = DecimalDigits | "a" | "b" | "c" | "d" | "e" | "f" | "A" | "B" | "C" | "D" | "E" | "F" ;

(* Comments are ignored. *)
Comments = Comment { Comment } ;
Comment = ( "#" { ?any? } ?Unicode line break/newline? )
        | ( "#*" { ?any? } "*#" ) ;
```

Here, `SofProgram` is the syntax specification for an entire SOF source file. The source file consists of two types of syntactical constructs: *Comments* and *Tokens*.

Comments are purely for the benefit of the programmer and are entirely ignored. Tooling is encouraged to place programming-language-like information about the code inside comments. There are two types of comments: Single-line comments start with `#` and extend to the end of the line, while multi-line comments start with `#*` and end with `*#` and can span multiple lines. In the following and the language reference in general, comments are treated as non-existent, and it is recommended to remove comments from the source code as one of the first steps of code processing.

Tokens are the core of the SOF program. The tokens are ordered in a linear sequence. The only exception is the code block token: A code block recursively nests another sequence of tokens. The major other differentiation in the token type is between the *Literal Tokens* that behave and look like the literals in other programming languages, as well as the *Primitive Tokens* aka. keywords that execute program logic.

## Executing

Executing an SOF program is as simple as executing all tokens in the order that they are given in the source code. What each token does can vary wildly and each token is specified precisely in the main language reference. In general, executed tokens manipulate the *Stack* and the *Nametables*, and while Primitive Tokens have a predefined constant action, Literals vary depending on the exact token text given.

### The SOF program environment

Every SOF program has an environment in which it is executed. This mainly consists of the Stack and the Nametable (or Nametables). The stack is a LIFO stack/queue (last in, first out) that can contain any kind of value. Of these values, there are types that the user can place and read via the use of certain tokens, and there are the "hidden types" that are used to make the program execute correctly. The form of these types is less precisely defined because they heavily depend on the implementation. The following hidden types are used:

- Global nametable: Sits on the bottom of the stack and acts as the global nametable into which bindings and lookups are done if no other nametable is present; or if the `globaldef` or similar PTs are executed, which explicitly always operate on the global nametable.
- Function environment with local nametable: This is a nametable that hides the nametables below it on the stack when normal bindings and lookups are done. This nametable is placed on the stack when a function enters and can also serve as an indicator for the implementation on where to return execution to; similar to return addresses on assembly-level stacks. This nametable also holds a return value that is set by some tokens and returned to the caller (on the stack) when exiting the function. There are minor variations on the local nametables, for example for object constructors, but these behave almost the same.

#### The literal tokens

The literal tokens are all used to specify a value of a built-in type literally. They most commonly come in the form of basic data types like numbers and literal strings, but technically, code blocks are also literals. When a literal token is encountered, its value is placed on the stack, and the value of the token is derived from its physical form. This is self-explanatory for the literals of type Integer, Float, String (escape processing is explained elsewhere), and Boolean.

For code blocks, the code block's contained tokens must be stored in the data type in some form such that the contained tokens, and even additional code blocks, may be fully reconstructed by the implementation when the code block is later needed. This internal representation is deliberately kept unspecified so that implementations can choose any representation (or even multiple) that is most efficient in their circumstance. The code block, despite its appearance, therefore also just puts a data object on the stack that can later be executed or transformed as specified by the code block semantics. Most of these are given with the PTs that manipulate code blocks and can be found in the language reference. Because code blocks are considered to be immutable, implementations can take appropriate data-sharing measures to reduce these large on-stack data structures in size.

### Exiting a program

The SOF program exits when the last token is executed or an error occurs. The kinds of errors and when they are thrown are specified in the language reference. The implementation must guarantee that no memory leak or similar resource issue results from a normal or abnormal termination.
