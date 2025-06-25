use std::rc::Rc;

use gc_arena::Mutation;
use miette::SourceSpan;

use crate::ErrorKind;
use crate::lexer;
use crate::lexer::Identifier;
use crate::runtime::Stackable;

#[derive(Debug)]
pub enum InnerToken {
    Command(Command),
    Literal(Literal),
    CodeBlock(Vec<Token>),
}

#[derive(Debug)]
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

#[derive(Debug)]
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

#[derive(Debug)]
pub struct Token {
    pub inner: InnerToken,
    pub span: SourceSpan,
}

pub fn parse(tokens: Vec<&lexer::Token>) -> Result<Vec<Token>, ErrorKind> {
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
                while let Some(
                    full_next_token @ lexer::Token {
                        token: next_inner_token,
                        span: next_span,
                    },
                ) = token_iter.next()
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
                    return Err(ErrorKind::UnclosedCodeBlock {
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
                return Err(ErrorKind::UnclosedCodeBlock {
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
