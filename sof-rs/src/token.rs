use std::cmp::Ordering;
use std::fmt::{Debug, Display};

use flexstr::SharedStr;
use gc_arena::{Gc, Mutation};
use miette::SourceSpan;

use crate::identifier::Identifier;
use crate::parser::lexer;
use crate::runtime::stackable::{CodeBlock, Stackable, TokenVec};

#[derive(Debug, PartialEq, Clone)]
pub enum InnerToken {
	Command(Command),
	/// Special token only used internally to support while loops.
	WhileBody,
	/// Special token only used internally to support switch cases.
	SwitchBody,

	// literals
	Integer(i64),
	Decimal(f64),
	Identifier(Identifier),
	String(SharedStr),
	Boolean(bool),
	CodeBlock(TokenVec),
	ListStart,
	Curry,
}

impl InnerToken {
	pub fn as_stackable<'gc>(&self, mc: &Mutation<'gc>) -> Stackable<'gc> {
		match self {
			Self::Integer(int) => Stackable::Integer(*int),
			Self::Decimal(decimal) => Stackable::Decimal(*decimal),
			Self::Identifier(identifier) => Stackable::Identifier(identifier.clone()),
			Self::String(string) => Stackable::String(string.clone()),
			Self::Boolean(boolean) => Stackable::Boolean(*boolean),
			Self::ListStart => Stackable::ListStart,
			Self::Curry => Stackable::Curry,
			Self::CodeBlock(code) => Stackable::CodeBlock(Gc::new(mc, CodeBlock { code: code.clone() })),
			_ => unreachable!(),
		}
	}
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum Command {
	Def,
	Globaldef,
	Dexport,
	Use,
	Export,
	Dup,
	Pop,
	Swap,
	Over,
	Rot,
	Write,
	Writeln,
	Input,
	Inputln,
	If,
	Ifelse,
	While,
	DoWhile,
	Switch,
	Function,
	Constructor,
	Plus,
	Minus,
	Multiply,
	Divide,
	Modulus,
	LeftShift,
	RightShift,
	Cat,
	And,
	Or,
	Xor,
	Not,
	Less,
	LessEqual,
	Greater,
	GreaterEqual,
	Equal,
	NotEqual,
	Call,
	FieldAccess,
	MethodCall,
	NativeCall,
	CreateList,
	Describe,
	DescribeS,
	Assert,
	Return,
	ReturnNothing,
}

impl Display for Command {
	fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
		write!(f, "{}", match self {
			Self::Def => "def",
			Self::Globaldef => "globaldef",
			Self::Dexport => "dexport",
			Self::Use => "use",
			Self::Export => "export",
			Self::Dup => "dup",
			Self::Pop => "pop",
			Self::Swap => "swap",
			Self::Over => "over",
			Self::Rot => "rot",
			Self::Write => "write",
			Self::Writeln => "writeln",
			Self::Input => "input",
			Self::Inputln => "inputln",
			Self::If => "if",
			Self::Ifelse => "ifelse",
			Self::While => "while",
			Self::DoWhile => "dowhile",
			Self::Switch => "switch",
			Self::Function => "function",
			Self::Constructor => "constructor",
			Self::Plus => "+",
			Self::Minus => "-",
			Self::Multiply => "*",
			Self::Divide => "/",
			Self::Modulus => "%",
			Self::LeftShift => "<<",
			Self::RightShift => ">>",
			Self::Cat => "cat",
			Self::And => "and",
			Self::Or => "or",
			Self::Xor => "xor",
			Self::Not => "not",
			Self::Less => "<",
			Self::LessEqual => "<=",
			Self::Greater => ">",
			Self::GreaterEqual => ">=",
			Self::Equal => "=",
			Self::NotEqual => "/=",
			Self::Call => ".",
			Self::FieldAccess => ",",
			Self::MethodCall => ";",
			Self::NativeCall => "nativecall",
			Self::CreateList => "]",
			Self::Describe => "describe",
			Self::DescribeS => "describes",
			Self::Assert => "assert",
			Self::Return => "return",
			Self::ReturnNothing => "return:0",
		})
	}
}

