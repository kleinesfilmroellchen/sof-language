use std::collections::HashMap;
use std::collections::VecDeque;
use std::fmt::Display;
use std::ops::Add;
use std::ops::Div;
use std::ops::Mul;
use std::ops::Rem;
use std::ops::Sub;
use std::rc::Rc;

use gc_arena::Arena;
use gc_arena::Rootable;
use gc_arena::lock::GcRefLock;
use gc_arena_derive::Collect;
use miette::SourceSpan;

use crate::error::Error;
use crate::lexer::Identifier;
use crate::parser::Command;
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

#[derive(Debug, Collect, PartialEq)]
#[collect(require_static)]
pub struct CodeBlock {
    pub(crate) code: Vec<Token>,
}

#[derive(Debug, Collect, PartialEq)]
#[collect(require_static)]
pub struct Function {
    arguments: usize,
    is_constructor: bool,
    code: Vec<Token>,
}

#[derive(Debug, Collect, PartialEq)]
#[collect(no_drop)]
pub struct Object<'gc> {
    fields: Nametable<'gc>,
}

#[derive(Debug, Collect, PartialEq)]
#[collect(no_drop)]
pub struct Nametable<'gc> {
    entries: HashMap<Identifier, GcRefLock<'gc, Stackable<'gc>>>,
}

#[derive(Debug, Collect)]
#[collect(no_drop)]
pub struct Stack<'gc>(pub GcRefLock<'gc, VecDeque<Stackable<'gc>>>);

pub type StackArena = Arena<Rootable![Stack<'_>]>;

macro_rules! numeric_op {
    ($vis:vis $func_name:ident($command:ident, $func:ident)) => {
        $vis fn $func_name(
            &self,
            other: Stackable<'gc>,
            span: SourceSpan,
        ) -> Result<Stackable<'gc>, Error> {
            Ok(match (self, &other) {
                (Stackable::Integer(lhs), Stackable::Integer(rhs)) => Stackable::Integer(lhs.$func(rhs)),
                (Stackable::Integer(lhs), Stackable::Decimal(rhs)) => {
                    Stackable::Decimal((*lhs as f64).$func(rhs))
                }
                (Stackable::Decimal(lhs), Stackable::Integer(rhs)) => {
                    Stackable::Decimal(lhs.$func(*rhs as f64))
                }
                (Stackable::Decimal(lhs), Stackable::Decimal(rhs)) => Stackable::Decimal(lhs.$func(rhs)),
                _ => {
                    return Err(Error::InvalidTypes {
                        operation: Command::$command,
                        lhs: self.to_string(),
                        rhs: other.to_string(),
                        span,
                    });
                }
            })
        }
    };
}

impl<'gc> Stackable<'gc> {
    numeric_op!(pub add(Plus, add));
    numeric_op!(pub subtract(Minus, sub));
    numeric_op!(pub multiply(Multiply, mul));
    numeric_op!(divide_unchecked(Divide, div));
    numeric_op!(modulus_unchecked(Modulus, rem));

    pub fn divide(&self, other: Stackable<'gc>, span: SourceSpan) -> Result<Stackable<'gc>, Error> {
        if matches!(other, Stackable::Integer(0) | Stackable::Decimal(0.0)) {
            Err(Error::DivideByZero {
                lhs: self.to_string(),
                rhs: other.to_string(),
                span,
            })
        } else {
            self.divide_unchecked(other, span)
        }
    }

    pub fn modulus(
        &self,
        other: Stackable<'gc>,
        span: SourceSpan,
    ) -> Result<Stackable<'gc>, Error> {
        if matches!(other, Stackable::Integer(0) | Stackable::Decimal(0.0)) {
            Err(Error::DivideByZero {
                lhs: self.to_string(),
                rhs: other.to_string(),
                span,
            })
        } else {
            self.modulus_unchecked(other, span)
        }
    }

    pub fn shift_left(
        &self,
        other: Stackable<'gc>,
        span: SourceSpan,
    ) -> Result<Stackable<'gc>, Error> {
        Ok(match (self, &other) {
            (Stackable::Integer(lhs), Stackable::Integer(rhs)) => {
                Stackable::Integer(lhs.unbounded_shl((*rhs & 0xffff_ffff) as u32))
            }
            _ => {
                return Err(Error::InvalidTypes {
                    operation: Command::LeftShift,
                    lhs: self.to_string(),
                    rhs: other.to_string(),
                    span,
                });
            }
        })
    }
    pub fn shift_right(
        &self,
        other: Stackable<'gc>,
        span: SourceSpan,
    ) -> Result<Stackable<'gc>, Error> {
        Ok(match (self, &other) {
            (Stackable::Integer(lhs), Stackable::Integer(rhs)) => {
                Stackable::Integer(lhs.unbounded_shr((*rhs & 0xffff_ffff) as u32))
            }
            _ => {
                return Err(Error::InvalidTypes {
                    operation: Command::RightShift,
                    lhs: self.to_string(),
                    rhs: other.to_string(),
                    span,
                });
            }
        })
    }
}

impl<'gc> Display for Stackable<'gc> {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            Stackable::Integer(int) => write!(f, "{int}"),
            Stackable::Decimal(dec) => write!(f, "{dec}"),
            Stackable::Boolean(boolean) => write!(f, "{boolean}"),
            Stackable::Identifier(identifier) => write!(f, "{identifier}"),
            Stackable::String(string) => write!(f, "\"{string}\""),
            Stackable::CodeBlock(cb) => write!(f, "[CodeBlock {}n ]", cb.borrow().code.len()),
            Stackable::Function(func) => write!(
                f,
                "[Function/{} {}n ]",
                func.borrow().arguments,
                func.borrow().code.len()
            ),
            Stackable::Object(_) => write!(f, "[Object]"),
            Stackable::Nametable(nt) => write!(f, "NT[{}]", nt.borrow().entries.len()),
            Stackable::ListStart => write!(f, "["),
        }
    }
}

impl<'gc> PartialEq for Stackable<'gc> {
    fn eq(&self, other: &Self) -> bool {
        match (self, other) {
            (Self::Integer(l0), Self::Integer(r0)) => l0 == r0,
            (Self::Integer(l0), Self::Decimal(r0)) => *l0 as f64 == *r0,
            (Self::Decimal(l0), Self::Decimal(r0)) => l0 == r0,
            (Self::Decimal(l0), Self::Integer(r0)) => *l0 == *r0 as f64,
            (Self::Boolean(l0), Self::Boolean(r0)) => l0 == r0,
            (Self::Identifier(l0), Self::Identifier(r0)) => l0 == r0,
            (Self::String(l0), Self::String(r0)) => l0 == r0,
            (Self::CodeBlock(l0), Self::CodeBlock(r0)) => l0 == r0,
            (Self::Function(l0), Self::Function(r0)) => l0 == r0,
            (Self::Object(l0), Self::Object(r0)) => l0 == r0,
            (Self::Nametable(l0), Self::Nametable(r0)) => l0 == r0,
            (Self::ListStart, Self::ListStart) => true,
            _ => false,
        }
    }
}
