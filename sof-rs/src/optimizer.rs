//! Program optimization.

use std::sync::Arc;

use log::{debug, trace};

use crate::runtime::stackable::TokenVec;
use crate::token::Token;

/// Single optimization pass that can be executed on a token list.
pub type Pass = fn(tokens: &mut Vec<Token>);

pub static DEFAULT_PASSES: [Pass; 1] = [passes::combine_literal_pushes];

pub fn run_passes(tokens: &mut TokenVec) {
	debug!("running optimizer on {} tokens…", tokens.len());
	// TODO: super wasteful
	let mut work_tokens = (**tokens).clone();
	for pass in DEFAULT_PASSES {
		pass(&mut work_tokens);
	}
	trace!("after optimizer: {work_tokens:#?}");
	*tokens = Arc::new(work_tokens);
}

mod passes {
	use log::{debug, trace};
	use miette::SourceSpan;
	use smallvec::SmallVec;

	use crate::token::{InnerToken, Token};

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
			let current = &tokens[idx];
			if let InnerToken::Literals(literals) = &current.inner {
				// Set start span via first token.
				if previous_literal_tokens.is_empty() {
					start_span = current.span;
					start_index = idx;
				}
				end_span = current.span;
				previous_literal_tokens.extend(literals.clone());
			} else {
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
			}
			idx += 1;
		}
	}
}
