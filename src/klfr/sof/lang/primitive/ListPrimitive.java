package klfr.sof.lang.primitive;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import klfr.sof.lang.Stackable;

/**
 * SOF's standard list datatype. This is SOF's most basic and primary composition datatype with first-class support.
 * 
 * @author klfr
 */
public final class ListPrimitive extends Primitive implements List<Stackable> {

	private static final long		serialVersionUID	= 1L;

	/**
	 * The internal list that stores the data. In general, an array list is used, but this variable is kept abstract to
	 * enable future modification.
	 */
	private final List<Stackable>	list;

	/**
	 * Create a pre-populated list.
	 * 
	 * @param list The elements with which to populate this list.
	 */
	public ListPrimitive(Collection<Stackable> list) {
		this.list = new ArrayList<>(list);
	}

	/**
	 * Create an empty list.
	 */
	public ListPrimitive() {
		this.list = new ArrayList<>();
	}

	@Override
	public boolean equals(Stackable other) {
		return other instanceof ListPrimitive ? this.list.equals(((ListPrimitive) other).list) : false;
	}

	@Override
	public Object v() {
		return this.list;
	}

	@Override
	public String print() {
		StringBuilder builder = new StringBuilder("[ ");
		builder.append(this.list.stream().map(element -> element.print()).collect(() -> new StringBuilder(), (sb, a) -> (sb.isEmpty() ? sb : sb.append(" ")).append(a), (a, b) -> (a.isEmpty() || b.isEmpty() ? a : a.append(" ")).append(b)));
		return builder.append(" ]").toString();
	}

	@Override
	public String toDebugString(DebugStringExtensiveness e) {
		return switch (e) {
		case Full -> "List:["
				+ this.list.stream().map(element -> element.print()).collect(() -> new StringBuilder(), (sb, a) -> (sb.isEmpty() ? sb : sb.append(", ")).append(a), (a, b) -> (a.isEmpty() || b.isEmpty() ? a : a.append(", ")).append(b)) + "]";
		case Compact -> "List(" + this.list.size() + ")";
		case Type -> "List";
		};
	}

	////////////////////////////////////////////////////////////////////////////////////////////////
	//#region DELEGATED METHODS

	@Override
	public void forEach(Consumer<? super Stackable> action) {
		list.forEach(action);
	}

	@Override
	public boolean isEmpty() {
		return list.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return list.contains(o);
	}

	@Override
	public Iterator<Stackable> iterator() {
		return list.iterator();
	}

	@Override
	public boolean add(Stackable e) {
		return list.add(e);
	}

	@Override
	public boolean remove(Object o) {
		return list.remove(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return list.containsAll(c);
	}

	@Override
	public boolean addAll(Collection<? extends Stackable> c) {
		return list.addAll(c);
	}

	@Override
	public boolean addAll(int index, Collection<? extends Stackable> c) {
		return list.addAll(index, c);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return list.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return list.retainAll(c);
	}

	@Override
	public void replaceAll(UnaryOperator<Stackable> operator) {
		list.replaceAll(operator);
	}

	@Override
	public void clear() {
		list.clear();
	}

	@Override
	public Stackable get(int index) {
		return list.get(index);
	}

	@Override
	public int indexOf(Object o) {
		return list.indexOf(o);
	}

	@Override
	public Stream<Stackable> stream() {
		return list.stream();
	}

	@Override
	public Object[] toArray() {
		return list.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return list.toArray(a);
	}

	@Override
	public int size() {
		return list.size();
	}

	@Override
	public Stackable remove(int index) {
		return list.remove(index);
	}

	@Override
	public Stackable set(int index, Stackable element) {
		return list.set(index, element);
	}

	@Override
	public void add(int index, Stackable element) {
		list.add(index, element);
	}

	@Override
	public int lastIndexOf(Object o) {
		return list.lastIndexOf(o);
	}

	@Override
	public ListIterator<Stackable> listIterator() {
		return list.listIterator();
	}

	@Override
	public ListIterator<Stackable> listIterator(int index) {
		return list.listIterator(index);
	}

	@Override
	public List<Stackable> subList(int fromIndex, int toIndex) {
		return list.subList(fromIndex, toIndex);
	}

	//#endregion Delegated Methods
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
