package klfr.sof.lang;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

import klfr.sof.*;
import klfr.sof.lang.Stackable.DebugStringExtensiveness;

/**
 * The main data structure of SOF internally where all data resides. This is a
 * thin wrapper around {@code ConcurrentLinkedDeque} which is de-generified to
 * Stackables. The stack has the following special structure:
 * 
 * <pre>
 *   |-------------------|
 *   | (other elements)  |
 *   |-------------------|
 *   |       ...         |
 * ( |-------------------| )
 * ( | File namespace NT | )
 *   |-------------------|
 *   | Global Nametable  |
 *   |                   |
 * </pre>
 * 
 * This means that it is very easy to access the file's namespace Nametable and
 * the global Nametable.
 * 
 * @author klfr
 * @version 0.1a1
 */
public class Stack extends ConcurrentLinkedDeque<Stackable> {
	private static final long serialVersionUID = 1L;

	public Stack() {
	}

	@Override
	public Stackable getLast() throws CompilerException {
		try {
			final Stackable elmt = super.getLast();
			return elmt;
		} catch (final NoSuchElementException e) {
			throw new CompilerException.Incomplete("stack");
		}
	}

	@Override
	public Stackable peek() throws CompilerException {
		final Stackable elmt = super.peek();
		if (elmt == null)
			throw new CompilerException.Incomplete("stack");
		return elmt;
	}

	/**
	 * Pushes all values in the given collection to the stack, in the order that the
	 * iterator returns them.
	 * 
	 * @param args
	 */
	public Stack pushAll(Collection<Stackable> args) {
		for (var arg : args) {
			this.push(arg);
		}
		return this;
	}

	@Override
	public Stackable pop() throws CompilerException {
		try {
			final Stackable elmt = super.pop();
			if (elmt instanceof Nametable) {
				super.push(elmt);
				throw new CompilerException.Incomplete("stackaccess");
			}
			return elmt;
		} catch (final NoSuchElementException e) {
			throw new CompilerException.Incomplete("stack");
		}
	}

	/**
	 * Pop count elements from the stack and return them in the
	 * <strong>inverse</strong> order that they were popped.
	 * 
	 * @param count how many elements to pop.
	 * @return a list containing the elements popped, with the last popped element
	 *         first.
	 * @throws CompilerException
	 */
	public List<Stackable> pop(int count) throws CompilerException {
		// array for efficiency; it can be pre-allocated to specific size and then
		// inserted into at any location
		final var elts = new Stackable[count];
		for (int i = count; --i >= 0;) {
			elts[i] = this.pop();
		}
		return Arrays.asList(elts);
	}

	/**
	 * Pops a value with given type from the stack, or fails if the type does not
	 * match.
	 * 
	 * @param <T> The Stackable subtype expected.
	 * @param t   The class of the type, to allow for runtime type checking.
	 * @throws CompilerException if the popped element is not of type T, or if pop()
	 *                           itself failed.
	 */
	@SuppressWarnings("unchecked")
	public <T extends Stackable> T popTyped(final Class<T> t) throws CompilerException {
		final Stackable val = pop();
		if (t.isInstance(val)) {
			return (T) val;
		}
		super.push(val);
		throw new CompilerException.Incomplete("type", "type.checkfail", val,
				t.getAnnotation(StackableName.class).value());
	}

	/**
	 * Forces the stack to pop its topmost element, regardless of stack access
	 * restrictions. <strong>Users should use this function with great
	 * caution.</strong>
	 * 
	 * @return the popped value
	 */
	public Stackable forcePop() {
		return super.pop();
	}

	/**
	 * Returns the global nametable, which is always the lowest element of the
	 * stack.
	 * 
	 * @throws RuntimeException if you managed to delete or replace the global
	 *                          nametable (ノಠ益ಠ)ノ彡 ┻━━┻
	 */
	public Nametable globalNametable() throws RuntimeException {
		try {
			final Stackable lowest = getLast();
			return (Nametable) lowest;
		} catch (ClassCastException | CompilerException e) {
			throw new RuntimeException("Interpreter Exception: Global Nametable missing. ┻━━┻ ミ ヽ(ಠ益ಠ)ノ 彡  ┻━━┻");
		}
	}

