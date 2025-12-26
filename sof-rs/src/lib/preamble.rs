#![allow(clippy::unnecessary_wraps, clippy::needless_pass_by_value)]

use std::marker::PhantomData;

use rand::Rng;

use crate::error::Error;
use crate::runtime::Stackable;

pub fn to_integer(value: Stackable<'_>) -> Result<Option<Stackable<'_>>, Error> {
	Ok(Some(Stackable::Integer(match value {
		Stackable::Integer(i) => i,
		#[allow(clippy::cast_possible_truncation)]
		Stackable::Decimal(d) => d.round() as i64,
		Stackable::Boolean(b) => i64::from(b),
		Stackable::String(string) =>
			string.parse().map_err(|inner| Error::InvalidInteger { number_text: string, inner, span: (0, 0).into() })?,
		_ =>
			return Err(Error::InvalidTypeNative {
				name:  "convert:int".into(),
				value: value.to_string().into(),
				span:  (0, 0).into(),
			}),
	})))
}

pub fn to_float(value: Stackable<'_>) -> Result<Option<Stackable<'_>>, Error> {
	Ok(Some(Stackable::Decimal(match value {
		Stackable::Integer(i) => i as f64,
		Stackable::Decimal(d) => d,
		Stackable::Boolean(b) => i64::from(b) as f64,
		Stackable::String(string) =>
			string.parse().map_err(|inner| Error::InvalidFloat { number_text: string, inner, span: (0, 0).into() })?,
		_ =>
			return Err(Error::InvalidTypeNative {
				name:  "convert:int".into(),
				value: value.to_string().into(),
				span:  (0, 0).into(),
			}),
	})))
}

pub fn to_string(value: Stackable<'_>) -> Result<Option<Stackable<'_>>, Error> {
	Ok(Some(Stackable::String(value.to_string().into())))
}

pub fn random_float_01(_: PhantomData<&'_ ()>) -> Result<Option<Stackable<'_>>, Error> {
	let mut rng = rand::rng();
	let val = rng.random_range(0.0 .. 1.0);
	Ok(Some(Stackable::Decimal(val)))
}

pub fn random_float_range<'gc>(start: Stackable<'gc>, end: Stackable<'gc>) -> Result<Option<Stackable<'gc>>, Error> {
	let mut rng = rand::rng();

	let start = match start {
		Stackable::Decimal(d) => d,
		Stackable::Integer(i) => i as f64,
		_ =>
			return Err(Error::InvalidTypeNative {
				name:  "random:decimal".into(),
				value: start.to_string().into(),
				span:  (0, 0).into(),
			}),
	};
	let end = match end {
		Stackable::Decimal(d) => d,
		Stackable::Integer(i) => i as f64,
		_ =>
			return Err(Error::InvalidTypeNative {
				name:  "random:decimal".into(),
				value: end.to_string().into(),
				span:  (0, 0).into(),
			}),
	};

	let val = rng.random_range(start .. end);
	Ok(Some(Stackable::Decimal(val)))
}

pub fn random_int_range<'gc>(start: Stackable<'gc>, end: Stackable<'gc>) -> Result<Option<Stackable<'gc>>, Error> {
	let mut rng = rand::rng();

	let start = match start {
		Stackable::Integer(i) => i,
		_ =>
			return Err(Error::InvalidTypeNative {
				name:  "random:int".into(),
				value: start.to_string().into(),
				span:  (0, 0).into(),
			}),
	};
	let end = match end {
		Stackable::Integer(i) => i,
		_ =>
			return Err(Error::InvalidTypeNative {
				name:  "random:int".into(),
				value: end.to_string().into(),
				span:  (0, 0).into(),
			}),
	};

	let val = rng.random_range(start ..= end);
	Ok(Some(Stackable::Integer(val)))
}
