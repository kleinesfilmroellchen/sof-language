use std::borrow::Borrow;

use miette::SourceSpan;

use crate::error::Error;
use crate::token::{Command, InnerToken, Token};

pub mod lexer;

pub fn parse<T>(tokens: impl IntoIterator<Item = T>) -> Result<Vec<Token>, Error>
where
	T: Borrow<lexer::Token>,
{
	let mut token_iter = tokens.into_iter();
	let mut output = Vec::new();
	while let Some(t) = token_iter.next() {
		let token = &t.borrow().token;
		let span = t.borrow().span;
		match token {
			lexer::RawToken::Keyword(lexer::Keyword::ListStart) =>
				output.push(Token { inner: InnerToken::ListStart, span }),
			lexer::RawToken::Keyword(lexer::Keyword::Curry) => output.push(Token { inner: InnerToken::Curry, span }),
			lexer::RawToken::Decimal(decimal) => output.push(Token { inner: InnerToken::Decimal(*decimal), span }),
			lexer::RawToken::Integer(int) => output.push(Token { inner: InnerToken::Integer(*int), span }),
			lexer::RawToken::String(string) => output.push(Token { inner: InnerToken::String(string.clone()), span }),
			lexer::RawToken::Boolean(boolean) => output.push(Token { inner: InnerToken::Boolean(*boolean), span }),
			lexer::RawToken::Identifier(identifier) =>
				output.push(Token { inner: InnerToken::Identifier(identifier.clone()), span }),
			lexer::RawToken::Keyword(lexer::Keyword::CodeBlockStart) => {
				let mut depth = 1usize;
				let mut inner_tokens = Vec::new();
				let mut last_span = span;
				for full_next_token in token_iter.by_ref() {
					let next_inner_token = &full_next_token.borrow().token;
					let next_span = full_next_token.borrow().span;
					last_span = next_span;
					match next_inner_token {
						lexer::RawToken::Keyword(lexer::Keyword::CodeBlockStart) => {
							depth += 1;
						},
						lexer::RawToken::Keyword(lexer::Keyword::CodeBlockEnd) => {
							depth -= 1;
						},
						_ => {},
					}
					if depth == 0 {
						break;
					}
					inner_tokens.push(full_next_token);
				}
				if depth > 0 {
					return Err(Error::UnclosedCodeBlock { start_span: span, end_span: Some(last_span) });
				}

				let span = SourceSpan::new(span.offset().into(), last_span.offset() - span.offset() + last_span.len());
				let parsed = parse(inner_tokens)?;
				output.push(Token { inner: InnerToken::CodeBlock(parsed.into()), span });
			},
			lexer::RawToken::Keyword(lexer::Keyword::CodeBlockEnd) => {
				return Err(Error::UnclosedCodeBlock { start_span: span, end_span: None });
			},

			// desugar convenience commands into two separate tokens to simplify interpreter
			lexer::RawToken::Keyword(lexer::Keyword::DoubleCall) => {
				output.extend_from_slice(&[Token { inner: InnerToken::Command(Command::Call), span }, Token {
					inner: InnerToken::Command(Command::Call),
					span,
				}]);
			},

			lexer::RawToken::Keyword(keyword) =>
				output.push(Token { inner: InnerToken::Command(Command::from_keyword_checked(*keyword)), span }),
		}
	}
	Ok(output)
}