	/**
	 * Returns the current top-most scope which is the destination for names that
	 * are defined in the file-global scope. This is the global nametable (as
	 * returned by {@code globalNametable()}) if there is no namespace introduced,
	 * or the namespace defined by the 'namespace' command (which resides on the
	 * stack just above the global nametable).<br>
	 * <br>
	 * {@code globaldef} always uses this as do {@code def}'s outside of functions.
	 * 
	 * @throws RuntimeException
	 */
	public Nametable namingScope() throws RuntimeException {
		final Iterator<Stackable> helperIt = this.descendingIterator();
		// skip global nametable
		helperIt.next();
		if (helperIt.hasNext()) {
			final Stackable maybeNamespace = helperIt.next();
			// if it is a nametable but no local scope delimiter
			if (maybeNamespace instanceof Nametable && !(maybeNamespace instanceof FunctionDelimiter)) {
				// we found a namespace
				return (Nametable) maybeNamespace;
			}
		}
		// only end up here if no element above global nametable or no namespace above
		// global nametable
		return globalNametable();
	}

	/**
	 * Returns the current local scope, i.e. the topmost nametable that is on the
	 * stack.
	 */
	public Nametable localScope() throws RuntimeException {
		for (final var elmt : this) {
			if (elmt instanceof Nametable)
				return (Nametable) elmt;
		}
		// fallback (should not happen, as the last iteration of the loop should find
		// the global NT)
		return namingScope();
	}

	/**
	 * Performs fallback-enabled lookup of the identifier. This means that when the
	 * identifier is not found in one nametable, the next lower one is searched and
	 * so on. May return null when the identifier is not found at all.
	 * 
	 * @param id the identifier to search for
	 * @return the most local value that is associated with the identifier
	 */
	public Stackable lookup(final Identifier id) {
		for (final var elmt : this) {
			if (elmt instanceof Nametable) {
				final var nt = (Nametable) elmt;
				if (nt.hasMapping(id))
					return nt.get(id);
			}
		}
		return null;
	}

	/**
	 * Sets the namespace where file global name definitions are put into. Replaces
	 * an existing namespace, if necessary.<br>
	 * <strong>ATTENTION: Does not put the namespace into the global nametable for
	 * reference from other locations! You have to insert the namespace as an entry
	 * into the global nametable manually.</strong>
	 * 
	 * @return this stack
	 */
	public Stack setNamespace(final Nametable namespace) {
		final Iterator<Stackable> helperIt = this.descendingIterator();
		helperIt.next();// skip global nametable
		final Stackable maybeOldNamespace = helperIt.next();
		// take off global nametable temporarily
		final Nametable globalNametable = (Nametable) this.removeLast();
		// if the second-to-last element is a nametable but no local scope delimiter, we
		// found a namespace, remove it
		if (maybeOldNamespace instanceof Nametable && !(maybeOldNamespace instanceof FunctionDelimiter))
			this.removeLast();
		// add new namespace and global nametable
		this.addLast(namespace);
		this.addLast(globalNametable);

		return this;
	}

	/**
	 * Traverses the stack and returns the first nametable that is found on it. All
	 * elements above and including the nametable are discarded. This method is
	 * intended to be used for destroying local scopes when exiting them. This
	 * method will not remove the global nametable and return Optional.empty()
	 * instead. It will, however, still remove all elements above it. Therefore,
	 * callers of this method should cautiously push nametables to the stack which
	 * are then to be found by this method.
	 * 
	 * @return The highest nametable that was found, or none if none was found.
	 */
	public Optional<Nametable> popFirstNametable() {
		try {
			var nt = this.forcePop();
			while (!(nt instanceof Nametable))
				nt = this.forcePop();
			// popped the gnt
			if (this.isEmpty()) {
				this.push(nt);
				return Optional.empty();
			}
			return Optional.of((Nametable) nt);
		} catch (NoSuchElementException e) {
			throw new CompilerException.Incomplete("stackaccess");
		}
	}

	/**
	 * toString method that creates a visual multiline representation of the stack
	 * and its contents.
	 */
	public String toStringExtended() {
		return "┌─" + Interpreter.line66.substring(0, 37) + "─┐" + System.lineSeparator()
				+ this.stream().collect(() -> new StringBuilder(),
						(str, elmt) -> str.append(String.format("│%38s │%n├─" + Interpreter.line66.substring(0, 37) + "─┤%n",
								elmt.toDebugString(DebugStringExtensiveness.Compact), " ")),
						(e1, e2) -> e1.append(e2)).toString();
	}

}
