use std::sync::LazyLock;

use ahash::HashMapExt;
use gc_arena::{Collect, Gc, Mutation};
use miette::SourceSpan;
use smallvec::{SmallVec, smallvec};

use crate::error::Error;
use crate::identifier::Identifier;
use crate::runtime::stackable::{BuiltinMethod, BuiltinMethodRegistry, RegistryExt};
use crate::runtime::{Stack, Stackable};

pub type ListVec<'gc> = SmallVec<[Stackable<'gc>; 15]>;

#[derive(Debug, Default, Clone, PartialEq)]
pub struct List<'gc> {
	pub(crate) list: ListVec<'gc>,
}

unsafe impl Collect for List<'_> {
	fn needs_trace() -> bool
	where
		Self: Sized,
	{
		true
	}

	fn trace(&self, cc: &gc_arena::Collection) {
		for el in &self.list {
			el.trace(cc);
		}
	}
}

impl<'gc> List<'gc> {
	pub fn new(list: ListVec<'gc>, mc: &Mutation<'gc>) -> Gc<'gc, Self> {
		Gc::new(mc, Self { list })
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
			registry.insert(Identifier::new("idx"), unfuck_list_function(idx));
			registry.insert(Identifier::new("length"), unfuck_list_function(length));
			registry.insert(Identifier::new("head"), unfuck_list_function(head));
			registry.insert(Identifier::new("first"), unfuck_list_function(head));
			registry.insert(Identifier::new("second"), unfuck_list_function(second));
			registry.insert(Identifier::new("tail"), unfuck_list_function(tail));
			registry.insert(Identifier::new("take"), unfuck_list_function(take));
			registry.insert(Identifier::new("after"), unfuck_list_function(after));
			registry.insert(Identifier::new("reverse"), unfuck_list_function(reverse));
			registry.insert(Identifier::new("split"), unfuck_list_function(split));
			registry.insert(Identifier::new("push"), unfuck_list_function(push));
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
	if index < 0 {
		index += len as i64;
	}
	if index < 0 || index as usize >= len {
		return Err(Error::IndexOutOfBounds { index: index as usize, len, span });
	}
	Ok(index as usize)
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
	_span: SourceSpan,
) -> Result<Option<Stackable<'gc>>, Error> {
	Ok(Some(Stackable::Integer(this.list.len() as i64)))
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
	let prefix = List::new(this.list[.. index].into(), mc);
	Ok(Some(Stackable::List(prefix)))
}

fn after<'gc>(
	this: &List<'gc>,
	stack: &mut Stack<'gc>,
	mc: &Mutation<'gc>,
	span: SourceSpan,
) -> Result<Option<Stackable<'gc>>, Error> {
	let index = get_index(stack, this.list.len(), span)?;
	let suffix = List::new(this.list[index ..].into(), mc);
	Ok(Some(Stackable::List(suffix)))
}

fn reverse<'gc>(
	this: &List<'gc>,
	_stack: &mut Stack<'gc>,
	mc: &Mutation<'gc>,
	_span: SourceSpan,
) -> Result<Option<Stackable<'gc>>, Error> {
	let list_copy = this.list.iter().rev().cloned().collect();
	Ok(Some(Stackable::List(List::new(list_copy, mc))))
}

fn split<'gc>(
	this: &List<'gc>,
	stack: &mut Stack<'gc>,
	mc: &Mutation<'gc>,
	span: SourceSpan,
) -> Result<Option<Stackable<'gc>>, Error> {
	let index = get_index(stack, this.list.len(), span)?;
	let (first, second) = this.list.split_at(index);
	let first_list = List::new(first.into(), mc);
	let second_list = List::new(second.into(), mc);
	Ok(Some(Stackable::List(List::new(smallvec![Stackable::List(first_list), Stackable::List(second_list)], mc))))
}

fn push<'gc>(
	this: &List<'gc>,
	stack: &mut Stack<'gc>,
	mc: &Mutation<'gc>,
	span: SourceSpan,
) -> Result<Option<Stackable<'gc>>, Error> {
	let mut list_copy = this.list.clone();
	list_copy.push(stack.pop(span)?);
	Ok(Some(Stackable::List(List::new(list_copy, mc))))
}
