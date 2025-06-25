use std::num::ParseFloatError;
use std::num::ParseIntError;

use miette::Diagnostic;
use miette::SourceSpan;
use thiserror::Error;

mod parser;

#[derive(Error, Diagnostic, Debug)]
pub enum ErrorKind {
    #[error("{message}")]
    #[diagnostic(code(ParserError))]
    Parser {
        message: &'static str,
        #[label = "here"]
        span: SourceSpan,
    },
    #[error("invalid character '{chr}'")]
    #[diagnostic(code(ParserError))]
    InvalidCharacter {
        chr: char,
        #[label = "invalid"]
        span: SourceSpan,
    },
    #[error("invalid character '{chr}' in identifier \"{ident}\"")]
    #[diagnostic(code(ParserError))]
    InvalidIdentifier {
        chr: char,
        ident: String,
        #[label = "invalid"]
        span: SourceSpan,
    },
    #[error("invalid number \"{number_text}\"")]
    #[diagnostic(code(ParserError))]
    InvalidInteger {
        number_text: String,
        inner: ParseIntError,
        #[label("{inner}")]
        span: SourceSpan,
    },
    #[error("invalid number \"{number_text}\"")]
    #[diagnostic(code(ParserError))]
    InvalidFloat {
        number_text: String,
        inner: ParseFloatError,
        #[label("{inner}")]
        span: SourceSpan,
    },
    #[error("unclosed string")]
    #[diagnostic(code(ParserError))]
    UnclosedString {
        #[label = "string terminator '\"' expected here"]
        span: SourceSpan,
    },
}

fn main() -> miette::Result<()> {
    miette::set_hook(Box::new(|_| {
        Box::new(
            miette::MietteHandlerOpts::new()
                .terminal_links(true)
                .unicode(true)
                .context_lines(3)
                .tab_width(4)
                .break_words(true)
                .with_cause_chain()
                .build(),
        )
    }))?;

    let code =
        std::fs::read_to_string(std::env::args().nth(1).expect("usage: sof <filename>")).unwrap();
    let result = sof_main(&code);
    match result {
        Ok(_) => Ok(()),
        Err(why) => {
            let full_err = why.with_source_code(code);
            Err(full_err.into())
        }
    }
}

fn sof_main(code: impl AsRef<str>) -> miette::Result<()> {
    let result = parser::lex(code)?;
    println!("{result:#?}");
    Ok(())
}
