use miette::SourceSpan;

use crate::error::Error;
use crate::token::{Command, InnerToken, Literal, Token};

pub mod lexer;

pub fn parse(tokens: Vec<&lexer::Token>) -> Result<Vec<Token>, Error> {
	let mut token_iter = tokens.into_iter();
	let mut output = Vec::new();
	while let Some(lexer::Token { token, span }) = token_iter.next() {
		match token {
			lexer::RawToken::Keyword(lexer::Keyword::ListStart) =>
				output.push(Token { inner: InnerToken::Literal(Literal::ListStart), span: *span }),
			lexer::RawToken::Keyword(lexer::Keyword::Curry) =>
				output.push(Token { inner: InnerToken::Literal(Literal::Curry), span: *span }),
			lexer::RawToken::Decimal(decimal) =>
				output.push(Token { inner: InnerToken::Literal(Literal::Decimal(*decimal)), span: *span }),
			lexer::RawToken::Integer(int) =>
				output.push(Token { inner: InnerToken::Literal(Literal::Integer(*int)), span: *span }),
			lexer::RawToken::String(string) =>
				output.push(Token { inner: InnerToken::Literal(Literal::String(string.clone())), span: *span }),
			lexer::RawToken::Boolean(boolean) =>
				output.push(Token { inner: InnerToken::Literal(Literal::Boolean(*boolean)), span: *span }),
			lexer::RawToken::Identifier(identifier) =>
				output.push(Token { inner: InnerToken::Literal(Literal::Identifier(identifier.clone())), span: *span }),
			lexer::RawToken::Keyword(lexer::Keyword::CodeBlockStart) => {
				let mut depth = 1usize;
				let mut inner_tokens = Vec::new();
				let mut last_span = *span;
				for full_next_token @ lexer::Token { token: next_inner_token, span: next_span } in token_iter.by_ref() {
					last_span = *next_span;
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
					return Err(Error::UnclosedCodeBlock { start_span: *span, end_span: Some(last_span) });
				}

				let span = SourceSpan::new(span.offset().into(), last_span.offset() - span.offset() + last_span.len());
				let parsed = parse(inner_tokens)?;
				output.push(Token { inner: InnerToken::CodeBlock(parsed.into()), span });
			},
			lexer::RawToken::Keyword(lexer::Keyword::CodeBlockEnd) => {
				return Err(Error::UnclosedCodeBlock { start_span: *span, end_span: None });
			},
			// desugar convenience commands into two separate tokens to simplify interpreter
			lexer::RawToken::Keyword(lexer::Keyword::DoubleCall) => {
				output.extend_from_slice(&[Token { inner: InnerToken::Command(Command::Call), span: *span }, Token {
					inner: InnerToken::Command(Command::Call),
					span:  *span,
				}]);
			},
			lexer::RawToken::Keyword(keyword) =>
				output.push(Token { inner: InnerToken::Command(Command::from_keyword_checked(*keyword)), span: *span }),
		}
	}
	Ok(output)
}
