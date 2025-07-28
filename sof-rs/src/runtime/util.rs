use gc_arena::Collect;
use smallvec::SmallVec;

use super::Stackable;

#[derive(Collect, Debug, Clone)]
#[collect(no_drop)]
pub struct SwitchCase<'gc> {
	pub(super) conditional: Stackable<'gc>,
	pub(super) body:        Stackable<'gc>,
}

#[derive(Debug, Clone)]
pub struct SwitchCases<'gc>(pub(super) SmallVec<[SwitchCase<'gc>; 4]>);

unsafe impl Collect for SwitchCases<'_> {
	fn needs_trace() -> bool
	where
		Self: Sized,
	{
		true
	}

	fn trace(&self, cc: &gc_arena::Collection) {
		self.0.trace(cc);
	}
}
#[derive(Collect, Debug, Clone)]
#[collect(no_drop)]
#[allow(clippy::large_enum_variant)]
pub enum UtilityData<'gc> {
	While {
		body:                 Stackable<'gc>,
		conditional_callable: Stackable<'gc>,
		conditional_result:   bool,
	},
	Switch {
		/// cases that have been not touched so far
		remaining_cases: SwitchCases<'gc>,
		default_case:    Stackable<'gc>,
		/// body of case that will be run if the conditional succeeds
		next_body:       Stackable<'gc>,
	},
}
