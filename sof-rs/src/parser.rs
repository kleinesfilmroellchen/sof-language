use std::cmp::Ordering;
use std::fmt::Debug;
use std::fmt::Display;
use std::rc::Rc;

use miette::SourceSpan;

use crate::error::Error;
use crate::lexer;
use crate::lexer::Identifier;
use crate::runtime::Stackable;

#[derive(Debug, PartialEq, Clone)]
pub enum InnerToken {
    Command(Command),
    Literal(Literal),
    CodeBlock(Vec<Token>),
    /// Special token only used internally to support while loops.
    WhileBody,
}

#[derive(Debug, PartialEq, Clone)]
pub enum Literal {
    Integer(i64),
    Decimal(f64),
    Identifier(Identifier),
    String(String),
    Boolean(bool),
    ListStart,
}

impl Literal {
    pub fn as_stackable<'gc>(&self) -> Stackable<'gc> {
        match self {
            Literal::Integer(int) => Stackable::Integer(*int),
            Literal::Decimal(decimal) => Stackable::Decimal(*decimal),
            Literal::Identifier(identifier) => Stackable::Identifier(identifier.clone()),
            Literal::String(string) => Stackable::String(Rc::new(string.clone())),
            Literal::Boolean(boolean) => Stackable::Boolean(*boolean),
            Literal::ListStart => Stackable::ListStart,
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
    DoubleCall,
    Curry,
    FieldCall,
    MethodCall,
    NativeCall,
    CreateList,
    Describe,
    DescribeS,
    Assert,
}

impl Display for Command {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(
            f,
            "{}",
            match self {
                Self::Def => "def",
                Self::Globaldef => "globaldef",
                Self::Dexport => "dexport",
                Self::Use => "use",
                Self::Export => "export",
                Self::Dup => "dup",
                Self::Pop => "pop",
                Self::Swap => "swap",
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
                Self::DoubleCall => ":",
                Self::Curry => "|",
                Self::FieldCall => ",",
                Self::MethodCall => ";",
                Self::NativeCall => "nativecall",
                Self::CreateList => "]",
                Self::Describe => "describe",
                Self::DescribeS => "describes",
                Self::Assert => "assert",
            }
        )
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
            lexer::Keyword::DoubleCall => Self::DoubleCall,
            lexer::Keyword::Curry => Self::Curry,
            lexer::Keyword::FieldCall => Self::FieldCall,
            lexer::Keyword::MethodCall => Self::MethodCall,
            lexer::Keyword::NativeCall => Self::NativeCall,
            lexer::Keyword::ListEnd => Self::CreateList,
            lexer::Keyword::Describe => Self::Describe,
            lexer::Keyword::DescribeS => Self::DescribeS,
            lexer::Keyword::Assert => Self::Assert,
            _ => unreachable!(),
        }
    }
}

impl PartialEq<Ordering> for Command {
    fn eq(&self, other: &Ordering) -> bool {
        matches!(
            (self, other),
            (Command::LessEqual, Ordering::Less)
                | (Command::LessEqual, Ordering::Equal)
                | (Command::Greater, Ordering::Greater)
                | (Command::GreaterEqual, Ordering::Equal)
                | (Command::GreaterEqual, Ordering::Greater)
                | (Command::Equal, Ordering::Equal)
                | (Command::NotEqual, Ordering::Less)
                | (Command::NotEqual, Ordering::Greater)
                | (Command::Less, Ordering::Less)
        )
    }
}

#[derive(Clone)]
pub struct Token {
    pub inner: InnerToken,
    pub span: SourceSpan,
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
            InnerToken::Literal(arg0) => f.debug_tuple("Literal").field(arg0).finish(),
            InnerToken::CodeBlock(arg0) => f.debug_tuple("CodeBlock").field(arg0).finish(),
            InnerToken::WhileBody => f.debug_tuple("WhileBody").finish(),
        }?;
        write!(f, ", {:?} }}", (self.span.offset(), self.span.len()))
    }
}

pub fn parse(tokens: Vec<&lexer::Token>) -> Result<Vec<Token>, Error> {
    let mut token_iter = tokens.into_iter();
    let mut output = Vec::new();
    while let Some(lexer::Token { token, span }) = token_iter.next() {
        match token {
            lexer::RawToken::Keyword(lexer::Keyword::ListStart) => output.push(Token {
                inner: InnerToken::Literal(Literal::ListStart),
                span: *span,
            }),
            lexer::RawToken::Decimal(decimal) => output.push(Token {
                inner: InnerToken::Literal(Literal::Decimal(*decimal)),
                span: *span,
            }),
            lexer::RawToken::Integer(int) => output.push(Token {
                inner: InnerToken::Literal(Literal::Integer(*int)),
                span: *span,
            }),
            lexer::RawToken::String(string) => output.push(Token {
                inner: InnerToken::Literal(Literal::String(string.clone())),
                span: *span,
            }),
            lexer::RawToken::Boolean(boolean) => output.push(Token {
                inner: InnerToken::Literal(Literal::Boolean(*boolean)),
                span: *span,
            }),
            lexer::RawToken::Identifier(identifier) => output.push(Token {
                inner: InnerToken::Literal(Literal::Identifier(identifier.clone())),
                span: *span,
            }),
            lexer::RawToken::Keyword(lexer::Keyword::CodeBlockStart) => {
                let mut depth = 1usize;
                let mut inner_tokens = Vec::new();
                let mut last_span = *span;
                for full_next_token @ lexer::Token {
                    token: next_inner_token,
                    span: next_span,
                } in token_iter.by_ref()
                {
                    last_span = *next_span;
                    match next_inner_token {
                        lexer::RawToken::Keyword(lexer::Keyword::CodeBlockStart) => {
                            depth += 1;
                        }
                        lexer::RawToken::Keyword(lexer::Keyword::CodeBlockEnd) => {
                            depth -= 1;
                        }
                        _ => {}
                    }
                    if depth == 0 {
                        break;
                    }
                    inner_tokens.push(full_next_token);
                }
                if depth > 0 {
                    return Err(Error::UnclosedCodeBlock {
                        start_span: *span,
                        end_span: Some(last_span),
                    });
                }

                let span = SourceSpan::new(
                    span.offset().into(),
                    last_span.offset() - span.offset() + last_span.len(),
                );
                let parsed = parse(inner_tokens)?;
                output.push(Token {
                    inner: InnerToken::CodeBlock(parsed),
                    span,
                });
            }
            lexer::RawToken::Keyword(lexer::Keyword::CodeBlockEnd) => {
                return Err(Error::UnclosedCodeBlock {
                    start_span: *span,
                    end_span: None,
                });
            }
            lexer::RawToken::Keyword(keyword) => output.push(Token {
                inner: InnerToken::Command(Command::from_keyword_checked(*keyword)),
                span: *span,
            }),
        }
    }
    Ok(output)
}
