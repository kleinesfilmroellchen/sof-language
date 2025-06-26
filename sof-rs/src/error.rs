use std::num::ParseFloatError;
use std::num::ParseIntError;

use miette::Diagnostic;
use miette::SourceSpan;
use thiserror::Error;

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
        ident: String,
        #[label = "invalid"]
        span: SourceSpan,
    },
    #[error("invalid number \"{number_text}\"")]
    #[diagnostic(code(SyntaxError))]
    InvalidInteger {
        number_text: String,
        inner: ParseIntError,
        #[label("{inner}")]
        span: SourceSpan,
    },
    #[error("invalid number \"{number_text}\"")]
    #[diagnostic(code(SyntaxError))]
    InvalidFloat {
        number_text: String,
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
    #[error("invalid types for operation {operation}: {lhs} and {rhs}")]
    #[diagnostic(code(TypeError))]
    InvalidTypes {
        operation: Command,
        lhs: String,
        rhs: String,
        #[label]
        span: SourceSpan,
    },
    #[error("invalid type for operation {operation}: {value}")]
    #[diagnostic(code(TypeError))]
    InvalidType {
        operation: Command,
        value: String,
        #[label]
        span: SourceSpan,
    },
    #[error("divide by zero: {lhs} / {rhs}")]
    #[diagnostic(code(ArithmeticError))]
    DivideByZero {
        lhs: String,
        rhs: String,
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
