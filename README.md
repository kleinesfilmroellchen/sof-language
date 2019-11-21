# SOF - Stack with Objects and Functions

An experimental programming language concieved and implemented by kleinesfilmröllchen.

###### SOF is to be pronounced 'ess-oh-eff' in English or `/əs.ʊu.ˈəf/` in IPA. If you want to make me angry, you can also pronounce it 'sohf'.

This document will not explain the concept of a stack, please consult The Internet for information on stacks in computer science.

**This is a Work In Progress (WIP). Currently, the language is not turing-complete (lacks any method of control flow divergence, decision-making etc.), which is the main goal at the present time. Also, the program is extremely buggy and should not be used for anything serious.** If you cause a nuclear war and the inevitable destruction of mankind by using this software, I am not to blame.

### Installation and CLI usage

This is an Eclipse project, so I recommend cloning it with git and importing it into your workspace (again, there are great explanations for both of these). After building normally into the `/bin` folder, you should be able to execute the main CLI with the specified launch configuration. Alternatively, use the command line

```
"Path\To\java.exe" -Dfile.encoding=UTF-8 -p "Path\To\sof-language\bin" -classpath "USERDIR\.p2\pool\plugins\org.junit.jupiter.api_5.5.1.v20190826-0900.jar;USERDIR\.p2\pool\plugins\org.junit.jupiter.engine_5.5.1.v20190826-0900.jar;USERDIR\.p2\pool\plugins\org.junit.jupiter.migrationsupport_5.5.1.v20190826-0900.jar;USERDIR\.p2\pool\plugins\org.junit.jupiter.params_5.5.1.v20190826-0900.jar;USERDIR\.p2\pool\plugins\org.junit.platform.commons_1.5.1.v20190826-0900.jar;USERDIR\.p2\pool\plugins\org.junit.platform.engine_1.5.1.v20190826-0900.jar;USERDIR\.p2\pool\plugins\org.junit.platform.launcher_1.5.1.v20190826-0900.jar;USERDIR\.p2\pool\plugins\org.junit.platform.runner_1.5.1.v20190826-0900.jar;USERDIR\.p2\pool\plugins\org.junit.platform.suite.api_1.5.1.v20190826-0900.jar;USERDIR\.p2\pool\plugins\org.junit.vintage.engine_5.5.1.v20190826-0900.jar;USERDIR\.p2\pool\plugins\org.opentest4j_1.2.0.v20190826-0900.jar;USERDIR\.p2\pool\plugins\org.apiguardian_1.1.0.v20190826-0900.jar;USERDIR\.p2\pool\plugins\org.junit_4.12.0.v201504281640\junit.jar;USERDIR\.p2\pool\plugins\org.hamcrest.core_1.3.0.v20180420-1519.jar" -m sof/klfr.sof.cli.CLI -d
```

where you need to input your java directory (if the java bin folder is not on your PATH), the path to your compiled class files and your USERDIR which is where the JUnit5 jars should reside. If you are on Mac or Linux, replace the 'java.exe' with the appropriate binary's name.

Alternatively, do a JAR export based on the `sof.jardesc` JAR description file and the `manifest.mf` JAR manifest and run the jar with a simple `java -jar sof.jar`. This is the best solution as it makes for a very small archive without test files or the need to install JUnit5.

The command line tool currently supports the following arguments and options (taken from help output):

```
usage: sof [-h|-v]
       sof [-d] [-c command]
       sof [-d] filename [...filenames]
       sof [-d]

positional arguments:
   filename  Path to a file to be read and executed. Can
             be a list of files that are executed in order.

options:
   --help, -h
             Display this help message and exit.
   --version, -v
             Display version information and exit.
   -d        Execute in debug mode. Read the manual for
             more information.
   --command <command>, -c <command>
             Execute command and exit.
```

## 1. What is SOF?

```sof
"Hello World!" writeln
```

