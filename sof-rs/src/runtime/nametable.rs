use std::collections::HashMap;

use gc_arena_derive::Collect;
use miette::SourceSpan;

use crate::error::Error;
use crate::identifier::Identifier;
use crate::runtime::Stackable;

#[derive(Debug, Clone, Copy, Eq, PartialEq, Collect)]
#[collect(require_static)]
pub enum NametableType {
	Global,
	Module,
	Function,
	#[allow(unused)]
	Object,
}

#[derive(Debug, Collect, PartialEq)]
#[collect(no_drop)]
pub struct Nametable<'gc> {
	pub entries:        HashMap<Identifier, Stackable<'gc>, ahash::RandomState>,
	pub kind:           NametableType,
	pub return_value:   Option<Stackable<'gc>>,
	/// For modules: contains the exported symbols of the module that is using this nametable.
	pub exported_names: HashMap<Identifier, Stackable<'gc>, ahash::RandomState>,
}

impl<'gc> Nametable<'gc> {
	pub fn new(kind: NametableType) -> Self {
		Self {
			entries: HashMap::with_hasher(ahash::RandomState::with_seed(2)),
			kind,
			return_value: None,
			exported_names: HashMap::with_hasher(ahash::RandomState::with_seed(3)),
		}
	}

	pub fn define(&mut self, name: Identifier, value: Stackable<'gc>) {
		self.entries.insert(name, value);
	}

	pub fn lookup(&self, name: &Identifier, span: SourceSpan) -> Result<Stackable<'gc>, Error> {
		self.entries.get(name).ok_or_else(|| Error::UndefinedValue { name: name.clone(), span }).cloned()
	}

	pub fn set_return_value(&mut self, value: Stackable<'gc>) {
		self.return_value = Some(value);
	}

	pub fn export(&mut self, name: Identifier, value: Stackable<'gc>) {
		self.exported_names.insert(name, value);
	}

	pub fn import_from_module(&mut self, module_nametable: &Nametable<'gc>) {
		debug_assert!(module_nametable.kind == NametableType::Module);
		for (k, v) in &module_nametable.exported_names {
			self.entries.insert(k.clone(), v.clone());
		}
	}
}
