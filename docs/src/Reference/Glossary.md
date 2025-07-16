# Glossary

- **PT**: Primitive token. A special token that has the syntax of an identifier (i.e. if it wasn't special, it would be treated as an identifier) but executes a special operation that (for the most part) cannot be accomplished by other means.
- **Nametable**: A key-value mapping structure (compare to Java's & JavaScript's `Map`, python's `dict`) that is the second most important data structure of SOF internally after the Stack. All Nametables live on the Stack.
- **GNT**: Global nametable. Lowest element on the stack, used for top-level lookups and `globaldef`. Exported functions keep the GNT at export time.
