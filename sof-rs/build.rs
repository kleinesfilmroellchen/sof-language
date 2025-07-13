//! Add the nightly feature if we’re on a nightly compiler.

fn main() {
	if rustversion::cfg!(nightly) {
		println!("cargo::rustc-cfg=feature=\"nightly\"");
	}
}
