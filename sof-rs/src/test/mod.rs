use std::path::Path;

use miette::Result;

use crate::sof_main;

#[cfg(feature = "nightly")]
extern crate test;

const BASE_TEST_FILE_PATH: &str = "../test/klfr/sof/test/source";

fn run_file_test(name: impl AsRef<Path>) -> Result<()> {
	let full_path = Path::new(BASE_TEST_FILE_PATH).join(name);
	let code = std::fs::read_to_string(full_path).map_err(|e| miette::miette!("i/o error: {e}"))?;
	sof_main(code)
}

macro_rules! file_tests {
	($($test_names:ident),*) => {$(
		#[cfg(not(feature = "nightly"))]
		#[test]
		fn $test_names() -> Result<()> {
			run_file_test(concat!(stringify!($test_names), ".sof"))
		}
		#[cfg(feature = "nightly")]
		#[bench]
		fn $test_names(b: &mut test::Bencher) {
			b.iter(|| run_file_test(concat!(stringify!($test_names), ".sof")).unwrap());
		}
	)*};
	($($test_names:ident),*,) => { file_tests!{$($test_names),*} };
}

file_tests! {
	arithmetic,
	boolean,
	callability,
	codeblock,
	comparison,
	controlflow,
	define,
	function,
	list,
	miscellaneous,
	modules,
	nativecall,
	objects,
	string,
	test_preamble,
}

mod bench {
	#[allow(unused)] use super::*;

	#[test]
	fn benchmark_many_calls() -> Result<()> {
		run_file_test("benchmark/many_calls.sof")
	}
}
