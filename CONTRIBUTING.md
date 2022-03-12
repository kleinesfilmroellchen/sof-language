# Contributing to the SOF language implementation.

There are three main ways of contributing:

1. Improve the interpreter by adding features of the language that are documented but not yet implemented. It is best to first contact @kleinesfilmroellchen so that no duplicate work is done.

2. Try out the interpreter, experiment with edge cases and file bug reports for bugs that occur. Please stick to the bug report template for such bug reports.

3. Suggest and discuss language changes and extensions in the Issues section.

4. Write tutorials and examples for SOF and submit them for inclusion in the documentation by making a pull request to the wiki.

Code should follow the general Java code conventions, but isn't as strictly regulated as elsewhere. More specifically:

- Tabs instead of spaces, everywhere.
- Readable names in CamelCase everywhere. The general rule "Classes are nouns, methods are verbs" applies.
- Use the provided formatter config to format all Java code. SOF code should follow the SOF code style conventions as can be found in the documentation.
- No code containing warnings, unless good reasons are given. This, most importantly, includes imports and wildcard imports, unsafe typecasting and unused variables.
- **Good documentation**. If a method, class, member, non-trivial variable is undocumented, it should not be part of a commit. File bug reports for any undocumented code you can find. Good documentation includes stating intent, noting edge cases, documenting parameters, throws, and return values.
