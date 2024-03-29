package klfr.sof.lang;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.Logger;

import klfr.sof.*;
import klfr.sof.exceptions.*;
import klfr.sof.lang.Stackable.DebugStringExtensiveness;

/**
 * The main data structure of SOF internally where all data resides. This is a thin wrapper around
 * {@code ConcurrentLinkedDeque} which is de-generified to Stackables. The stack has the following special structure:
 * 
 * <pre>
 *   |-------------------|
 *   |  other elements   |
 *   |-------------------|
 *   |       ...         |
 *   |-------------------|
 *   | Global Nametable  |
 *   |                   |
 * </pre>
 * 
 * This means that it is very easy to access the file's namespace Nametable and the global Nametable.
 * 
 * @author klfr
 * @version 0.1a1
 */
public final class Stack extends ConcurrentLinkedDeque<Stackable> {

	private static final long			serialVersionUID		= 1L;

	private static final Logger		log						= Logger.getLogger(Stack.class.getCanonicalName());

	/**
	 * The stack of global nametables that have been stored away, as there's currently a different "fake" name table active.
	 * The stack that contains the other nametables is just for convenience. Technically, insertion of the new nametables
	 * directly above the previous "global" nametables plus adjusted global nametable retrieval logic would suffice.
	 * However, this is easier to implement.
	 */
	private final Deque<Nametable>	globalNametableStack	= new LinkedBlockingDeque<>();

	/**
	 * The stack starts out empty. The user of the stack is responsible for adding the global nametable.
	 */
	public Stack() {
	}

	/**
	 * Safe version of the {@link ConcurrentLinkedDeque#getLast()} method. This variant will throw
	 * IncompleteCompilerExceptions on all errors.
	 * 
	 * @return The tail of the deque, i.e. the lowest element on the stack.
	 * @throws IncompleteCompilerException if there is no element on the stack.
	 */
	public final Stackable getLastSafe() throws IncompleteCompilerException {
		try {
			final Stackable elmt = super.getLast();
			return elmt;
		} catch (final NoSuchElementException e) {
			throw new IncompleteCompilerException("stack");
		}
	}

	/**
	 * Safe version of the {@link ConcurrentLinkedDeque#peek()} method. This variant will throw IncompleteCompilerExceptions
	 * on all errors.
	 * 
	 * @return The topmost element on the stack (which will not be removed).
	 * @throws IncompleteCompilerException if there is no element on the stack.
	 */
	public final Stackable peekSafe() throws IncompleteCompilerException {
		final Stackable elmt = super.peek();
		if (elmt == null)
			throw new IncompleteCompilerException("stack");
		return elmt;
	}

	/**
	 * Pushes all values in the given collection to the stack, in the order that the iterator returns them.
	 * 
	 * @param contentsToPush The collection whose contents to push to the stack.
	 * @return This stack.
	 */
	public final Stack pushAll(Collection<Stackable> contentsToPush) {
		for (var arg : contentsToPush) {
			this.push(arg);
		}
		return this;
	}

	/**
	 * Safe version of the {@link ConcurrentLinkedDeque#pop()} method. This variant will throw IncompleteCompilerExceptions
	 * on all errors.
	 * 
	 * @param ignoreTransparentData Whether to ignore transparent data. If this value is true, all {@link TransparentData}
	 *                                 on the stack is discarded and never returned by this function.
	 * @return The topmost non-transparent element on the stack, which is removed.
	 * @throws IncompleteCompilerException if there is no element on the stack. or if there was a stack access violation.
	 */
	public final Stackable popSafe(final boolean ignoreTransparentData) throws IncompleteCompilerException {
		try {
			final Stackable elmt = super.pop();
			if (elmt instanceof Nametable) {
				super.push(elmt);
				throw new IncompleteCompilerException("stackaccess");
			} else if ((elmt instanceof TransparentData) && ignoreTransparentData) {
				log.fine("skipping transparent data");
				return this.popSafe();
			}
			return elmt;
		} catch (final NoSuchElementException e) {
			throw new IncompleteCompilerException("stack");
		}
	}

