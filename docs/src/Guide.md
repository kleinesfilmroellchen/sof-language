# SOF Language tutorial and guide

This section is a tutorial and a more user-focused guide of SOF's features. It's recommended that you have some experience programming in common paradigms.

## Basics

SOF is an interpreted language. Use the installation steps described in the [Readme](https://github.com/kleinesfilmroellchen/sof-language/blob/master/README.md) to install SOF and launch the REPL interpreter. Note that the results of programs you typed in might not be visible; they are probably placed on the stack. This is different to a lot of REPL interpreters (such as Python or JavaShell), which print the result of the last expression, whether you put in a `println` or not. I recommend typing along with this tutorial and modifying the examples in your own creative ways to learn more about things are done in SOF. All example code shows the input and output of the interpreter, where `>>>` is a user input line, `...` is a input continuation line and `!!!` is an error information starting line.

Let's get the [Hello World](https://en.wikipedia.org/wiki/%22Hello,_World!%22_program) out of the way:

```sof
>>> "Hello, world!" writeln
Hello, world!
```

This introduces both string literals (double quotes, escape sequences coming soon!) as well as the most basic I/O command: `writeln`, which takes a string and prints it to standard output, ending the line. Most languages call this command `println`, a leftover term from when such a command would actually instruct a printer to print the text on paper. But I'm getting off track here.

You may have noticed something weird here. Don't believe me? Let's try some arithmetic:

```sof
>>> 3 12 + writeln
15
>>> 26 18 * writeln
468
>>> 378 9 / writeln
42
>>> 1 2 + 3 + writeln
6
```

You can see that the first line clearly computes 3 + 12, but why is the operator (`+`, addition) after the numbers it operates on (`3` and `12`)? The reason is that the stack-based nature of SOF causes it to have postscript notation: all the operations come after the operands they operate on. Most famously, the document description language PostScript by Adobe uses this operation method, and - surprise, surprise - it works off a stack as well.

Each operation or function will take a different number of arguments that come before it. As we saw, `writeln` only takes one argument: the thing to be printed, while `+` takes two arguments: the two numbers to be added. The same goes for the other arithmetic operations, including `-` not shown here.

With this new knowledge, take a look at the last line. Where is the second operand to the second `+` instruction? The `3` is one of them, but the other seems to be missing. Except, of course, it doesn't.

Any operation that occurs in SOF ever only has the Stack to work with. This means that not only do literals place their values onto the stack, but operations place their result back onto the stack. When the interpreter sees the first `+`, it retrieves the top two elements on the stack, which in this case happen to be the numbers `1` and `2` put there by the literals, and computes their sum. The result, 3, is placed back onto the stack, which can be imagined as shortening the code to `3 3 +`. The second `+` doesn't even know where its numbers come from - they may be from a user input function, they may be literals, they may be the result of an operation or they may be a duplication of another value (which, in this case, is quite possible). As long as they are both numbers, the `+` sums them and places that value onto the stack, ready for use in the next computation (which happens to be `writeln`).
