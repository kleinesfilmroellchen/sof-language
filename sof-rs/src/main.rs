use crate::interpreter::run;

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
    let result = lexer::lex(code)?;
    // println!("{result:#?}");
    let parsed = parser::parse(result.iter().collect())?;
    // println!("{parsed:#?}");
    run(parsed)?;
    Ok(())
}
