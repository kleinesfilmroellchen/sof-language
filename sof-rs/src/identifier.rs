use gc_arena_derive::Collect;
use lean_string::LeanString;

#[derive(Clone, Eq, PartialEq, Hash, Collect)]
#[collect(require_static)]
#[repr(transparent)]
pub struct Identifier(LeanString);

impl Identifier {
	pub fn new(ident: &str) -> Self {
		Self(LeanString::from(ident))
	}

	pub const fn new_static(ident: &'static str) -> Self {
		Self(LeanString::from_static_str(ident))
	}
}

impl std::ops::Deref for Identifier {
	type Target = LeanString;

	fn deref(&self) -> &Self::Target {
		&self.0
	}
}

impl std::fmt::Display for Identifier {
	fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
		self.0.fmt(f)
	}
}

impl std::fmt::Debug for Identifier {
	fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
		write!(f, "id({})", self.0)
	}
}
