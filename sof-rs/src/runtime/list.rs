use std::cell::LazyCell;

use ahash::HashMapExt;
use gc_arena::lock::{GcRefLock, RefLock};
use gc_arena::{Collect, Mutation};
use miette::SourceSpan;
use smallvec::SmallVec;

use crate::error::Error;
use crate::identifier::Identifier;
use crate::runtime::stackable::BuiltinMethodRegistry;
use crate::runtime::{Stack, Stackable};

pub type ListVec<'gc> = SmallVec<[Stackable<'gc>; 16]>;

#[derive(Debug, Default, Clone, PartialEq)]
pub struct List<'gc> {
	pub(crate) list: ListVec<'gc>,
}

unsafe impl<'gc> Collect for List<'gc> {
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
	pub const REGISTRY: LazyCell<BuiltinMethodRegistry<'gc, List<'gc>>> = LazyCell::new(|| {
		let mut registry = BuiltinMethodRegistry::new();
		registry.insert(Identifier::new("idx"), idx);
		registry.insert(Identifier::new("length"), length);
		registry
	});

	pub fn new(list: ListVec<'gc>, mc: &Mutation<'gc>) -> GcRefLock<'gc, Self> {
		GcRefLock::new(mc, RefLock::new(Self { list }))
	}
}

fn idx<'gc>(this: &List<'gc>, stack: &mut Stack<'gc>, span: SourceSpan) -> Result<Option<Stackable<'gc>>, Error> {
	let Stackable::Integer(mut index) = stack.pop(span)? else {
		return Err(Error::InvalidTypeNative { name: "idx".into(), value: "List".into(), span });
	};
	if index < 0 {
		index = this.list.len() as i64 + index;
	}
	let value = this.list.get(index as usize).cloned().ok_or_else(|| Error::IndexOutOfBounds {
		index: index as usize,
		len: this.list.len(),
		span,
	})?;

	Ok(Some(value))
}

fn length<'gc>(this: &List<'gc>, _stack: &mut Stack<'gc>, _span: SourceSpan) -> Result<Option<Stackable<'gc>>, Error> {
	Ok(Some(Stackable::Integer(this.list.len() as i64)))
}
