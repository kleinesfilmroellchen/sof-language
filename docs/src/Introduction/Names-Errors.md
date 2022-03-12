# Names and Errors

Let's get into more advanced topics. Variables, branching and of course the programmer's favorite: Errors.

## Naming things

Right now, we are only using the stack for storing things. We can duplicate, remove and operate on these values, but if we need lower values that were put on the stack previously, we are going to run into some issues quickly. For this reason, we can use the mighty `def` operator:

```sof
>>> 3 x def
>>> # do some stuff here
>>> x . writeln
3
```

`def` is short for "define". In this case it defines that the name `x` should represent the number value `3`. Nothing fancy happens, but in the background, SOF created what is essentially a variable of name "x" and given it the value of the number 3. We can now go off and do something else (also note the use of basic `# comments`) and come back later to retrieve x's value. This is done by giving the "variable"'s name followed by a `.` . That little dot, the Call operator, is incredibly powerful (and powers, like, everything in SOF at all), but let's not get ahead of ourselves. Here, it is just used to retrieve the value associated with a name. Also, from now on, we will use the proper SOF terminology and call this simple `x` an Identifier. It doesn't do anything on its own, it is just a piece of data that can be used to identify (hence the name) variables and other named things.

## Making decisions

You are probably already waiting for conditional execution and all that turing-complete stuff. Here it is:

```sof
>>> { "That number is small" writeln } 4 1000 < { "That number is large" writeln } ifelse         
That number is small
>>> { "That number is small" writeln } 2000 1000 < { "That number is large" writeln } ifelse 
That number is large
```

There are a bunch of things here that need discussion. The braces delimit code blocks, which are a way of grouping instructions (in this case, two simple output writes) to be executed later. More on them in a bit. The `<` is a basic comparison operator, a less than operator that needs two numbers and returns a boolean (true/false) according to the comparison result. Finally, the `ifelse` is an operation that takes in two executable things, in this case the code blocks, and executes them based on the boolean result that lies in between them: if it is true (if the number is smaller than 1000), it executes the first block, if it is false (greater 1000), it executes the second block. A simple if would omit the alternative block and just execute the first block if the condition was true:

```sof
>>> { "Primary school completed!" writeln } 2 1 > if
Primary school completed!
>>> { "Primary school completed!" writeln } 2 4 > if
>>>
```

## Errors

Up until now, we have only written simple programs that do not crash, because they conform to SOF's syntax and other rules. But let's say you screwed up while typing a string and forgot the closing quote:

```sof
>>> "a string
... 
!!! Syntax Error in line 1 at index 1:
 "a string
  ^
    No closing '"' for string literal.
```

The first thing that will happen when you try this is that a line with three dots will appear. This is because SOF found the error, but many errors can be corrected on the next line, so it gives you another chance. This error, however, is not resolvable: Ending the continuation line with another press of the enter key will make SOF scream at you. But in a good way.

First, there is the !!! bit, which signals an error. Then, the name of the Error is given. The [Errors section](Reference#errors) in the reference has information on all errors, but the most common ones are Syntax, your code is sh\*t, and Type, your data is sh\*t. After the error type comes the information on where the error occurred (possibly incorrect) as well as the segment of code where the error occurred combined with a pointer to the exact character (mostly correct, no guarantees). This helps you find the place of mishap. Finally, there is some additional information on what went wrong: in this case, no closing quote for the string literal was found, which is exactly the error. Note that as with every language, the false behavior might be somewhere else, but wasn't detected due to legal SOF behavior. For example, you might be passing a wrong parameter to a function, which will only be detected when some operation tries to act on that parameter and finds it to be of a wrong type.
