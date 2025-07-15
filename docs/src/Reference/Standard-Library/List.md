# `list`

## `idx`: Index into a list

**Arguments** < index: Integer < list: List

**Return value** < element: the value at index `index` in the `list`

This function implements normal indexing into a list. Any element can be retrieved from a list by means of its index. Indices are zero-based as in most programming languages. This means that the first element in a list is referred to by an index of zero. Using a negative index will retrieve elements from the end of the list, i.e. an index of -1 refers to the last element of the list (disregarding its size), -2 to the second to last element and so on. This is very useful for list-size-independent indexing from the end.

An index that would reach past the limits of the list throws an `IndexError`. The `element` function will always throw for empty lists. The indexing and throwing behavior is inherited by all list functions that take indices, unless noted otherwise.

## `length`: Length of a list

**Arguments** < list: List

**Return value** < List length: Integer

Returns the number of elements in the list, always nonnegative. Returns zero for the empty list.

## `head`: First element of a list

**Arguments** < list: List

**Return value** < First element in the list: Any value

This function returns the first value of the list. It throws `IndexError` if the list is empty.

## `tail`: List tail

**Arguments** < list: List

**Return value** < The list's tail: List

Returns a new list that has the first element of the old list removed. Together with `head`, it can be used to split a list into its first element and its remainder.

Returns an empty list for the empty list.

## `reverse`: Reverse a list

**Arguments** < list: List

**Return value** < The reverse of the list: List

Returns a new list which has all the elements of the old list, but at inversed positions. For example, the last element is now the first and the second element is now the second-to-last.

Returns the empty list for the empty list.

## `split`: Split up a list

**Arguments** < index: Integer < list: List

**Return value** < A two-element list with the first and second portion of the list, in that order.

This function splits up a list into two halves. The first half contains all elements up to the index (including the element at the index), and the second half contains all elements after the index. The two halves are returned as a list containing two elements. It is a more efficient combination of the `take` and `after` functions.

## `take`: First n elements of a list

**Arguments** < n: Integer < list: List

**Return value** < List of length n: List

Returns a new list that contains the elements of the given list up to the given index, exclusive. For positive n, this always means that the length of the new list is equal to n. Returns the empty list for n=0, returns the entire list if n is greater or equal the list's length.

## `after`: Elements after an index

**Arguments** < n: Integer < list: List

**Return value** < List with elmts after n: List

Inverse of `take`; returns the elements that `take` dropped from the list. For positive n, this is equivalent to dropping the first n elements from the list, for negative n, it is equivalent to taking `|n|` elements from the end of the list (possibly the entire list if `|n| > length(list)`. For n=0, the entire list is returned. For n greater than list length, the empty list is returned.

## `first`: First element of the list

**Arguments** < list: List

**Return value** < element: Any value

Returns the first element in the list, equivalent to `list 1 take`. This is intended for use with tuple-like lists.

## `second`: Second element of a list

**Arguments** < list: List

**Return value** < second element: Any value

Returns the second element of the list, similar to `fst`, and equivalent to `list 2 take`.

## `filter`: Filter a list

**Arguments** < filter predicate: Callable < list: List

**Return value** < filtered list: List

This is the first of the higher-order list processing functions. `filter` takes a list and a callable. The callable is then provided with each element at a time in order (the element is placed on the stack and the callable is invoked, therefore at least both functions and codeblocks will work). For each element where the callable returns a truthy value, the element is retained in the resulting list. For each element where the callable returns a falsy value, the element is discarded. The result is a list where only the elements that the filtering function deemed necessary are retained.