SOF, the (ugly but useful & pronouncible) acronym for **S**tack with **O**bjects and **F**unctions, is a programming language first concieved in June of 2019 by kleinesfilmröllchen. It has exactly one goal, which is also its name:

> **Create a pure stack-based programming language with object-oriented and functional capabilities.**

This means that SOF has the following main features:

- The fundamental element of a program is the stack: All data is contained in the stack and the stack combined with the current execution location in the source code gives full information about the state of the program and how to continue its execution.
- All code is sequential and every token you write out operates on the stack in some way, be it a literal, an arithmetic operation, a function call, a block of code or otherwise. In essence, the entire programming language can be broken down into its tokens and other units and a description of what each of them does with the stack.
- Creating blocks of SOF source code is the basic mechanism that enables loops, conditions, objects, functions and possibly meta-programming. These code blocks live on the stack together with any other data, which conveniently makes functions first-class objects.

## 2. Some sample programs

To get a taste of SOF, here are several sample programs written in SOF. Note that not every program might work with the current state of the interpreter, as the interpreter is in early alpha development and not complete by any means.

```
# play a guessing game
15 number def
0 guess def
{
	input int guess def
	{ {
		"You are correct!" writeln
	} number . guess . =
	{
		"You are too low!" writeln
	} ifelse } number . guess . <
	{
		"You are too high!" writeln
	} ifelse
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
} sum 1 function

"The result is: " 1 sum . cat writeln 
```

## 3. Basic principles of SOF

An SOF program consist of a list of "tokens", input elements that can be one of three basic syntatic types:

1. Basic token: Tokens that don't contain spaces.
2. String literal token: Tokens that represent string literals; these start with a double quote " and end with another one.
3. Code block token: Tokens that represent executable blocks of code and contain other tokens; these start with a open curly bracket { and end with a close curly bracket } .

Tokens are separated by at least one arbitrary whitespace character, e.g. space, newline, tab. This means that SOF is extremely tolerant in terms of formatting, as every token is self-contained.

Every token can be seen as an action that operates on the stack. Some basic tokens include:

