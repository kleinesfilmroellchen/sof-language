# The Stack

You can't understand SOF without understanding the stack. On the flip side, once you do, SOF should feel much more intuitive.

## What is a stack?

This section will shortly explain the notion of a stack as used in computer science. Feel free to skip it if you know about stacks and the LIFO principle.

A stack, in computer science, is just like a stack in the real world. Imagine a stack of books:

```
----------
|  book  |
----------
 |  book  |
 ----------
|  book  |
----------
|  book  |
----------
```

These books are heavy - you cannot lift more than one at a time. And because they are placed on top of each other, you can only access the topmost one. For these reasons, you can only do one of two basic things: put another book onto the stack or remove one book from the stack, making the book below that the new topmost book. (Technically, you could also count looking at the topmost book as a basic operation). These operations are called **push** and **pop** (and peek), respectively.

The same thing goes for stacks in computer science, but now the books are stored in electronic memory and the books are data: in the case of SOF numbers, strings, commands, code etc. You will often see a stack being referred to as a LIFO queue, which stands for "last in - first out", i.e. the last element that went into the "queue" (on top of the stack) is the first element that will be retrieved back with a pop operation.

## Advanced stacking

Here are some cool things to do with a stack:

```sof
>>> "world" dup writeln writeln
world
world
```

The `dup` operator, short for duplicate, creates an identical copy of anything on the stack.

```sof
>>> 6 7 8 pop writeln
7
```

The `pop` operator does exactly what it says: it discards a value from the stack. In this case, this makes the 7 the topmost element of the stack, which is then printed.

```sof
>>> 4 5 swap writeln
4
```

The `swap` operator swaps the topmost two elements of the stack.

