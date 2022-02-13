[![CodeQL](https://github.com/kleinesfilmroellchen/sof-language/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/kleinesfilmroellchen/sof-language/actions/workflows/codeql-analysis.yml) [![Gradle CI & Tests](https://github.com/kleinesfilmroellchen/sof-language/actions/workflows/gradle.yml/badge.svg)](https://github.com/kleinesfilmroellchen/sof-language/actions/workflows/gradle.yml)

# SOF - Stack with Objects and Functions

An experimental fully stack-based reverse-polish-notation functional and object-oriented programming language concieved and implemented by kleinesfilmröllchen.

###### SOF is to be pronounced 'ess-oh-eff' in English or `/əs.ʊu.ˈəf/` in IPA. If you want to make me angry, you can also pronounce it 'sohf'.

**This is an experimental programming language.** If you cause a nuclear war and the inevitable destruction of mankind by using this software, I am not to blame.

SOF is written in Java 16 (cutting edge!) and requires no libraries outside the standard library. It leverages the module system (you may use it in your project as well!) and uses JUnit Jupiter for testing (currently) about 60% of the codebase.

### Installation and CLI Usage

This is a Gradle 7 project using the Java Application plugin with the module system and JUnit Jupiter tests. The usual Gradle tasks for these situations exist and have not been renamed/added to. As a quick reference: Use `gradlew build` to run the full build including tests. Run `gradlew test` to run the tests. Use `gradlew javadoc` to build the javadoc. All building happens into the `build/` subfolders.

Use the following command line to run SOF. `gradlew run` works but is somewhat buggy.

```
java -p ./build/out/bin -m sof/klfr.sof.cli.CLI [arguments]
```

assuming that you have Java 16+ on your $PATH and you are inside the root folder of the project. Replace \[arguments\] with whatever arguments you want to give to SOF.

Alternatively, you can use the Gradle-built distributions in build/distributions/. This one comes with handy scripts for Windows and Linux that auto-detect your Java.

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
   -P        Do not run the preprocessor before
             executing the input file(s).
   --command, -c COMMAND
             Execute COMMAND and exit.
   --performance
             Run performance tests and show results

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

## 2. Some Sample Programs

To get a taste of SOF, here are several sample programs written in SOF. Also checkout the [examples folder](./examples).

```sof
# fibbonachi

{
	n def
	0 i def
	1 x def 1 y def 1 z def
	{
		z .
		x . y . + z def # z := x + y
		y . x def # x := y
		z . y def # y := z
		writeln # z old
		i . 1 + i def
	} { i . n . < } while
} 1 function fib def

"enter a number: " write input convert:int : fib :

# alternative function definition: heavier stack use, runs faster

{
	1 x def 0 y def # counters, z is only defined later
	{
		y . dup x . + z def # z = y + x,  y (old) on the stack
		z . y def x def # y = z, x = y (old)
		z . writeln # write z, counter on the stack
	} { 1 - dup 0 > } while # while counter > 0
} 1 function fib def

```

```sof
# factorial

{
	dup dup # n*3 on the stack
	{ 1 return } swap 2 < if # return 1 if n < 2, n*2 on the stack
	1 - fact : * return # return factorial of n-1 times n
} 1 function fact def

"enter a number: " write input convert:int : fact : writeln
```

```sof
# play a guessing game
random:int number def
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

100 N def

# loop
1 i def
0 result def
{
	result . i . + result def
	i . 1 + i def
} { i . N . <= } while

"The result (loop) is: " result . cat writeln

# function
{
	n def # arg1
	# gauss formula
	n . n . 1 + * 2 / return
} 1 function sum def

N . sum :
"The result (function) is: " swap cat writeln 
```

## 3. Basic Principles of SOF

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

### For more information, [take a look at the official documentation](https://github.com/kleinesfilmroellchen/sof-language/wiki).

## 4. The Minimalism of SOF - A Personal Note

SOF is a minimalist language.

- The way SOF works can be explained in very simple terms and does not contain many complicated algorithms and decision trees.
- SOF syntax is extremely simple (see [the syntax definition](https://github.com/kleinesfilmroellchen/sof-language/wiki/SOF-Language-Specification)) and therefore easy to interpret and compile by many primitive tools.
- SOF is so basic in its I/O and internal operations that one can translate any SOF program into any bytecode utilizing nothing more than a couple of temporary register containing a number and a pure stack of numbers. Although I have not yet written such a compiler for an elemental ultra-pure-stack-based language (like the pure lambda calculus in functional programming), a skilled reader should have no trouble of writing down the translation instructions and/or a program to do that for you.

SOF is a tool for understanding programming languages. It is very usable, the learning curve is not very steep regardless of whether you have programming experience or not, and it is turing-complete and universal. At the same time, it is so simple that I can write its interpreter or even a compiler without knowing how 'real' compilers work or how to write a 'real' compiler. Even very primitive environments should be able to parse and run SOF.

Just to be pretentious; Antoine de Saint-Exupéry is famously quoted on

> Perfection is achieved, not when there is nothing more to add, but when there is nothing left to take away.

I think that SOF is an excellent example of this. I cannot currently think of anything that you can take away from SOF without making it inferior in capability (or, at least, severely less usable, like removing the function PT would do, for example). It is very surprising indeed that such a simple thing as a reverse-polish notation language with three token types and literally less than 30 lines of syntax definition could be as feature-rich and capable as any other modern programming language, even if with odd syntax and inferior speed. (The latter is down to me being simply not capable of writing an efficient interpreter or any sort of compiler. PostScript and Forth prove that postfix languages can be extremely performant.)
