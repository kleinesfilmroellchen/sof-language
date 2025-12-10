//! Module support.

use std::path::{Path, PathBuf};
use std::sync::Arc;

use ahash::HashMap;
use internment::ArcIntern;
use log::debug;

use crate::error::Error;
use crate::parser::{self, lexer};
use crate::runtime::stackable::TokenVec;

fn module_name_to_file_path(name: &str) -> PathBuf {
	let mut with_slashes = name.replace('.', std::path::MAIN_SEPARATOR_STR);
	// FIXME: handle .stackof extension?
	with_slashes.push_str(".sof");
	PathBuf::from(with_slashes)
}

/// Retrieves the path to a module based on where the module was imported from (`calling_path`) and where the standard
/// library is situated (`standard_library_path`).
///
/// This function does not verify that the returned path exists; some platforms make use of symbolic paths that are
/// never used with the file system (especially when there is no file system).
pub fn path_to_module(module_name: &str, calling_directory: &Path, standard_library_path: &Path) -> PathBuf {
	match module_name.strip_prefix('.') {
		None => standard_library_path.join(module_name_to_file_path(module_name)),
		// FIXME: relative-upwards imports are not handled in the Java implementation either
		Some(relative_module) => calling_directory.join(module_name_to_file_path(relative_module)),
	}
}

#[derive(Debug)]
pub struct ModuleRegistry {
	parsed_modules:        HashMap<ArcIntern<PathBuf>, TokenVec>,
	standard_library_path: PathBuf,
}

impl ModuleRegistry {
	pub fn new(standard_library_path: impl Into<PathBuf>) -> Self {
		Self { parsed_modules: HashMap::default(), standard_library_path: standard_library_path.into() }
	}

	/// Look up the module in the registry, and if not available, lex and parse it after loading it from disk.
	pub fn lookup_module(
		&mut self,
		module_name: &str,
		calling_module: &Path,
	) -> Result<(ArcIntern<PathBuf>, TokenVec), Error> {
		let calling_directory = calling_module.parent().unwrap_or(Path::new("/"));
		let module_path = ArcIntern::new(path_to_module(module_name, calling_directory, &self.standard_library_path));

		if let Some(already_parsed_module) = self.parsed_modules.get(&module_path) {
			debug!("{} was already parsed", module_path.display());
			return Ok((module_path, already_parsed_module.clone()));
		}

		let code = std::fs::read_to_string(&*module_path)
			.map_err(|err| Error::ModuleFileNotReadable { path: module_path.to_path_buf(), inner: err })?;
		let lexed = lexer::lex(code)?;
		let parsed = Arc::new(parser::parse(lexed)?);

		self.parsed_modules.insert(module_path.clone(), parsed.clone());
		Ok((module_path, parsed))
	}
}
