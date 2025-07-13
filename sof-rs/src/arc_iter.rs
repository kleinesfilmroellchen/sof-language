//! Generic iterator over `Arc<Vec<T>>` that allows usage of [`Iterator`] (with cloning of inner values) without having
//! to re-allocate the reference-counted vector (which is what happens if you just call `.into_iter` on an
//! `Arc<Vec<T>>`).

use std::marker::PhantomData;
use std::ptr::NonNull;
use std::sync::Arc;
pub(crate) struct ArcVecIter<'a, T> {
	/// mainly used so that we keep alive a reference to the vector and the below pointers stay valid for our lifetime
	vec:  Arc<Vec<T>>,
	/// emulates borrowing from the Vec, such that the lifetime makes sense and the borrow checker is happy
	data: PhantomData<&'a T>,

	// Stolen from Rust standard library Vec’s into_iter.
	ptr: NonNull<T>,
	end: *const T,
}

impl<T> ArcVecIter<'_, T> {
	pub fn new(vec: Arc<Vec<T>>) -> Self {
		let range = vec.as_ptr_range();
		// SAFETY: as_ptr_range() guarantees pointer is non-null (but possibly dangling).
		//         We guarantee to never write to the NonNull, as we only copy out of it.
		let buf = unsafe { NonNull::new_unchecked(range.start.cast_mut()) };
		let begin = buf.as_ptr().cast_const();
		let end = if size_of::<T>() == 0 { begin.wrapping_byte_add(vec.len()) } else { range.end };
		Self { vec, ptr: buf, end, data: PhantomData }
	}

	/// Straightforward `next()` implementation that’s a bit slow.
	#[allow(unused)]
	fn next_simple(&mut self) -> Option<&T> {
		let mut index = 0; // iteration index
		if index >= self.vec.len() {
			return None;
		}
		let value = &self.vec[index];
		index += 1;
		Some(value)
	}
}

impl<'a, T> Iterator for ArcVecIter<'a, T> {
	type Item = &'a T;

	fn next(&mut self) -> Option<Self::Item> {
		// Mostly stolen from Rust standard library Vec’s into_iter.
		let ptr = if size_of::<T>() == 0 {
			if std::ptr::eq(self.ptr.as_ptr(), self.end) {
				return None;
			}
			// `ptr` has to stay where it is to remain aligned, so we reduce the length by 1 by
			// reducing the `end`.
			self.end = self.end.wrapping_byte_sub(1);
			self.ptr
		} else {
			// SAFETY: self.end is guaranteed to be dangling or valid. We never read from the pointer returned.
			if self.ptr == unsafe { NonNull::new_unchecked(self.end.cast_mut()) } {
				return None;
			}
			let old = self.ptr;
			// SAFETY: We check this pointer before next use in the `if` above.
			self.ptr = unsafe { old.add(1) };
			old
		};
		// SAFETY: If T is a ZST, any pointer read is okay.
		//         Otherwise, we checked above that the pointer is in range.
		Some(unsafe { ptr.as_ref() })
	}
}
