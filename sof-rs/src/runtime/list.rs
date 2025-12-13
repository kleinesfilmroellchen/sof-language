#![allow(clippy::unnecessary_wraps)] // false positive, the builtins need to have specific signatures

use std::mem::MaybeUninit;
use std::sync::LazyLock;

use ahash::HashMapExt;
use gc_arena::{Collect, Gc, Mutation};
use miette::SourceSpan;

use crate::error::Error;
use crate::identifier::Identifier;
use crate::runtime::stackable::{BuiltinMethod, BuiltinMethodRegistry, RegistryExt};
use crate::runtime::{Stack, Stackable};

/// Underlying list object. Always heap-allocated, as it is not Sized.
#[derive(Debug, PartialEq, Collect)]
#[collect(no_drop)]
pub struct List<'gc> {
	pub(crate) list: Box<[Stackable<'gc>]>,
}

impl<'gc> List<'gc> {
	pub fn new_from_ref(list: &[Stackable<'gc>], mc: &Mutation<'gc>) -> Gc<'gc, Self> {
		Self::new_from_box(list.into(), mc)
	}

	pub fn new_from_box(list: Box<[Stackable<'gc>]>, mc: &Mutation<'gc>) -> Gc<'gc, Self> {
		Gc::new(mc, Self { list })
	}

	/// Creates a copy of the list with a new element pushed.
	pub fn with_pushed(&self, new_element: Stackable<'gc>, mc: &Mutation<'gc>) -> Gc<'gc, Self> {
		let Some(new_size) = self.list.len().checked_add(1) else {
			panic!("maximum list size exceeded");
		};
		let mut new_storage = Box::new_uninit_slice(new_size);
		new_storage[.. self.list.len()].write_clone_of_slice(self.list.as_ref());
		new_storage[self.list.len()] = MaybeUninit::new(new_element);
		// SAFETY: [0..old_size] was written by write_clone_of_slice, and [old_size] (the new element) was written by
		// the assignment. Therefore, the entire storage is now initialized.
		unsafe { Gc::new(mc, Self { list: new_storage.assume_init() }) }
	}
}

type FuckedListFunction<'gc> = for<'a, 'b, 'c> fn(
	this: &'a List<'gc>,
	stack: &'b mut Stack<'gc>,
	_mc: &'c Mutation<'gc>,
	span: SourceSpan,
) -> Result<Option<Stackable<'gc>>, Error>;

fn unfuck_list_function(func: FuckedListFunction<'_>) -> BuiltinMethod<List<'_>> {
	// SAFETY: Early and late bound lifetimes do not matter for static functions. ABIs are compatible.
	unsafe { std::mem::transmute(func) }
}

impl List<'_> {
	pub fn get_builtin_method<'gc>(id: &Identifier, span: SourceSpan) -> Result<BuiltinMethod<List<'gc>>, Error> {
		static REGISTRY: LazyLock<BuiltinMethodRegistry<List<'_>>> = LazyLock::new(|| {
			let mut registry = BuiltinMethodRegistry::new();
			registry.insert(Identifier::new_static("idx"), unfuck_list_function(idx));
			registry.insert(Identifier::new_static("length"), unfuck_list_function(length));
			registry.insert(Identifier::new_static("head"), unfuck_list_function(head));
			registry.insert(Identifier::new_static("first"), unfuck_list_function(head));
			registry.insert(Identifier::new_static("second"), unfuck_list_function(second));
			registry.insert(Identifier::new_static("tail"), unfuck_list_function(tail));
			registry.insert(Identifier::new_static("take"), unfuck_list_function(take));
			registry.insert(Identifier::new_static("after"), unfuck_list_function(after));
			registry.insert(Identifier::new_static("reverse"), unfuck_list_function(reverse));
			registry.insert(Identifier::new_static("split"), unfuck_list_function(split));
			registry.insert(Identifier::new_static("push"), unfuck_list_function(push));
			registry
		});
		// SAFETY: Early and late bound lifetimes do not matter for static functions.
		unsafe { std::mem::transmute(REGISTRY.get_builtin_method(id, span)) }
	}
}

fn get_index(stack: &mut Stack<'_>, len: usize, span: SourceSpan) -> Result<usize, Error> {
	let index = stack.pop(span)?;
	let Stackable::Integer(mut index) = index else {
		return Err(Error::InvalidTypeNative { name: "list index".into(), value: index.to_string().into(), span });
	};
	#[allow(clippy::cast_possible_wrap)]
	if index < 0 {
		index += len as i64;
	}
	// still < 0 -> programming bug
	if index < 0 {
		return Err(Error::NegativeIndexOutOfBounds { index, len, span });
	}
	let index = index.try_into().map_err(|_| Error::Overflow { span, original: index.to_string().into() })?;
	if index >= len {
		return Err(Error::IndexOutOfBounds { index, len, span });
	}
	Ok(index)
}

