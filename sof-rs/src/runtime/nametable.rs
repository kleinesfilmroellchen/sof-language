use std::collections::HashMap;

use gc_arena_derive::Collect;
use miette::SourceSpan;

use crate::error::Error;
use crate::lexer::Identifier;
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
    pub entries: HashMap<Identifier, Stackable<'gc>>,
    pub kind: NametableType,
}

impl<'gc> Nametable<'gc> {
    pub fn new(kind: NametableType) -> Self {
        Self {
            entries: HashMap::new(),
            kind,
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
}
