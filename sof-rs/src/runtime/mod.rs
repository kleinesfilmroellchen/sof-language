use std::collections::HashMap;
use std::collections::VecDeque;
use std::rc::Rc;

use gc_arena::Arena;
use gc_arena::Mutation;
use gc_arena::Rootable;
use gc_arena::lock::GcRefLock;
use gc_arena_derive::Collect;

use crate::ErrorKind;
use crate::lexer::Identifier;
use crate::parser::Token;

#[derive(Debug, Collect)]
#[collect(no_drop)]
pub enum Stackable<'gc> {
    Integer(i64),
    Decimal(f64),
    Boolean(bool),
    Identifier(Identifier),
    String(Rc<String>),
    CodeBlock(GcRefLock<'gc, CodeBlock>),
    Function(GcRefLock<'gc, Function>),
    Object(GcRefLock<'gc, Object<'gc>>),
    Nametable(GcRefLock<'gc, Nametable<'gc>>),
    ListStart,
}

#[derive(Debug, Collect)]
#[collect(require_static)]
pub struct CodeBlock {
    pub(crate) code: Vec<Token>,
}

#[derive(Debug, Collect)]
#[collect(require_static)]
pub struct Function {
    arguments: usize,
    is_constructor: bool,
    code: Vec<Token>,
}

#[derive(Debug, Collect)]
#[collect(no_drop)]
pub struct Object<'gc> {
    fields: Nametable<'gc>,
}

#[derive(Debug, Collect)]
#[collect(no_drop)]
pub struct Nametable<'gc> {
    entries: HashMap<Identifier, GcRefLock<'gc, Stackable<'gc>>>,
}

#[derive(Debug, Collect)]
#[collect(no_drop)]
pub struct Stack<'gc>(pub GcRefLock<'gc, VecDeque<Stackable<'gc>>>);

