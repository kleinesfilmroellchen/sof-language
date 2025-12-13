//! Program optimization.

use std::sync::Arc;

use log::{debug, trace};

use crate::runtime::stackable::TokenVec;
use crate::token::{InnerToken, Literal, Token};

/// Single optimization pass that can be executed on a token list.
pub type Pass = fn(tokens: &mut Vec<Token>);

pub static DEFAULT_PASSES: [Pass; 2] = [passes::combine_id_calls, passes::combine_literal_pushes];

pub fn run_passes(tokens: &mut TokenVec) {
	debug!("running optimizer on {} tokens…", tokens.len());
	// TODO: super wasteful
	let mut work_tokens = (**tokens).clone();
	for pass in DEFAULT_PASSES {
		pass(&mut work_tokens);
		trace!("after pass {:?}: {work_tokens:#?}", pass);
	}
	*tokens = Arc::new(work_tokens);
}

/// Recursively apply a pass to any nested token lists.
#[inline]
fn recurse_pass(pass: Pass, token: &mut Token) {
	match &mut token.inner {
		InnerToken::Literal(Literal::CodeBlock(cb)) => {
			let mut new_block = cb.as_ref().clone();
			pass(&mut new_block);
			*cb = Arc::new(new_block);
		},
		_ => {},
	}
}

mod passes {
	use log::{debug, trace};
	use miette::SourceSpan;
	use smallvec::SmallVec;

	use crate::optimizer::recurse_pass;
	use crate::token::{Command, InnerToken, Literal, Token};

	/// Optimization pass which combines identifiers and following calls / double calls into simplified lookup/call
	/// tokens.
	pub fn combine_id_calls(tokens: &mut Vec<Token>) {
		debug!("combine_id_calls");

		let mut idx = 0;
		while idx < tokens.len() {
			recurse_pass(combine_id_calls, &mut tokens[idx]);

			let current = &tokens[idx];
			match &current.inner {
				InnerToken::Literal(Literal::Identifier(id)) => {
					let start_index = idx;
					idx += 1;
					let Some(Token { inner: InnerToken::Command(Command::Call), span: end_span }) = tokens.get(idx)
					else {
						// Nothing to optimize.
						debug_assert!(idx == start_index + 1);
						continue;
					};

					idx += 1;
					let Some(Token { inner: InnerToken::Command(Command::Call), span: end_span }) = tokens.get(idx)
					else {
						let end_index = idx - 1;
						// Found only a single call token afterwards.
						trace!("combining lookup in range [{start_index}; {end_index}]");
						tokens.splice(start_index ..= end_index, [Token {
							inner: InnerToken::LookupName(id.clone()),
							span:  SourceSpan::new(
								current.span.offset().into(),
								end_span.offset() + end_span.len() - current.span.offset(),
							),
						}]);
						idx -= 1;
						debug_assert!(idx == start_index + 1);
						continue;
					};

					// Found two call tokens afterwards.
					let end_index = idx;
					trace!("combining name call in range [{start_index}; {end_index}]");
					tokens.splice(start_index ..= end_index, [Token {
						inner: InnerToken::CallName(id.clone()),
						span:  SourceSpan::new(
							current.span.offset().into(),
							end_span.offset() + end_span.len() - current.span.offset(),
						),
					}]);
					idx -= 1;
					debug_assert!(idx == start_index + 1);
					continue;
				},
				_ => {},
			}
			idx += 1;
		}
	}

	/// Optimization pass which combines multiple literal tokens into a single token which pushes multiple literals at
	/// once.
	pub fn combine_literal_pushes(tokens: &mut Vec<Token>) {
		debug!("combine_literal_pushes");

		let mut start_span = SourceSpan::new(0.into(), 0);
		let mut end_span = SourceSpan::new(0.into(), 0);
		let mut start_index = 0;
		let mut previous_literal_tokens = smallvec::SmallVec::new();

		let mut idx = 0;
		while idx < tokens.len() {
			recurse_pass(combine_literal_pushes, &mut tokens[idx]);

			let current = &tokens[idx];
			match &current.inner {
				InnerToken::Literals(literals) => {
					// Set start span via first token.
					if previous_literal_tokens.is_empty() {
						start_span = current.span;
						start_index = idx;
					}
					end_span = current.span;
					previous_literal_tokens.extend(literals.clone());
				},
				InnerToken::Literal(literal) => {
					// Set start span via first token.
					if previous_literal_tokens.is_empty() {
						start_span = current.span;
						start_index = idx;
					}
					end_span = current.span;
					previous_literal_tokens.push(literal.clone());
				},
				_ => {
					if idx > start_index + 1 && !previous_literal_tokens.is_empty() {
						// End the literal list
						trace!("combining literals in range [{start_index}; {}]", idx - 1);
						let previous_lit_tokens = previous_literal_tokens;
						previous_literal_tokens = SmallVec::new();
						tokens.splice(start_index .. idx, [Token {
							inner: InnerToken::Literals(previous_lit_tokens),
							span:  SourceSpan::new(
								start_span.offset().into(),
								end_span.offset() + end_span.len() - start_span.offset(),
							),
						}]);
						idx = start_index + 1;
						continue;
					}
					previous_literal_tokens = SmallVec::new();
				},
			}
			idx += 1;
		}
	}
}
