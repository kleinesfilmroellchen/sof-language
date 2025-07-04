use std::path::Path;

use miette::Result;

use crate::sof_main;

const BASE_TEST_FILE_PATH: &str = "../test/klfr/sof/test/source";

fn run_file_test(name: impl AsRef<Path>) -> Result<()> {
    let full_path = Path::new(BASE_TEST_FILE_PATH).join(name);
    let code = std::fs::read_to_string(full_path).map_err(|e| miette::miette!("i/o error: {e}"))?;
    sof_main(code)
}

macro_rules! file_tests {
    ($($test_names:ident),*) => {$(
        #[test]
        fn $test_names() -> Result<()> {
            run_file_test(concat!(stringify!($test_names), ".sof"))
        }
    )*};
    ($($test_names:ident),*,) => { file_tests!{$($test_names),*} };
}

file_tests! {
    arithmetic,
    boolean,
    codeblock,
    comparison,
    controlflow,
    convert_callable,
    define,
    function,
    list,
    miscellaneous,
    modules,
    objects,
    string,
    test_preamble,
}
