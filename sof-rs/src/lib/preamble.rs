use crate::error::Error;
use crate::runtime::Stackable;

pub fn to_integer<'gc>(value: Stackable<'gc>) -> Result<Option<Stackable<'gc>>, Error> {
	Ok(Some(Stackable::Integer(match value {
		Stackable::Integer(i) => i,
		Stackable::Decimal(d) => d.round() as i64,
		Stackable::Boolean(b) => b as i64,
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

pub fn to_float<'gc>(value: Stackable<'gc>) -> Result<Option<Stackable<'gc>>, Error> {
	Ok(Some(Stackable::Decimal(match value {
		Stackable::Integer(i) => i as f64,
		Stackable::Decimal(d) => d,
		Stackable::Boolean(b) => b as i64 as f64,
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

pub fn to_string<'gc>(value: Stackable<'gc>) -> Result<Option<Stackable<'gc>>, Error> {
	Ok(Some(Stackable::String(value.to_string().into())))
}
