use std::num::ParseFloatError;
use std::num::ParseIntError;

use flexstr::SharedStr;
use miette::Diagnostic;
use miette::SourceSpan;
use thiserror::Error;

use crate::identifier::Identifier;
use crate::parser::Command;

#[derive(Error, Diagnostic, Debug)]
pub enum Error {
    #[error("invalid character '{chr}'")]
    #[diagnostic(code(SyntaxError))]
    InvalidCharacter {
        chr: char,
        #[label = "invalid"]
        span: SourceSpan,
    },
    #[error("invalid character '{chr}' in identifier \"{ident}\"")]
    #[diagnostic(code(SyntaxError))]
    InvalidIdentifier {
        chr: char,
        ident: SharedStr,
        #[label = "invalid"]
        span: SourceSpan,
    },
    #[error("invalid number \"{number_text}\"")]
    #[diagnostic(code(SyntaxError))]
    InvalidInteger {
        number_text: SharedStr,
        inner: ParseIntError,
        #[label("{inner}")]
        span: SourceSpan,
    },
    #[error("invalid number \"{number_text}\"")]
    #[diagnostic(code(SyntaxError))]
    InvalidFloat {
        number_text: SharedStr,
        inner: ParseFloatError,
        #[label("{inner}")]
        span: SourceSpan,
    },
    #[error("unclosed string")]
    #[diagnostic(code(SyntaxError))]
    UnclosedString {
        #[label = "string terminator '\"' expected here"]
        span: SourceSpan,
    },
    #[error("unclosed code block")]
    #[diagnostic(code(SyntaxError))]
    UnclosedCodeBlock {
        #[label = "this code block is unclosed"]
        start_span: SourceSpan,
        #[label = "code block end '}}' expected here"]
        end_span: Option<SourceSpan>,
    },
    #[error("cannot pop value from empty stack")]
    #[diagnostic(code(StackAccessError))]
    MissingValue {
        #[label]
        span: SourceSpan,
    },
    #[error("no nametable available")]
    #[diagnostic(code(StackAccessError))]
    MissingNametable {
        #[label]
        span: SourceSpan,
    },
    #[error("name {name} is not defined")]
    #[diagnostic(code(NameError))]
    UndefinedValue {
        name: Identifier,
        #[label]
        span: SourceSpan,
    },
    #[error("invalid types for operation {operation}: {lhs} and {rhs}")]
    #[diagnostic(code(TypeError))]
    InvalidTypes {
        operation: Command,
        lhs: SharedStr,
        rhs: SharedStr,
        #[label]
        span: SourceSpan,
    },
    #[error("invalid type for operation {operation}: {value}")]
    #[diagnostic(code(TypeError))]
    InvalidType {
        operation: Command,
        value: SharedStr,
        #[label]
        span: SourceSpan,
    },
    #[error("divide by zero: {lhs} / {rhs}")]
    #[diagnostic(code(ArithmeticError))]
    DivideByZero {
        lhs: SharedStr,
        rhs: SharedStr,
        #[label]
        span: SourceSpan,
    },
    #[error("non-comparable values: {lhs} and {rhs}")]
    #[diagnostic(code(ArithmeticError))]
    Incomparable {
        lhs: SharedStr,
        rhs: SharedStr,
        #[label]
        span: SourceSpan,
    },
    #[error("assertion failed")]
    #[diagnostic(code(AssertionError))]
    AssertionFailed {
        #[label]
        span: SourceSpan,
    },
}
