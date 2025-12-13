use std::path::{Path, PathBuf};
use std::str::FromStr;

use argh::FromArgs;

#[derive(FromArgs, Debug, Clone)]
/// sof-rs: interpreter for the Stack with Objects and Functions Language.
pub struct Args {
	/// run sof-rs in interactive mode.
	#[argh(switch, short = 'i')]
	interactive: bool,

	/// code to execute (before interactive mode or source files)
	#[argh(option, short = 'c')]
	command: Option<String>,

	/// internal debugging options, see documentation for a full list.
	#[argh(option, short = 'D', long = "debug-opt")]
	pub(crate) debug_options: Vec<DebugOption>,

	/// path to library
	#[argh(option, short = 'l', default = "Path::new(\"../lib\").into()")]
	pub(crate) library_path: PathBuf,

	/// SOF source file to execute. (Root module.)
	#[argh(positional)]
	pub(crate) input: Option<PathBuf>,
}

impl Args {
	/// Determines whether a REPL should be opened. The rules are:
	/// - if --interactive, always open REPL
	/// - otherwise, only open REPL if neither input file nor inline command was used.
	pub fn should_open_repl(&self) -> bool {
		self.interactive || (self.input.is_none() && self.command.is_none())
	}

	/// Configure the logger with the options that control logging behavior.
	pub fn configure_env_logger(&self, builder: &mut env_logger::Builder) {
		let mut should_apply_default = true;
		#[allow(clippy::match_wildcard_for_single_variants)]
		for log_level in self.debug_options.iter().filter_map(|f| match f {
			DebugOption::LogLevel(log_level) => Some(log_level),
			_ => None,
		}) {
			log_level.configure_env_logger(builder);
			should_apply_default = false;
		}

		if should_apply_default {
			builder.filter_level(log::LevelFilter::Info);
		}
	}
}

#[derive(Clone, Debug)]
pub enum DebugOption {
	/// Outputs serialized version of stack after execution, which can be used as a snapshot for faster module loading
	/// or integrating into the binary.
	ExportSnapshot { target_filename: PathBuf },
	/// Sets interpreter log level.
	LogLevel(LogLevel),
}

#[derive(Clone, Copy, Debug)]
pub enum LogLevel {
	/// All logging, even default logging, disabled.
	Off,
	/// Enable debug logging in sof-rs.
	SelfDebug,
	/// Enable debug logging everywhere.
	AllDebug,
	/// Enable tracing in sof-rs (no change to external debug logging).
	Trace,
}

impl LogLevel {
	pub fn configure_env_logger(self, builder: &mut env_logger::Builder) {
		match self {
			Self::Off => builder.filter_level(log::LevelFilter::Off),
			Self::SelfDebug => builder.filter_module("sof", log::LevelFilter::Debug),
			Self::AllDebug => builder.filter_level(log::LevelFilter::Debug),
			Self::Trace => builder.filter_module("sof", log::LevelFilter::Trace),
		};
	}
}

impl FromStr for DebugOption {
	type Err = String;

	fn from_str(s: &str) -> Result<Self, Self::Err> {
		let Some((option, value)) = s.split_once('=') else {
			return Err(format!("invalid debug option {s}, should have format `key=value`"));
		};
		match option {
			"export-snapshot" => Ok(Self::ExportSnapshot { target_filename: Path::new(value).to_owned() }),
			"log" => Ok(Self::LogLevel(value.parse()?)),
			_ => Err(format!("invalid debug option {option}")),
		}
	}
}

impl FromStr for LogLevel {
	type Err = String;

	fn from_str(s: &str) -> Result<Self, Self::Err> {
		Ok(match s {
			"off" => Self::Off,
			"self-debug" => Self::SelfDebug,
			"all-debug" => Self::AllDebug,
			"trace" => Self::Trace,
			_ => return Err(format!("invalid log configuration: {s}")),
		})
	}
}
