use flexstr::FlexStrBase;
use gc_arena_derive::Collect;
use internment::ArcIntern;

#[derive(Clone, Eq, PartialEq, Hash, Collect)]
#[collect(require_static)]
#[repr(transparent)]
pub struct Identifier(FlexStrBase<ArcIntern<str>>);

impl Identifier {
	// force the compiler to check the size constraint at compile-time
	const __CHECK_SIZE: usize = if size_of::<ArcIntern<str>>() == size_of::<*const u8>() * 2 {
		0
	} else {
		panic!("ArcIntern has invalid size on this platform, must be exactly two pointers")
	};

	pub fn new(ident: &str) -> Self {
		let _ = Self::__CHECK_SIZE;
		Self(FlexStrBase::from(ident))
	}
}

impl std::ops::Deref for Identifier {
	type Target = FlexStrBase<ArcIntern<str>>;

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
