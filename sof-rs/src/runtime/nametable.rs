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
    Function,
    Object,
}

#[derive(Debug, Collect, PartialEq)]
#[collect(no_drop)]
pub struct Nametable<'gc> {
    pub entries: HashMap<Identifier, Stackable<'gc>, ahash::RandomState>,
    pub kind: NametableType,
    pub return_value: Option<Stackable<'gc>>,
}

impl<'gc> Nametable<'gc> {
    pub fn new(kind: NametableType) -> Self {
        Self {
            entries: HashMap::with_hasher(ahash::RandomState::with_seed(2)),
            kind,
            return_value: None,
        }
    }

    pub fn define(&mut self, name: Identifier, value: Stackable<'gc>) {
        self.entries.insert(name, value);
    }

    pub fn lookup(&self, name: Identifier, span: SourceSpan) -> Result<Stackable<'gc>, Error> {
        self.entries
            .get(&name)
            .ok_or(Error::UndefinedValue {
                name: name.clone(),
                span,
            })
            .cloned()
    }

    pub fn set_return_value(&mut self, value: Stackable<'gc>) {
        self.return_value = Some(value);
    }
}