	/**
	 * Safe version of the {@link ConcurrentLinkedDeque#pop()} method. This variant will throw IncompleteCompilerExceptions
	 * on all errors. This function discards all transparent data.
	 * 
	 * @return The topmost non-transparent element on the stack, which is removed.
	 * @throws IncompleteCompilerException if there is no element on the stack. or if there was a stack access violation.
	 */
	public final Stackable popSafe() throws IncompleteCompilerException {
		return popSafe(true);
	}

	/**
	 * Pop count elements from the stack and return them in the <strong>inverse</strong> order that they were popped.
	 * 
	 * @param count how many elements to pop.
	 * @return a list containing the elements popped, with the last popped element first.
	 * @throws IncompleteCompilerException If at least one pop operation fails.
	 */
	public final List<Stackable> popSafe(int count) throws IncompleteCompilerException {
		// array for efficiency; it can be pre-allocated to specific size and then
		// inserted into at any location
		final var elts = new Stackable[count];
		for (int i = count; --i >= 0;) {
			elts[i] = this.popSafe();
		}
		return Arrays.asList(elts);
	}

	/**
	 * Pops a value with given type from the stack, or fails if the type does not match.
	 * 
	 * @param <T> The Stackable subtype expected.
	 * @param t   The class of the type, to allow for runtime type checking.
	 * @throws IncompleteCompilerException if the popped element is not of type T, or if pop() itself failed.
	 * @return The popped element.
	 */
	@SuppressWarnings("unchecked")
	public final <T extends Stackable> T popTyped(final Class<T> t) throws IncompleteCompilerException {
		final Stackable val = popSafe();
		if (t.isInstance(val)) {
			return (T) val;
		}
		super.push(val);
		throw new IncompleteCompilerException("type", "type.checkfail", val, Optional.ofNullable(t.getAnnotation(StackableName.class)).orElse(new StackableName() {

			@Override
			public Class<? extends Annotation> annotationType() {
				return StackableName.class;
			}

			@Override
			public String value() {
				return "<Anonymous>";
			}
		}).value());
	}

	/**
	 * Forces the stack to pop its topmost element, regardless of stack access restrictions.<br/>
	 * <br/>
	 * <strong>Users should use this method with great caution. It is not advised to use this in any circumstance where the
	 * SOF programmer controls the stack. This means that this method is only to be used when the element to be removed is
	 * known with great certanity to not cause any issues once it is removed.</strong> For example, this may be used to
	 * remove a nametable from the stack that is no longer needed. But first, the user should check that the topmost element
	 * <em>is</em> the nametable before removing it with {@code forcePop}.
	 * 
	 * @return The popped value.
	 */
	public final Stackable forcePop() {
		return super.pop();
	}

	/**
	 * Returns the global nametable, which is always the lowest element of the stack.
	 * 
	 * @return The global nametable.
	 * @throws RuntimeException If you managed to delete or replace the global nametable (ノಠ益ಠ)ノ彡 ┻━━┻
	 */
	public final Nametable globalNametable() throws RuntimeException {
		try {
			final Stackable lowest = getLastSafe();
			return (Nametable) lowest;
		} catch (ClassCastException | IncompleteCompilerException e) {
			throw new RuntimeException("Interpreter Exception: Global Nametable missing. ┻━━┻ ミ ヽ(ಠ益ಠ)ノ 彡  ┻━━┻", e);
		}
	}