impl Command {
	pub fn from_keyword_checked(keyword: lexer::Keyword) -> Self {
		match keyword {
			lexer::Keyword::Def => Self::Def,
			lexer::Keyword::Globaldef => Self::Globaldef,
			lexer::Keyword::Dexport => Self::Dexport,
			lexer::Keyword::Use => Self::Use,
			lexer::Keyword::Export => Self::Export,
			lexer::Keyword::Dup => Self::Dup,
			lexer::Keyword::Pop => Self::Pop,
			lexer::Keyword::Swap => Self::Swap,
			lexer::Keyword::Over => Self::Over,
			lexer::Keyword::Rot => Self::Rot,
			lexer::Keyword::Write => Self::Write,
			lexer::Keyword::Writeln => Self::Writeln,
			lexer::Keyword::Input => Self::Input,
			lexer::Keyword::Inputln => Self::Inputln,
			lexer::Keyword::If => Self::If,
			lexer::Keyword::Ifelse => Self::Ifelse,
			lexer::Keyword::While => Self::While,
			lexer::Keyword::DoWhile => Self::DoWhile,
			lexer::Keyword::Switch => Self::Switch,
			lexer::Keyword::Function => Self::Function,
			lexer::Keyword::Constructor => Self::Constructor,
			lexer::Keyword::Plus => Self::Plus,
			lexer::Keyword::Minus => Self::Minus,
			lexer::Keyword::Multiply => Self::Multiply,
			lexer::Keyword::Divide => Self::Divide,
			lexer::Keyword::Modulus => Self::Modulus,
			lexer::Keyword::LeftShift => Self::LeftShift,
			lexer::Keyword::RightShift => Self::RightShift,
			lexer::Keyword::Cat => Self::Cat,
			lexer::Keyword::And => Self::And,
			lexer::Keyword::Or => Self::Or,
			lexer::Keyword::Xor => Self::Xor,
			lexer::Keyword::Not => Self::Not,
			lexer::Keyword::Less => Self::Less,
			lexer::Keyword::LessEqual => Self::LessEqual,
			lexer::Keyword::Greater => Self::Greater,
			lexer::Keyword::GreaterEqual => Self::GreaterEqual,
			lexer::Keyword::Equal => Self::Equal,
			lexer::Keyword::NotEqual => Self::NotEqual,
			lexer::Keyword::Call => Self::Call,
			lexer::Keyword::FieldAccess => Self::FieldAccess,
			lexer::Keyword::MethodCall => Self::MethodCall,
			lexer::Keyword::NativeCall => Self::NativeCall,
			lexer::Keyword::ListEnd => Self::CreateList,
			lexer::Keyword::Describe => Self::Describe,
			lexer::Keyword::DescribeS => Self::DescribeS,
			lexer::Keyword::Assert => Self::Assert,
			lexer::Keyword::Return => Self::Return,
			lexer::Keyword::ReturnNothing => Self::ReturnNothing,
			_ => unreachable!(),
		}
	}
}

impl PartialEq<Ordering> for Command {
	fn eq(&self, other: &Ordering) -> bool {
		matches!(
			(self, other),
			(Command::LessEqual | Command::Less, Ordering::Less)
				| (Command::LessEqual | Command::GreaterEqual | Command::Equal, Ordering::Equal)
				| (Command::Greater | Command::GreaterEqual, Ordering::Greater)
		)
	}
}

#[derive(Clone)]
pub struct Token {
	pub inner: InnerToken,
	pub span:  SourceSpan,
}

impl PartialEq for Token {
	fn eq(&self, other: &Self) -> bool {
		self.inner == other.inner
	}
}

impl Debug for Token {
	fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
		f.write_str("Token { ")?;
		match &self.inner {
			InnerToken::Command(arg0) => f.debug_tuple("Command").field(arg0).finish(),
			InnerToken::CodeBlock(arg0) => f.debug_tuple("CodeBlock").field(arg0).finish(),
			InnerToken::Integer(arg0) => f.debug_tuple("Integer").field(arg0).finish(),
			InnerToken::Decimal(arg0) => f.debug_tuple("Decimal").field(arg0).finish(),
			InnerToken::Identifier(arg0) => f.debug_tuple("Identifier").field(arg0).finish(),
			InnerToken::String(arg0) => f.debug_tuple("String").field(arg0).finish(),
			InnerToken::Boolean(arg0) => f.debug_tuple("Boolean").field(arg0).finish(),
			InnerToken::ListStart => f.debug_tuple("ListStart").finish(),
			InnerToken::Curry => f.debug_tuple("Curry").finish(),
			InnerToken::WhileBody => f.debug_tuple("WhileBody").finish(),
			InnerToken::SwitchBody => f.debug_tuple("SwitchBody").finish(),
		}?;
		write!(f, ", {:?} }}", (self.span.offset(), self.span.len()))
	}
}
