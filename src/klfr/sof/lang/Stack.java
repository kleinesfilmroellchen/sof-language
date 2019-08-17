package klfr.sof.lang;

import java.io.Serializable;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentLinkedDeque;
import klfr.sof.CompilationError;
import klfr.sof.Interpreter;

public class Stack extends ConcurrentLinkedDeque<Stackable> implements Serializable {

	private static final long serialVersionUID = 1L;

	private Interpreter parent;

	public Stack(Interpreter parent) {
		this.parent = parent;
	}

	@Override
	public Stackable getLast() throws CompilationError {
		try {
			Stackable elmt = super.getLast();
			return elmt;
		} catch (NoSuchElementException e) {
			throw parent.makeException("Stack", "Stack is empty.");
		}
	}

	@Override
	public Stackable peek() throws CompilationError {
		Stackable elmt = super.peek();
		if (elmt == null) throw parent.makeException("Stack", "Stack is empty.");
		return elmt;
	}

	@Override
	public Stackable pop() throws CompilationError {
		try {
			Stackable elmt = super.pop();
			if (elmt instanceof Nametable) {
				super.push(elmt);
				throw parent.makeException("Stack Access",
						"Manipulation of Nametables and Stack delimiters on the stack is not allowed.");
			}
			return elmt;
		} catch (NoSuchElementException e) {
			throw parent.makeException("Stack", "Stack is empty.");
		}
	}

	/**
	 * Returns the global nametable, which is always the lowest element of the
	 * stack.
	 * @throws RuntimeException if you managed to delete or replace the global
	 * nametable (ノಠ益ಠ)ノ彡 ┻━━┻
	 */
	public Nametable globalNametable() throws RuntimeException {
		try {
			Stackable lowest = getLast();
			return (Nametable) lowest;
		} catch (ClassCastException e) {
			throw new RuntimeException("Interpreter Exception: Global Nametable missing");
		} catch (CompilationError e) {
			throw new RuntimeException("Interpreter Exception: Global Nametable missing. ┻━━┻ ミ ヽ(ಠ益ಠ)ノ 彡  ┻━━┻");
		}
	}

	/**
	 * Returns the current top-most scope which is the destination for names that
	 * are defined in the file-global scope. This is the global nametable (as
	 * returned by {@code globalNametable()}) if there is no namespace introduced,
	 * or the namespace defined by the 'namespace' command (which resides on the
	 * stack just above the global nametable)
	 * @return
	 * @throws RuntimeException
	 */
	public Nametable namingScope() throws RuntimeException {
		Iterator<Stackable> helperIt = this.descendingIterator();
		helperIt.next();
		if (helperIt.hasNext()) {
			Stackable maybeNamespace = helperIt.next();
			// if it is a nametable but no local scope delimiter
			if (maybeNamespace instanceof Nametable && !(maybeNamespace instanceof ScopeDelimiter)) {
				//we found a namespace
				return (Nametable) maybeNamespace;
			}
		}
		//only end up here if no element above global nametable or no namespace above global nametable
		return globalNametable();
	}

	/**
	 * Returns the current local scope, i.e. the topmost nametable that is on the
	 * stack.
	 */
	public Nametable localScope() throws RuntimeException {
		Iterator<Stackable> elements = this.iterator();
		while (elements.hasNext()) {
			Stackable elmt = elements.next();
			if (elmt instanceof Nametable) return (Nametable) elmt;
		}
		//fallback (should not happen)
		return globalNametable();
	}

	/**
	 * Sets the namespace where file global name definitions are put into. Replaces
	 * an existing namespace, if necessary.<br>
	 * <strong>ATTENTION: Does not put the namespace into the global nametable for
	 * reference from other locations! You have to insert the namespace as an entry
	 * into the global nametable manually.</strong>
	 * @return this stack
	 */
	public Stack setNamespace(Nametable namespace) {
		Iterator<Stackable> helperIt = this.descendingIterator();
		helperIt.next();//skip global nametable
		Stackable maybeOldNamespace = helperIt.next();
		//take off global nametable temporarily
		Nametable globalNametable = (Nametable) this.removeLast();
		// if the second-to-last element is a nametable but no local scope delimiter, we found a namespace, replace it
		if (maybeOldNamespace instanceof Nametable && !(maybeOldNamespace instanceof ScopeDelimiter)) this.removeLast();
		this.addLast(namespace);
		this.addLast(globalNametable);

		return this;
	}

}