pub type StackArena = Arena<Rootable![Stack<'_>]>;

impl<'gc> Stackable<'gc> {
    pub fn add(
        &self,
        other: Stackable<'gc>,
        mc: &Mutation<'gc>,
    ) -> Result<Stackable<'gc>, ErrorKind> {
        Ok(match (self, other) {
            (Stackable::Integer(lhs), Stackable::Integer(rhs)) => Stackable::Integer(*lhs + rhs),
            (Stackable::Integer(_), Stackable::Decimal(_)) => todo!(),
            (Stackable::Integer(_), Stackable::Boolean(_)) => todo!(),
            (Stackable::Integer(_), Stackable::Identifier(identifier)) => todo!(),
            (Stackable::Integer(_), Stackable::String(_)) => todo!(),
            (Stackable::Integer(_), Stackable::CodeBlock(_)) => todo!(),
            (Stackable::Integer(_), Stackable::Function(_)) => todo!(),
            (Stackable::Integer(_), Stackable::Object(_)) => todo!(),
            (Stackable::Integer(_), Stackable::Nametable(_)) => todo!(),
            (Stackable::Integer(_), Stackable::ListStart) => todo!(),
            (Stackable::Decimal(_), Stackable::Integer(_)) => todo!(),
            (Stackable::Decimal(lhs), Stackable::Decimal(rhs)) => Stackable::Decimal(*lhs + rhs),
            (Stackable::Decimal(_), Stackable::Boolean(_)) => todo!(),
            (Stackable::Decimal(_), Stackable::Identifier(identifier)) => todo!(),
            (Stackable::Decimal(_), Stackable::String(_)) => todo!(),
            (Stackable::Decimal(_), Stackable::CodeBlock(_)) => todo!(),
            (Stackable::Decimal(_), Stackable::Function(_)) => todo!(),
            (Stackable::Decimal(_), Stackable::Object(_)) => todo!(),
            (Stackable::Decimal(_), Stackable::Nametable(_)) => todo!(),
            (Stackable::Decimal(_), Stackable::ListStart) => todo!(),
            (Stackable::Boolean(_), Stackable::Integer(_)) => todo!(),
            (Stackable::Boolean(_), Stackable::Decimal(_)) => todo!(),
            (Stackable::Boolean(_), Stackable::Boolean(_)) => todo!(),
            (Stackable::Boolean(_), Stackable::Identifier(identifier)) => todo!(),
            (Stackable::Boolean(_), Stackable::String(_)) => todo!(),
            (Stackable::Boolean(_), Stackable::CodeBlock(_)) => todo!(),
            (Stackable::Boolean(_), Stackable::Function(_)) => todo!(),
            (Stackable::Boolean(_), Stackable::Object(_)) => todo!(),
            (Stackable::Boolean(_), Stackable::Nametable(_)) => todo!(),
            (Stackable::Boolean(_), Stackable::ListStart) => todo!(),
            (Stackable::Identifier(identifier), Stackable::Integer(_)) => todo!(),
            (Stackable::Identifier(identifier), Stackable::Decimal(_)) => todo!(),
            (Stackable::Identifier(identifier), Stackable::Boolean(_)) => todo!(),
            (Stackable::Identifier(identifier), Stackable::Identifier(_)) => todo!(),
            (Stackable::Identifier(identifier), Stackable::String(_)) => todo!(),
            (Stackable::Identifier(identifier), Stackable::CodeBlock(_)) => todo!(),
            (Stackable::Identifier(identifier), Stackable::Function(_)) => todo!(),
            (Stackable::Identifier(identifier), Stackable::Object(_)) => todo!(),
            (Stackable::Identifier(identifier), Stackable::Nametable(_)) => todo!(),
            (Stackable::Identifier(identifier), Stackable::ListStart) => todo!(),
            (Stackable::String(_), Stackable::Integer(_)) => todo!(),
            (Stackable::String(_), Stackable::Decimal(_)) => todo!(),
            (Stackable::String(_), Stackable::Boolean(_)) => todo!(),
            (Stackable::String(_), Stackable::Identifier(identifier)) => todo!(),
            (Stackable::String(_), Stackable::String(_)) => todo!(),
            (Stackable::String(_), Stackable::CodeBlock(_)) => todo!(),
            (Stackable::String(_), Stackable::Function(_)) => todo!(),
            (Stackable::String(_), Stackable::Object(_)) => todo!(),
            (Stackable::String(_), Stackable::Nametable(_)) => todo!(),
            (Stackable::String(_), Stackable::ListStart) => todo!(),
            (Stackable::CodeBlock(_), Stackable::Integer(_)) => todo!(),
            (Stackable::CodeBlock(_), Stackable::Decimal(_)) => todo!(),
            (Stackable::CodeBlock(_), Stackable::Boolean(_)) => todo!(),
            (Stackable::CodeBlock(_), Stackable::Identifier(identifier)) => todo!(),
            (Stackable::CodeBlock(_), Stackable::String(_)) => todo!(),
            (Stackable::CodeBlock(_), Stackable::CodeBlock(_)) => todo!(),
            (Stackable::CodeBlock(_), Stackable::Function(_)) => todo!(),
            (Stackable::CodeBlock(_), Stackable::Object(_)) => todo!(),
            (Stackable::CodeBlock(_), Stackable::Nametable(_)) => todo!(),
            (Stackable::CodeBlock(_), Stackable::ListStart) => todo!(),
            (Stackable::Function(_), Stackable::Integer(_)) => todo!(),
            (Stackable::Function(_), Stackable::Decimal(_)) => todo!(),
            (Stackable::Function(_), Stackable::Boolean(_)) => todo!(),
            (Stackable::Function(_), Stackable::Identifier(identifier)) => todo!(),
            (Stackable::Function(_), Stackable::String(_)) => todo!(),
            (Stackable::Function(_), Stackable::CodeBlock(_)) => todo!(),
            (Stackable::Function(_), Stackable::Function(_)) => todo!(),
            (Stackable::Function(_), Stackable::Object(_)) => todo!(),
            (Stackable::Function(_), Stackable::Nametable(_)) => todo!(),
            (Stackable::Function(_), Stackable::ListStart) => todo!(),
            (Stackable::Object(_), Stackable::Integer(_)) => todo!(),
            (Stackable::Object(_), Stackable::Decimal(_)) => todo!(),
            (Stackable::Object(_), Stackable::Boolean(_)) => todo!(),
            (Stackable::Object(_), Stackable::Identifier(identifier)) => todo!(),
            (Stackable::Object(_), Stackable::String(_)) => todo!(),
            (Stackable::Object(_), Stackable::CodeBlock(_)) => todo!(),
            (Stackable::Object(_), Stackable::Function(_)) => todo!(),
            (Stackable::Object(_), Stackable::Object(_)) => todo!(),
            (Stackable::Object(_), Stackable::Nametable(_)) => todo!(),
            (Stackable::Object(_), Stackable::ListStart) => todo!(),
            (Stackable::Nametable(_), Stackable::Integer(_)) => todo!(),
            (Stackable::Nametable(_), Stackable::Decimal(_)) => todo!(),
            (Stackable::Nametable(_), Stackable::Boolean(_)) => todo!(),
            (Stackable::Nametable(_), Stackable::Identifier(identifier)) => todo!(),
            (Stackable::Nametable(_), Stackable::String(_)) => todo!(),
            (Stackable::Nametable(_), Stackable::CodeBlock(_)) => todo!(),
            (Stackable::Nametable(_), Stackable::Function(_)) => todo!(),
            (Stackable::Nametable(_), Stackable::Object(_)) => todo!(),
            (Stackable::Nametable(_), Stackable::Nametable(_)) => todo!(),
            (Stackable::Nametable(_), Stackable::ListStart) => todo!(),
            (Stackable::ListStart, Stackable::Integer(_)) => todo!(),
            (Stackable::ListStart, Stackable::Decimal(_)) => todo!(),
            (Stackable::ListStart, Stackable::Boolean(_)) => todo!(),
            (Stackable::ListStart, Stackable::Identifier(identifier)) => todo!(),
            (Stackable::ListStart, Stackable::String(_)) => todo!(),
            (Stackable::ListStart, Stackable::CodeBlock(_)) => todo!(),
            (Stackable::ListStart, Stackable::Function(_)) => todo!(),
            (Stackable::ListStart, Stackable::Object(_)) => todo!(),
            (Stackable::ListStart, Stackable::Nametable(_)) => todo!(),
            (Stackable::ListStart, Stackable::ListStart) => todo!(),
        })
    }
}
