//! SOF interpreter written in Rust.

#![cfg_attr(feature = "nightly", feature(test))]

use std::path::{Path, PathBuf};
use std::sync::Arc;
use std::time;

use log::{debug, info};
use miette::{NamedSource, miette};
use rustyline::Config;
use rustyline::error::ReadlineError;

use crate::runtime::StackArena;
use crate::runtime::interpreter::{new_arena, run, run_on_arena};

mod arc_iter;
mod error;
mod identifier;
mod parser;
mod runtime;
mod token;

#[cfg(test)] mod test;

/// Returns a pretty-printed variant of the given path.
///
/// The pretty-printing rules are as follows:
/// - If the file is relative to the working directory, print a relative file name without leading `./`.
/// - If the file is not relative, i.e. its canonical path does not contain the working directory, print an absolute
///   file name. On Windows, extended path length syntax (`\\?\`) is omitted.
///
/// # Panics
/// Programming bugs.
#[must_use]
fn file_name_for(path: &Path) -> String {
	let cwd = uniform_canonicalize(&PathBuf::from(".")).unwrap();
	if path.starts_with(&cwd) {
		path.strip_prefix(cwd).unwrap().to_string_lossy().to_string()
	} else {
		path.as_os_str().to_string_lossy().to_string()
	}
}

/// Implements a more uniform canonicalization. The main difference to ``std::fs::canonicalize`` is that it doesn't
/// create the extended length syntax on Windows. This is for better compatibility with file link-supporting terminals
/// and the `trycmd` integration tests.
#[inline]
fn uniform_canonicalize(path: &Path) -> std::io::Result<PathBuf> {
	#[cfg(not(any(windows, target_family = "wasm")))]
	{
		path.canonicalize()
	}
	#[cfg(windows)]
	{
		// All extended length paths start with '\\?\' (length 4), see https://learn.microsoft.com/en-us/windows/win32/fileio/naming-a-file#maximum-path-length-limitation
		Ok(PathBuf::from(path.canonicalize()?.into_os_string().to_string_lossy()[4 ..].to_owned()))
	}
	#[cfg(target_family = "wasm")]
	{
		Ok(path.to_owned())
	}
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
	env_logger::Builder::from_default_env().filter_level(log::LevelFilter::Info).init();

	if let Some(filename) = std::env::args().nth(1) {
		let readable_filename = file_name_for(Path::new(filename.as_str()));
		info!("sof version {} (sof-rs), main file is {readable_filename}", env!("CARGO_PKG_VERSION"),);
		let code = std::fs::read_to_string(&readable_filename).map_err(|err| {
			miette! {
				code = "IOError",
				"could not read source file {readable_filename}: {err}"
			}
		})?;
		let result = sof_main(&code);
		match result {
			Ok(()) => Ok(()),
			Err(why) => Err(why.with_source_code(NamedSource::new(readable_filename, code))),
		}
	} else {
		let mut rl =
			rustyline::DefaultEditor::with_config(Config::builder().auto_add_history(true).indent_size(4).build())
				.unwrap();
		println!("sof version {} (sof-rs)", env!("CARGO_PKG_VERSION"));

		let mut arena = new_arena();

		loop {
			let readline = rl.readline(">>> ");
			match readline {
				Ok(line) => {
					let result = run_code_on_arena(line, &mut arena);
					if let Err(why) = result {
						println!("{why:?}");
					}
				},
				Err(ReadlineError::Interrupted | ReadlineError::Eof) => {
					break Ok(());
				},
				Err(err) => {
					println!("error: {err:#?}");
					break Ok(());
				},
			}
		}
	}
}

fn run_code_on_arena(code: impl AsRef<str>, arena: &mut StackArena) -> miette::Result<()> {
	let result = parser::lexer::lex(code)?;
	let parsed = Arc::new(parser::parse(result.iter().collect())?);
	debug!("parsed code as {parsed:#?}");
	run_on_arena(arena, parsed)?;
	Ok(())
}

fn sof_main(code: impl AsRef<str>) -> miette::Result<()> {
	let start_time = time::Instant::now();
	let lexed = parser::lexer::lex(code)?;
	debug!(target: "sof::lexer", "lexed: {lexed:#?}");
	let parsed = Arc::new(parser::parse(lexed.iter().collect())?);
	debug!(target: "sof::parser", "parsed: {parsed:#?}");
	let metrics = run(parsed)?;
	let end_time = time::Instant::now();

	info!(
		"Performance metrics:
total time:   {:>13.2}μs
tokens run:   {:>10}
time / token: {:>13.2?}μs
calls:        {:>10}
GC runs:      {:>10}",
		(end_time - start_time).as_nanos() as f64 / 1_000.,
		metrics.token_count,
		(end_time - start_time).as_micros() as f64 / metrics.token_count as f64,
		metrics.call_count,
		metrics.gc_count,
	);

	Ok(())
}
