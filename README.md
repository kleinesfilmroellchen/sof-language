# SOF - Stack with Objects and Functions

An experimental fully stack-based reverse-polish-notation functional and object-oriented programming language concieved and implemented by kleinesfilmröllchen.

###### SOF is to be pronounced 'ess-oh-eff' in English or `/əs.ʊu.ˈəf/` in IPA. If you want to make me angry, you can also pronounce it 'sohf'.

**This is a Work In Progress (WIP) experimental programming language.** If you cause a nuclear war and the inevitable destruction of mankind by using this software, I am not to blame.

### Installation and CLI usage

This is an Eclipse project, so I recommend cloning it with git and importing it into your workspace (again, there are great explanations for both of these). After building normally, you should be able to execute the main CLI class with a normal launch configuration. Alternatively, use the command line

```
java -p bin -m sof/klfr.sof.cli.CLI [arguments]
```

assuming that you have java 12+ on your $PATH and you are inside the root folder of the project. Replace \[arguments\] with whatever arguments you want to give to SOF.

The command line tool currently supports the following arguments and options (taken from help output):

```
sof - Interpreter for Stack with Objects and
      Functions (SOF) Programming Language.
usage: sof [-hvdpP] [-c COMMAND]
           FILENAME [...FILENAMES]

positional arguments:
   filename  Path to a file to be read and
             executed. Can be a list of files that
             are executed in order.

options:
   --help, -h
             Display this help message and exit.
   --version, -v
             Display version information and exit.
   -d        Execute in debug mode. Read the manual
             for more information.
   -p        Run the preprocessor and exit.
   -P        Do not run the preprocessor.
   --command, -c COMMAND
             Execute COMMAND and exit.

When used without execution-starting arguments (-c
or filename), sof is started in interactive mode.

Quit the program with ^C.
```

