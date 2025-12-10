# SOF Language Specification

r[intro]
This section specifies the Stack with Objects and Functions programming language.

> [!WARNING]
> The reference is in the process of being rewritten in a combined axiomatic + operational semantics (opsem) style to accurately specify all intended behavior of SOF. This rewrite is not yet complete; this is roughly the current state:
>
> - [x] Introduction, lexing and parsing, baseline definitions, high-level opsem – done
> - [x] Errors – done
> - [x] Types – largely complete, may need some more cross-references and elaboration
> - [ ] Naming – missing; some behavior specified elsewhere
> - [ ] Primitive tokens – largely still in old style which is missing opsem descriptions
>   - [ ] Arithmetic
>   - [ ] Control Flow
>   - [ ] Stack & Naming
>   - [ ] Functional
>   - [ ] Typing
>   - [x] Modules – complete
> - [ ] Builtin functions – still in old style which is missing opsem descriptions
> - [x] Module system – complete
> - [ ] Language internals – will be merged into other sections, especially naming
> - [x] Glossary – needs minor fixes

r[definition]

## Definitions

r[definition.rfc2119]
The key words "MUST", "MUST NOT", "REQUIRED", "SHALL", "SHALL NOT",
"SHOULD", "SHOULD NOT", "RECOMMENDED", "MAY", and "OPTIONAL" in this
document are to be interpreted as described in [RFC2119](https://www.rfc-editor.org/rfc/rfc2119.html).

Most terms are defined as they are introduced.

r[definition.interpreter]
An SOF **interpreter** is any program that takes SOF source files as input and executes them according to this language specification, producing identical semantics to any other interpreter. Alternatively, an interpreter MAY also produce another program in source code or machine executable form which produces identical semantics to the SOF source file inputs; what is more commonly called a compiler. This document describes a set of behaviors that are allowed and MUST be followed in an ideal interpreter. In practice, some programs that deviate slightly (most commonly due to bugs) from this specification are also called interpreters.

r[definition.tool]
An SOF **tool** is any other program that deals with SOF source files. Their behaviors are not prescribed in this specification, and they MAY intentionally go beyond or against the specification to serve a certain purpose. For instance, tools may decide to introspect source code comments for a variety of purposes, while comments must be ignored by interpreters.

r[source-file]

## Source Files

An SOF source file is a program or part of a program in the SOF programming language. Source files are plain text files using UTF-8 [^unicode] character encoding. Newline sequences consist of an optional carriage return followed by a line feed.

r[source-file.extension]
Source files use the standard extension `.sof`.

r[source-file.syntax]
Source files adhere to the following Extended Backus-Naur form specification:

```ebnf
(* A program is a series of tokens and comments, where tokens MUST be separated by whitespace. *)
SofProgram = [Token] { [Comments] ?Whitespace? [Comments] Token } { [Comments] ?Whitespace? } ;

Token = "def" | "globaldef" | "dexport" | "use" | "export"
      | "dup" | "pop" | "swap" | "rot" | "over"
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

r[source-file.syntax.error]
Any violation of this syntax by a program MUST raise a [SyntaxError](Errors.md) when given as input to an interpreter.

`SofProgram` is the syntax specification for an entire SOF source file. The source file consists of two types of syntactical constructs: _Comments_ and _Tokens_.

r[source-file.comments]
Comments are purely for the benefit of the programmer and MUST NOT have meaning to an SOF interpreter.

> [!NOTE]
> Tokens are the core of the SOF program. The tokens are ordered in a linear sequence. The only exception is the code block token: A code block recursively nests another sequence of tokens. The major other differentiation in the token type is between the **Literal Tokens** that behave and look like the literals in other programming languages, as well as the **Primitive Tokens** aka. keywords that execute program logic. The phrase **Primitive Token** is used to distinguish atomic keywords from the non-atomic `{` and `}` keywords.

r[state]

## Program state

For semantic purposes, the program state of a running SOF program consists mainly of two things:

r[state.stack]

- A stack of values visible to the program. The stack is a LIFO stack/queue (last in, first out) that can contain any kind of value. Of these values, there are types that the user can place and read via the use of certain tokens, and there are the "hidden types" that are used to make the program execute correctly. Hidden types are specified less precisely, and the user is not generally allowed to interact with them.

r[state.call-stack]

- A stack of token lists that are being executed, with associated information about where these lists come from and what should happen after they finish executing.

r[execution]

## Executing

r[execution.opsem]
An SOF program consists of a list of tokens. Executing the program consists of running the action of each token in the order it appears in the list of tokens.

> [!NOTE]
> This reference specifies the behavior of each token in the form of [*operational semantics*](https://en.wikipedia.org/wiki/Operational_semantics) (opsem), that is: the steps which need to be taken to execute a token correctly.

Each token may have an effect on the SOF program environment, modifying it to a new state. Tokens may change the execution state of the program, by modifying the token list stack. For instance:

- add a token list to be executed next
- stop executing the current token list or any others that are being executed
- modify the current token list

r[execution.errors]
Tokens may also produce errors, which can then cause further changes to the SOF program environment.

r[execution.exit]
The SOF program exits when:

- the last token is executed, and the token list stack is empty.
- an uncaught error occurs.
- the program is aborted by any other, lower-level means, such as a system call requesting process termination (`exit()`)

r[defined-behavior]

## Defined Behavior

r[defined-behavior.intro]
By default, any behavior in SOF is considered **defined**. This means that the interpreter MUST NOT deviate from the behavior prescribed in the specification. Some parts of SOF execution are **unspecified** and leave multiple implementation avenues for interpreters. In these cases, any behavior the interpreter chooses to exhibit MUST be limited to this unspecified part. In particular, any behavior exhibited in unspecified sections MUST NOT propagate to the remainder of the program execution. This means that any further execution MUST (return to and) follow defined SOF execution behavior.

r[defined-behavior.nativecall]
The exception to defined behavior is formed by native functions, invoked through the `nativecall` operator. As specified, native functions may be supplied by the user and are therefore not part of the interpreter and its guarantees of SOF semantics. The specification for `nativecall` defines what set of behaviors are allowed for native functions. By virtue of being defined in another programming language, possibly with vastly more flexible behavior than SOF and complex semantics, possibly with far-reaching access to the SOF execution state, the interpreter cannot be expected to verify the correct behavior of all native functions, including catching any incorrect behavior as soon as it happens.

Therefore, if a native function does not uphold the allowed set of behaviors for native functions as laid out in the specification for `nativecall`, as soon as this function is invoked through use of `nativecall`, from this point onwards, the interpreter MAY exhibit any possible behavior, including any behavior normally forbidden by this specification. The interpreter MAY never return to defined behavior, as opposed to situations where behavior is locally unspecified. This is called **Undefined Behavior**. Undefined Behavior MUST NOT result from any other action other than the one described.

<!-- #### The literal tokens

The literal tokens are all used to specify a value of a built-in type literally. They most commonly come in the form of basic data types like numbers and literal strings, but technically, code blocks are also literals. When a literal token is encountered, its value is placed on the stack, and the value of the token is derived from its physical form. This is self-explanatory for the literals of type Integer, Float, String (escape processing is explained elsewhere), and Boolean.

For code blocks, the code block's contained tokens must be stored in the data type in some form such that the contained tokens, and even additional code blocks, may be fully reconstructed by the implementation when the code block is later needed. This internal representation is deliberately kept unspecified so that implementations can choose any representation (or even multiple) that is most efficient in their circumstance. The code block, despite its appearance, therefore also just puts a data object on the stack that can later be executed or transformed as specified by the code block semantics. Most of these are given with the PTs that manipulate code blocks and can be found in the language reference. Because code blocks are considered to be immutable, implementations can take appropriate data-sharing measures to reduce these large on-stack data structures in size. -->

[^unicode]: RFC3629: _UTF-8, a transformation format of ISO 10646._ <https://www.rfc-editor.org/rfc/rfc3629.html>
