use log::debug;
use log::info;
use miette::miette;
use rustyline::Config;
use rustyline::error::ReadlineError;

use crate::interpreter::new_arena;
use crate::interpreter::run;
use crate::interpreter::run_on_arena;
use crate::runtime::StackArena;

mod error;
mod interpreter;
mod lexer;
mod parser;
mod runtime;

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
    env_logger::Builder::from_default_env()
        .filter_level(log::LevelFilter::Info)
        .filter_module("sof", log::LevelFilter::Trace)
        .init();

    if let Some(filename) = std::env::args().nth(1) {
        info!(
            "sof version {} (sof-rs), main file is {filename}",
            env!("CARGO_PKG_VERSION"),
        );
        let code = std::fs::read_to_string(&filename).map_err(|err| {
            miette! {
                code = "IOError",
                "could not read source file {filename}: {err}"
            }
        })?;
        let result = sof_main(&code);
        match result {
            Ok(_) => Ok(()),
            Err(why) => {
                let full_err = why.with_source_code(code);
                Err(full_err.into())
            }
        }
    } else {
        let mut rl = rustyline::DefaultEditor::with_config(
            Config::builder()
                .auto_add_history(true)
                .indent_size(4)
                .build(),
        )
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
                }
                Err(ReadlineError::Interrupted | ReadlineError::Eof) => {
                    break Ok(());
                }
                Err(err) => {
                    println!("error: {err:#?}");
                    break Ok(());
                }
            }
        }
    }
}

fn run_code_on_arena(code: impl AsRef<str>, arena: &mut StackArena) -> miette::Result<()> {
    let result = lexer::lex(code)?;
    let parsed = parser::parse(result.iter().collect())?;
    debug!("parsed code as {parsed:#?}");
    run_on_arena(arena, parsed)?;
    Ok(())
}

fn sof_main(code: impl AsRef<str>) -> miette::Result<()> {
    let lexed = lexer::lex(code)?;
    debug!("lexed: {lexed:#?}");
    let parsed = parser::parse(lexed.iter().collect())?;
    debug!("parsed: {parsed:#?}");
    run(parsed)?;
    Ok(())
}