- Literal token: This is a string (see above), an integer, a floating-point decimal number or a boolean. They place their own value onto the stack.
- Operator token: The basic arithmetic operations add (+), subtract (-), multiply (*) and divide (/) (as well as some others), which will pop two values off the stack, compute a result and push that back onto the stack. It should be noted right now that all operators and functions consider the lower elements on the stack further to the left, as this is the most natural for programming. For example, the SOF code `3 4 /` is equivalent to the mathematical operation `3 / 4`, as the operator '/' treats the lower value (`3` in this case) as the left operand of the division, the dividend.
- Special primitive token: These tokens take on special roles in the language and enable more advanced features. You can think of them as keywords (because they would be identifiers if they weren't treated specially). The most basic ones are `dup`, which duplicates the topmost value on the stack, `pop`, which discards one value of the stack, `def`, which defines a variable and `write` and `writeln`, which output text. The most basic flow-controlling special tokens are the well-known `if`, `while`, `elseif` etc.
- Call operator: The special token `.` calls functions and code blocks, retrieves variable values and creates objects. Quite special, that little guy! As it turns out, the working principle of this operator is somewhat complicated, but at the same time can handle several apparently distinct language features. Therefore, an entire sub-section is devoted to it.
- Identifier token: An identifier that will later be used to access some named code (a function), some named value (a variable) or some named constructor function (an object). Identifiers are normal data as they simply place themselves onto the stack like literals.

### The Call Operator '.'

TODO: this section is not written yet. Be ready for the mayhem!

## 4. The Simplicity of SOF - A Personal Note

SOF is a simple language. By simple, I do not mean easy to learn or understand, although I put some effort towards self-explanatory syntax and features. I mean:

- The way SOF works can be explained in very simple terms and does not contain many complicated algorithms and decision trees.
- SOF syntax is extremely basic (see 5. EBNF Syntax definition) and therefore easy to interpret and compile by many primitive tools.
- SOF is so basic in its I/O and internal operations that one can translate any SOF program into any bytecode utilizing nothing more than one temporary register containing a number and a pure stack of numbers. Although I have not yet written such a compiler for an elemental ultra-pure-stack-based language (like the pure lambda calculus in functional programming), a skilled reader should have no trouble of writing down the translation instructions and/or a program to do that for you.

SOF is a tool for understanding programming languages. It is very usable, the learning curve is not very steep regardless of whether you have programming experience or not, and it is turing-complete and universal. At the same time, it is so simple that I can write its interpreter or even a compiler without knowing how 'real' compilers work or how to write a 'real' compiler. Heck, I can even write a bytecompiler for an intermediate language!

By creating a programming language which takes one concept to the extreme and sticks with it by any means, even when introducing new concepts, I want to explore programming in such a 'focused' language, which is so different from the mainstream of OOP and even from the other outliers such as FP and logical programming.

I was surprised at how easy it was to create an immensly powerful programming language by just introducing some general features. For example, the code block construct powers all of OOP, FP, enables turing-completeness, makes SOF higher-order and makes not only functions, but code in general first-class objects.

## 5. EBNF Syntax definition

This formal syntax definition utilizes standard Extended Backus-Naur Form formal language grammar notation. An entire SOF program needs to conform to the syntax of `SofProgram`.

```ebnf

SofProgram = { Token } ;
Token = "def" | "dup" | "write" | "writeln" | "pop"
      | "if" | "else" | "elseif" | "while" | "for"
      | "+" | "-" | "*" | "/" | "%" | "."
      | "<" | "<=" | ">" | ">=" | "="
      | "[" | "]"
      | Number | String | Boolean
      | Identifier | CodeBlock ;
Identifier = ? any Unicode character with class "Alphabetical" ? { ? Unicode Alphabetical ? | DecimalDigits | "_" } ;
CodeBlock = "{" { Token } "}" ;

(* Literals *)
String = '"' { ? any character, including the terminal sequence '\"' ? } '"' ;
Boolean = "true" | "false" | "True" | "False" ;
Number = [ "+" | "-" ] ( Integer | Decimal ) ;
Integer = "0" ( "h" | "x" ) HexDigits { HexDigits }
        | [ "0d" ] DecimalDigits { DecimalDigits }
	| "0o" OctalDigits { DecimalDigits }
	| "0b" BinaryDigits { BinaryDigits } ;
Decimal = DecimalDigits { DecimalDigits } "." DecimalDigits { DecimalDigits }
          [ ("e" | "E") [ "+" | "-" ] DecimalDigits { DecimalDigits } ] ;

BinaryDigits = "0" | "1" ;
OctalDigits = BinaryDigits | "2" | "3" | "4" | "5" | "6" | "7" ;
DecimalDigits = OctalDigits | "8" | "9" ;
HexDigits = DecimalDigits | "a" | "b" | "c" | "d" | "e" | "f" | "A" | "B" | "C" | "D" | "E" | "F" ;

```

## The SOF interpreter

The main project at the current date is to complete the SOF interpreter, the program that executes SOF source code. At the current state it is a very bare-bones non-optimized command line program written in Java 10. The interpreter is extremely simple in that it does not do any optimization and never compiles code to any intermediate level before it is run.

While in development, the special primitive tokens `describe` and `describes` can be used when starting the interpreter with the debug flag '-d' to describe the topmost stack element and the entire stack and global nametable, respectively.

Currently there is only basic output, arithmetic, literals, variable definition and calling in the local scope implemented. However, I will continue to extend the interpreter's capabilities until it reaches the prospected language definition. This means that many times I will talk about and document the language's vision instead of its current state, so don't be confused.

The project is coming up on 50% code coverage through JUnit5 tests, and the long-term goal is as close to 100% as possible. As many of the main files, like the `Interpreter` and `CLI` classes, are hard to test, this will probably never be achieved but anywhere near that goal is good enough. I am very welcome to any test contributions!
