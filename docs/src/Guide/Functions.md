# It's a bird! It's a plane! No wait - It's a function!

Functions are essentially code blocks on steroids. They can't do anything more or less, they are just safer and more convenient. To create a function, this pattern is generally used:

```sof
# create the function
{ 3 + return } 1 function addThree globaldef
# and call it
1 addThree : writeln # 4
```

The important bit is the primitive token (keyword) `function`. It takes in a piece of executable SOF code, a "Callable", as well as an argument count, which is 1 in this case. The code for the function here is a simple code block, the only way you can specify a Callable literally. Code block data behaves like any other data, it lives on the stack and can be assigned a name. Its superpower is *delayed execution*, that is, the SOF instructions you place inside it are not executed immediately, they are stored, to be run later. In this case, unlike the `if` and `ifelse` commands you saw earlier, we don't even use the stored code directly, instead, we use it as the logic of a newly created function. But the function itself is also just a Callable, some data, now sitting on the stack. To use it repeatedly from anywhere, we must `def` it like any other variable and give it a name. We use the `globaldef` in this case, which will always `def` globally (duh) (Python programmers: compare this to the `global` keyword). This pattern is SOF convention and allows you to define functions globally even inside other functions and classes.

We can now call the function, like we called the variables beforehand. But behold! This time around, we don't simply retrieve the Identifier's "value" with `.`. That would be the function itself again, which we want to call! For this reason, the double-call operator `:` exists. It simply executes two calls, the first time retrieving the function, the second time executing it. It is both more performant and more compact than `. .`, but identical in function.

Now, what happens when we call the function? If you know any programming language, you know that functions (or methods, procedures, lambdas or whatever they're called) can recieve one or more arguments. SOF, of course, is no different. If you know hardware/assembly-level programming, this may sound familiar: Arguments to an SOF function are expected to be on the stack. The function definition specifies the number of arguments, and SOF places a "stack protector" under all of a function's arguments. This means that a function's body cannot change anything that is on the stack, except for its arguments, of course. Also, anything that is on the stack when the function exits will be deleted, up to this "stack protector". Think of the stack protector as a specially-marked return address in assembler. The function cannot mess with the caller's stack and cannot clobber it. You can absolutely be sure how many elements from the stack are consumed, according to the function's argument count.

The code block looks familiar, but it now uses the new-fangled operation `return`. This will break out of the function body instantly and return the topmost value on the stack from the function. Alternatively, you can use the `return:nothing` PT, which will end the function without returning anything. This is the default behavior when the function's end is reached without a `return`. The return value is placed onto the stack, obviously. In the example we use it as an argument to `writeln`.
