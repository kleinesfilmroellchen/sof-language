package klfr.sof.lang.primitive;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import klfr.sof.lang.Stackable;

/**
 * SOF's standard list datatype. This is SOF's most basic and primary
 * composition datatype with first-class support.
 * 
 * @author klfr
 */
public class ListPrimitive extends Primitive implements Collection<Stackable> {

	private static final long serialVersionUID = 1L;

	/**
	 * The internal list that stores the data. In general, an array list is used,
	 * but this variable is kept abstract to enable future modification.
	 */
	private List<Stackable> list;

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

	////////////////////////////////////////////////////////////////////////////////////////////////
	//#region DELEGATED METHODS

	public void forEach(Consumer<? super Stackable> action) {
		list.forEach(action);
	}

	public boolean isEmpty() {
		return list.isEmpty();
	}

	public boolean contains(Object o) {
		return list.contains(o);
	}

	public Iterator<Stackable> iterator() {
		return list.iterator();
	}

	public boolean add(Stackable e) {
		return list.add(e);
	}

	public boolean remove(Object o) {
		return list.remove(o);
	}

	public boolean containsAll(Collection<?> c) {
		return list.containsAll(c);
	}

	public boolean addAll(Collection<? extends Stackable> c) {
		return list.addAll(c);
	}

	public boolean addAll(int index, Collection<? extends Stackable> c) {
		return list.addAll(index, c);
	}

	public boolean removeAll(Collection<?> c) {
		return list.removeAll(c);
	}

	public boolean retainAll(Collection<?> c) {
		return list.retainAll(c);
	}

	public void replaceAll(UnaryOperator<Stackable> operator) {
		list.replaceAll(operator);
	}

	public void clear() {
		list.clear();
	}

	public Stackable get(int index) {
		return list.get(index);
	}

	public int indexOf(Object o) {
		return list.indexOf(o);
	}

	public Stream<Stackable> stream() {
		return list.stream();
	}

	public Object[] toArray() {
		return list.toArray();
	}

	public <T> T[] toArray(T[] a) {
		return list.toArray(a);
	}

	public int size() {
		return list.size();
	}

	public Stackable remove(int index) {
		return list.remove(index);
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
