fn main() {
	if rustversion::cfg!(nightly) {
		println!("cargo::rustc-cfg=feature=\"nightly\"");
	}
}