fn idx<'gc>(
	this: &List<'gc>,
	stack: &mut Stack<'gc>,
	_mc: &Mutation<'gc>,
	span: SourceSpan,
) -> Result<Option<Stackable<'gc>>, Error> {
	let index = get_index(stack, this.list.len(), span)?;
	let value = this.list[index].clone();
	Ok(Some(value))
}

fn length<'gc>(
	this: &List<'gc>,
	_stack: &mut Stack<'gc>,
	_mc: &Mutation<'gc>,
	span: SourceSpan,
) -> Result<Option<Stackable<'gc>>, Error> {
	Ok(Some(Stackable::Integer(
		this.list
			.len()
			.try_into()
			.map_err(|_| Error::Overflow { span, original: this.list.len().to_string().into() })?,
	)))
}

fn tail<'gc>(
	this: &List<'gc>,
	_stack: &mut Stack<'gc>,
	_mc: &Mutation<'gc>,
	span: SourceSpan,
) -> Result<Option<Stackable<'gc>>, Error> {
	Ok(Some(this.list.last().cloned().ok_or_else(|| Error::IndexOutOfBounds { index: 0, len: 0, span })?))
}

fn head<'gc>(
	this: &List<'gc>,
	_stack: &mut Stack<'gc>,
	_mc: &Mutation<'gc>,
	span: SourceSpan,
) -> Result<Option<Stackable<'gc>>, Error> {
	Ok(Some(this.list.first().cloned().ok_or_else(|| Error::IndexOutOfBounds { index: 0, len: 0, span })?))
}

fn second<'gc>(
	this: &List<'gc>,
	_stack: &mut Stack<'gc>,
	_mc: &Mutation<'gc>,
	span: SourceSpan,
) -> Result<Option<Stackable<'gc>>, Error> {
	Ok(Some(this.list.get(1).cloned().ok_or_else(|| Error::IndexOutOfBounds { index: 0, len: 0, span })?))
}

fn take<'gc>(
	this: &List<'gc>,
	stack: &mut Stack<'gc>,
	mc: &Mutation<'gc>,
	span: SourceSpan,
) -> Result<Option<Stackable<'gc>>, Error> {
	let index = get_index(stack, this.list.len(), span)?;
	let prefix = List::new_from_ref(this.list[.. index].into(), mc);
	Ok(Some(Stackable::List(prefix)))
}

fn after<'gc>(
	this: &List<'gc>,
	stack: &mut Stack<'gc>,
	mc: &Mutation<'gc>,
	span: SourceSpan,
) -> Result<Option<Stackable<'gc>>, Error> {
	let index = get_index(stack, this.list.len(), span)?;
	let suffix = List::new_from_ref(this.list[index ..].into(), mc);
	Ok(Some(Stackable::List(suffix)))
}

fn reverse<'gc>(
	this: &List<'gc>,
	_stack: &mut Stack<'gc>,
	mc: &Mutation<'gc>,
	_span: SourceSpan,
) -> Result<Option<Stackable<'gc>>, Error> {
	let list_copy = this.list.iter().rev().cloned().collect();
	Ok(Some(Stackable::List(List::new_from_box(list_copy, mc))))
}

fn split<'gc>(
	this: &List<'gc>,
	stack: &mut Stack<'gc>,
	mc: &Mutation<'gc>,
	span: SourceSpan,
) -> Result<Option<Stackable<'gc>>, Error> {
	let index = get_index(stack, this.list.len(), span)?;
	let (first, second) = this.list.split_at(index);
	let first_list = List::new_from_ref(first.into(), mc);
	let second_list = List::new_from_ref(second.into(), mc);
	// FIXME: should perform in-place init
	Ok(Some(Stackable::List(List::new_from_box(
		Box::new([Stackable::List(first_list), Stackable::List(second_list)]),
		mc,
	))))
}

fn push<'gc>(
	this: &List<'gc>,
	stack: &mut Stack<'gc>,
	mc: &Mutation<'gc>,
	span: SourceSpan,
) -> Result<Option<Stackable<'gc>>, Error> {
	let list_copy = this.with_pushed(stack.pop(span)?, mc);
	Ok(Some(Stackable::List(list_copy)))
}