	/**
	 * Replaces the global nametable with the given temporary global nametable. The actual global nametable is stored in a
	 * stack of temporarily removed global nametables. This is to facilitate the module system with different global
	 * nametables for each module: This method is called before entering a module function.
	 * 
	 * @param newGlobalNametable The new global nametable that replaces the current global nametable for the time being.
	 * 
	 * @throws RuntimeException If you managed to delete or replace the global nametable (ノಠ益ಠ)ノ彡 ┻━━┻
	 */
	public final void pushGlobalNametable(final Nametable newGlobalNametable) throws RuntimeException {
		try {
			final var currentGlobalNametable = super.pollLast();
			globalNametableStack.push((Nametable) currentGlobalNametable);
			super.addLast(newGlobalNametable);
		} catch (ClassCastException | NullPointerException e) {
			throw new RuntimeException("Interpreter Exception: Global Nametable missing. ┻━━┻ ミ ヽ(ಠ益ಠ)ノ 彡  ┻━━┻", e);
		}
	}

	/**
	 * Removes and returns the current global nametable, and places the topmost nametable of the global nametable stack back
	 * into the global nametable position. This is to facilitate the module system with different global nametables: This
	 * method is called after exiting from a module function.
	 * 
	 * @return The current global nametable.
	 */
	public final Nametable popGlobalNametable() throws RuntimeException {
		try {
			final var currentGlobalNametable = super.pollLast();
			super.addLast(globalNametableStack.pop());
			return (Nametable) currentGlobalNametable;
		} catch (NullPointerException | ClassCastException e) {
			throw new RuntimeException("Interpreter Exception: Global Nametable missing. ┻━━┻ ミ ヽ(ಠ益ಠ)ノ 彡  ┻━━┻", e);
		}
	}

	/**
	 * Returns the current local scope, i.e. the topmost nametable that is on the stack.
	 * 
	 * @return The local, topmost nametable.
	 * @throws RuntimeException If the stack is empty or there are no nametables on the stack (an absolutely illegal state).
	 */
	public final Nametable localScope() throws RuntimeException {
		for (var elmt : this) {
			if (elmt instanceof Nametable localNt)
				return localNt;
		}
		// fallback (should not happen, as the last iteration of the loop should find
		// the global NT)
		return globalNametable();
	}

	/**
	 * Performs fallback-enabled lookup of the identifier. This means that when the identifier is not found in one
	 * nametable, the next lower one is searched and so on. May return null when the identifier is not found at all.
	 * 
	 * @param id The identifier to search for.
	 * @return The most local value that is associated with the identifier.
	 */
	public final Stackable lookup(final Identifier id) {
		for (var elmt : this) {
			if (elmt instanceof Nametable nt) {
				if (nt.hasMapping(id))
					return nt.get(id);
			}
		}
		return null;
	}

	/**
	 * Traverses the stack and returns the first nametable that is found on it. All elements above and including the
	 * nametable are discarded. <br/>
	 * <br/>
	 * This method is intended to be used for destroying local scopes when exiting them. This method will not remove the
	 * global nametable and return Optional.empty() instead. It will, however, still remove all elements above it.
	 * Therefore, callers of this method should cautiously push nametables to the stack which are then to be found by this
	 * method.
	 * 
	 * @return The highest nametable that was found, or none if none was found.
	 * @see Stack#forcePop()
	 */
	public final Optional<Nametable> popFirstNametable() {
		var nt = this.forcePop();
		while (!(nt instanceof Nametable))
			nt = this.forcePop();
		// popped the gnt
		if (this.isEmpty()) {
			this.push(nt);
			return Optional.empty();
		}
		return Optional.of((Nametable) nt);
	}

	/**
	 * ToString method that creates a visual multiline representation of the stack and its contents.
	 * 
	 * @return a visual multiline representation of the stack and its contents.
	 */
	public final String toStringExtended() {
		return "┌─" + Interpreter.line66.substring(0, 37) + "─┐" + System.lineSeparator() + this.stream()
				.collect(() -> new StringBuilder(), (str, elmt) -> str.append(String.format("│%38s │%n├─" + Interpreter.line66.substring(0, 37) + "─┤%n", elmt.toDebugString(DebugStringExtensiveness.Compact), " ")), (e1, e2) -> e1.append(e2))
				.toString();
	}

}

/*  
The SOF programming language interpreter.
Copyright (C) 2019-2020  kleinesfilmröllchen

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