A basic Visual Studio language support extension has been written, which can be found [on the marketplace](https://marketplace.visualstudio.com/items?itemName=kleinesfilmroellchen.sof-language-support) and [here on github](https://github.com/kleinesfilmroellchen/sof-syntaxhighlight).

## 1. What is SOF?

```sof
"Hello World!" writeln
```

SOF, the (ugly but useful & pronouncible) acronym for **S**tack with **O**bjects and **F**unctions, is a programming language first concieved in June of 2019 by kleinesfilmröllchen. It has exactly one goal, which is also its name:

> **Create a pure stack-based programming language with object-oriented and functional capabilities.**

This means that SOF has the following main features:

- The fundamental element of a program is the stack: All data is contained in the stack and the interpreter needs little to (if implemented a certain way) no hidden data in order to execute successfully.
- All code is sequential and every token you write out operates on the stack in some way, be it a literal, an arithmetic operation, a function call, a block of code or otherwise. This leads to SOF exclusively using reverse-polish notation, or postfix notation, where operations come after the operands. In essence, the entire programming language can be broken down into its few types of tokens and a description of what each of them does with the stack.
- SOF does not distinguish between data and code; code is first-class by design. Code can be grouped into manipulatable data with the code block construct `{ }` (cmp. PostScript). This is the only nesting/recursive token of the entire language, which powers turing-completeness (branching & conditional execution) as well as FP and OOP.

## 2. Some sample programs

To get a taste of SOF, here are several sample programs written in SOF. Note that not every program might work with the current state of the interpreter, as the interpreter is in early alpha development and not complete by any means.

```sof
# fibbonachi
{
	n def
	0 i def
	1 x def 1 y def 1 z def
	{
		z .
		x . y . + z def
		y . x def
		z . y def
		writeln
		i . 1 + i def
	} { i . n . < } while
} 1 function fib def

"enter a number: " write input convert:int : fib :

# alternative function definition: heavier stack use, runs faster

{
	1 x def 1 y def 1 z def # counter
	{
		y . dup x . + z def # yold
		z . y def x def
		dup writeln # counter
	} { 1 - 0 > } while
} 1 function fib def

```

```sof
# factorial

{
	dup dup # arg*3
	{ 1 return } 2 < if # arg*2
	1 - fact : * return
} 1 function fact def

"enter a number: " write input convert:int : fact :
```

```sof
# play a guessing game
15 number def
0 guess def
{
	input convert:int : guess def
	switch:: {
		"You are correct!" writeln
	} { number . guess . = }
	{
		"You are too low!" writeln
	} { number . guess . < }
	{
		"You are too high!" writeln
	} switch
} { guess . number . /= } while
```

```sof
# compute the sum of the first one hundred natural numbers, using a loop and a function

# loop
1 i def
0 result def
{
	result . i . + result def # this syntax might change
	i . 1 + i def
} { i . 100 <= } while

"The result is: " result . cat writeln

# function
{
	n def # arg1
	# gauss formula
	n . n . 1 + * 2 / return
} 1 function sum def

"The result is: " 1 sum : cat writeln 
```

## 3. Basic principles of SOF

An SOF program consist of a list of "tokens", input elements that can be one of three basic syntatic types:

1. Basic token: Tokens that don't contain spaces.
2. String literal token: Tokens that represent string literals; these start with a double quote " and end with another one.
3. Code block token: Tokens that represent executable blocks of code and contain other tokens; these start with a open curly bracket `{` and end with a close curly bracket `}`.

Tokens are separated by at least one arbitrary whitespace character, e.g. space, newline, tab. This means that SOF is extremely tolerant in terms of formatting, but even non-word tokens like arithmetic operators have to be separated visually.

Every token can be seen as an action that operates on the stack. Some basic tokens include:

- Literal token: This is a string (see above), an integer, a floating-point decimal number or a boolean. They place their own value onto the stack.
- Operator token: The basic arithmetic operations add (`+`), subtract (`-`), multiply (`\*`) and divide (`/`) (as well as some others), which will pop two values off the stack, compute a result and push that back onto the stack. It should be noted right now that all operators and functions consider the lower elements on the stack further to the left, as this is the most natural for programming. For example, the SOF code `3 4 /` is equivalent to the mathematical operation `3 / 4`, as the operator `/` treats the lower value (`3` in this case) as the left operand of the division, the dividend.
- Special primitive token (PT): These tokens take on special roles in the language and enable more advanced features. You can think of them as keywords (because they would be identifiers if they weren't treated specially). The most basic ones are `dup`, which duplicates the topmost value on the stack, `pop`, which discards one value of the stack, `def`, which defines a variable and `write` and `writeln`, which output text. The most basic flow-controlling special tokens are the well-known `if`, `while`, `elseif` etc.
- Call operator: The special token `.` calls functions and code blocks, retrieves variable values and creates objects. Quite special, that little guy! As it turns out, this operator is fairly simple in behavior, but at the same time can handle several apparently distinct language features. The Double Call operator `:` is just an abbreviation for two calls head-to-head, useful for invoking named functions.
- Identifier token: An identifier that will later be used to access some named code (a function), some named value (a variable) or some named constructor function (a class). Identifiers are normal data as they simply place themselves onto the stack like literals.

### For more information, [visit the wiki](https://github.com/kleinesfilmroellchen/sof-language/wiki).

## 4. The Simplicity of SOF - A Personal Note

SOF is a simple language. By simple, I do not mean easy to learn or understand, although I put some effort towards self-explanatory syntax and features. I mean:

- The way SOF works can be explained in very simple terms and does not contain many complicated algorithms and decision trees.
- SOF syntax is extremely basic (see 5. EBNF Syntax definition) and therefore easy to interpret and compile by many primitive tools.
- SOF is so basic in its I/O and internal operations that one can translate any SOF program into any bytecode utilizing nothing more than one temporary register containing a number and a pure stack of numbers. Although I have not yet written such a compiler for an elemental ultra-pure-stack-based language (like the pure lambda calculus in functional programming), a skilled reader should have no trouble of writing down the translation instructions and/or a program to do that for you.

SOF is a tool for understanding programming languages. It is very usable, the learning curve is not very steep regardless of whether you have programming experience or not, and it is turing-complete and universal. At the same time, it is so simple that I can write its interpreter or even a compiler without knowing how 'real' compilers work or how to write a 'real' compiler. Even very primitive environments should be able to parse and run SOF.

Just to be pretentious; Antoine de Saint-Exupéry is famously quoted on

> Perfection is achieved, not when there is nothing more to add, but when there is nothing left to take away.

I think that SOF is an excellent example of this. I cannot currently think of anything that you can take away from SOF without making it inferior in capability (or, at least, severely less usable, like removing the function PT would do, for example). It is very surprising indeed that such a simple thing as a reverse-polish notation language with three token types and literally less than 30 lines of syntax definition could be as feature-rich and capable as any other modern programming language, even if with odd syntax and inferior speed. (The latter is down to me being simply not capable of writing an efficient interpreter or any sort of compiler. PostScript and Forth prove that postfix languages can be extremely performant.)

## 5. EBNF Syntax definition

This formal syntax definition utilizes standard Extended Backus-Naur Form formal language grammar notation. An entire SOF program needs to conform to the syntax of `SofProgram`. Note that this syntax may be incorrect and will be rechecked with the language in the future.

Identifiers are quite restricted in the characters they can contain. The characters `'`, `:` and `_` are to be used for word separation or language conventions that denote return type, argument count etc. [see here](https://github.com/kleinesfilmroellchen/sof-language/wiki/Programming-conventions). However, all normal Unicode alphabetical characters are allowed, i.e. every character that is used in some language's normal text. SOF is therefore suited for international use by design. In contrast to identifiers, primitive tokens may contain any characters but are most often valid identifiers, as well.

```ebnf
SofProgram = { Token } ;
Token = "def" | "dup" | "pop" | "swap" | "write" | "writeln" | "input" | "inputln"
      | "if" | "else" | "elseif" | "while" | "switch"
      | "+" | "-" | "*" | "/" | "%" | "and" | "or" | "xor" | "not" | "." | ":"
      | "<" | "<=" | ">" | ">=" | "=" | "/="
      | "[" | "]"
      | Number | String | Boolean
      | Identifier | CodeBlock ;
Identifier = ? Unicode "Alphabetical" ? { ? Unicode "Alphabetical" ? | DecimalDigits | "_" | "'" | ":" } ;
CodeBlock = "{" { Token } "}" ;
(* Literals *)
String = '"' { ? any character ? '\"' } '"' ;
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
```

## The SOF interpreter

The main project at the current date is to complete the SOF interpreter, the program that executes SOF source code. At the current state it is a very bare-bones non-optimized command line program written in Java 12. The interpreter is extremely simple in that it does not do any optimization and just uses an AST as intermediate representation.

While in development, the special primitive tokens `describe` and `describes` can be used when starting the interpreter with the debug flag '-d' to describe the topmost stack element and the entire stack and global nametable, respectively.

Currently many fundamental features, including base features (variables, arithmetic, branching), functions and codeblocks, native calls implemented. However, I will continue to extend the interpreter's capabilities until it reaches the prospected language definition. This means that many times I will talk about and document the language's vision instead of its current state, so don't be confused.

The project is coming up on 70% code coverage through JUnit5 tests, and the long-term goal is as close to 100% as possible. As many of the main files, like the `Interpreter` and `CLI` classes, are hard to test, this will probably never be achieved but anywhere near that goal is good enough. I am very welcome to any test contributions!

Tests can be run with:

```
java -jar C:\Users\malub\Documents\java-libs\junit-platform-console-standalone-1.7.0-M1.jar --cp bin:testbins -o sof -p klfr.sof.test
```
