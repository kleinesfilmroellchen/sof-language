use std::fmt::Display;
use std::iter::Peekable;
use std::ops::Deref;
use std::rc::Rc;

use gc_arena_derive::Collect;
use miette::SourceOffset;
use miette::SourceSpan;
use unicode_ident::is_xid_continue;
use unicode_ident::is_xid_start;

use crate::ErrorKind;

#[derive(Debug, Clone)]
pub enum RawToken {
    Keyword(Keyword),
    Decimal(f64),
    Integer(i64),
    String(String),
    Boolean(bool),
    Identifier(Identifier),
}

#[derive(Debug, Clone, Copy, Eq, PartialEq)]
pub enum Keyword {
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
    ListStart,
    ListEnd,
    Describe,
    DescribeS,
    Assert,
    CodeBlockStart,
    CodeBlockEnd,
}

impl Keyword {
    pub fn checked_from_char(chr: char) -> Self {
        match chr {
            '+' => Self::Plus,
            '-' => Self::Minus,
            '/' => Self::Divide,
            '*' => Self::Multiply,
            '%' => Self::Modulus,
            '=' => Self::Equal,
            '>' => Self::Greater,
            '<' => Self::Less,
            '[' => Self::ListStart,
            '{' => Self::CodeBlockStart,
            '}' => Self::CodeBlockEnd,
            ']' => Self::ListEnd,
            '|' => Self::Curry,
            '.' => Self::Call,
            ':' => Self::DoubleCall,
            ',' => Self::FieldCall,
            ';' => Self::MethodCall,
            _ => unreachable!(),
        }
    }

    pub fn checked_from_chars(first: char, second: char) -> Self {
        match (first, second) {
            ('>', '>') => Self::RightShift,
            ('<', '<') => Self::LeftShift,
            ('>', '=') => Self::GreaterEqual,
            ('<', '=') => Self::LessEqual,
            ('/', '=') => Self::NotEqual,
            _ => unreachable!(),
        }
    }

    pub fn from_identifier_keyword(ident: &str) -> Option<Self> {
        Some(match ident {
            "if" => Self::If,
            "ifelse" => Self::Ifelse,
            "def" => Self::Def,
            "globaldef" => Self::Globaldef,
            "dexport" => Self::Dexport,
            "export" => Self::Export,
            "use" => Self::Use,
            "pop" => Self::Pop,
            "dup" => Self::Dup,
            "swap" => Self::Swap,
            "write" => Self::Write,
            "writeln" => Self::Writeln,
            "input" => Self::Input,
            "inputln" => Self::Inputln,
            "cat" => Self::Cat,
            "and" => Self::And,
            "or" => Self::Or,
            "xor" => Self::Xor,
            "not" => Self::Not,
            "nativecall" => Self::NativeCall,
            "describe" => Self::Describe,
            "describes" => Self::DescribeS,
            "assert" => Self::Assert,
            "while" => Self::While,
            "dowhile" => Self::DoWhile,
            "switch" => Self::Switch,
            "function" => Self::Function,
            "constructor" => Self::Constructor,
            _ => return None,
        })
    }
}

#[derive(Debug)]
pub struct Token {
    pub token: RawToken,
    pub span: SourceSpan,
}

#[derive(Debug, Clone, Eq, PartialEq, Hash, Collect)]
#[collect(require_static)]
#[repr(transparent)]
pub struct Identifier(Rc<String>);

impl Deref for Identifier {
    type Target = String;

    fn deref(&self) -> &Self::Target {
        &self.0
    }
}

impl Display for Identifier {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        self.0.fmt(f)
    }
}

pub fn lex(string: impl AsRef<str>) -> Result<Vec<Token>, ErrorKind> {
    let mut char_iter = string.as_ref().chars().enumerate().peekable();
    let mut tokens = Vec::new();
    while let Some((next_position, next_char)) = char_iter.next() {
        let next_offset = next_position.into();
        match next_char {
            '#' => {
                // comment
                let multiline = char_iter.peek().is_some_and(|(_, c)| *c == '*');
                if multiline {
                    char_iter.next();
                }
                while let Some((_, comment_char)) = char_iter.next() {
                    match comment_char {
                        '\n' if !multiline => break,
                        '*' if char_iter.peek().is_some_and(|(_, c)| *c == '#') => {
                            char_iter.next();
                            break;
                        }
                        _ => {}
                    }
                }
            }
            c if is_xid_start(c) => {
                let mut ident = String::new();
                ident.push(c);
                while let Some((_, idc)) = char_iter.next() {
                    if is_xid_continue(idc) || [':', '\''].contains(&idc) {
                        ident.push(idc);
                    } else if idc.is_whitespace() {
                        break;
                    } else {
                        return Err(ErrorKind::InvalidIdentifier {
                            chr: idc,
                            span: (SourceOffset::from(next_position + ident.chars().count()), 1)
                                .into(),
                            ident,
                        });
                    }
                }

                tokens.push(Token {
                    span: (next_offset, ident.chars().count()).into(),
                    token: if ident.to_ascii_lowercase() == "true" {
                        RawToken::Boolean(true)
                    } else if ident.to_ascii_lowercase() == "false" {
                        RawToken::Boolean(false)
                    } else {
                        Keyword::from_identifier_keyword(&ident).map_or_else(
                            || RawToken::Identifier(Identifier(Rc::new(ident))),
                            |kw| RawToken::Keyword(kw),
                        )
                    },
                });
            }
            c if c.is_whitespace() => continue,
            '+' | '-' | '*' | '/' | '%' | '=' | '>' | '<' | '.' | ',' | ';' | ':' | '[' | ']'
            | '|' | '{' | '}'
                if char_iter.peek().is_none_or(|(_, c)| c.is_whitespace()) =>
            {
                tokens.push(Token {
                    span: SourceSpan::new(next_offset, 1),
                    token: RawToken::Keyword(Keyword::checked_from_char(next_char)),
                });
            }
            '>' if matches!(char_iter.peek(), Some((_, '>' | '='))) => {
                tokens.push(Token {
                    span: SourceSpan::new(next_offset, 2),
                    token: RawToken::Keyword(Keyword::checked_from_chars(
                        next_char,
                        char_iter.next().unwrap().1,
                    )),
                });
            }
            '<' if matches!(char_iter.peek(), Some((_, '<' | '='))) => {
                tokens.push(Token {
                    span: SourceSpan::new(next_offset, 2),
                    token: RawToken::Keyword(Keyword::checked_from_chars(
                        next_char,
                        char_iter.next().unwrap().1,
                    )),
                });
            }
            '/' if matches!(char_iter.peek(), Some((_, '='))) => {
                tokens.push(Token {
                    span: SourceSpan::new(next_offset, 2),
                    token: RawToken::Keyword(Keyword::NotEqual),
                });
            }
            '"' => {
                let mut string = String::new();
                while let Some((pos, string_char)) = char_iter.next() {
                    match string_char {
                        '"' => break,
                        '\\' => {
                            let (_, escaped) =
                                char_iter.next().ok_or(ErrorKind::UnclosedString {
                                    span: SourceSpan::new(pos.into(), 0),
                                })?;
                            string.push(escaped);
                        }
                        _ => string.push(string_char),
                    }
                }
                tokens.push(Token {
                    span: SourceSpan::new(next_offset, string.chars().count()),
                    token: RawToken::String(string),
                })
            }
            '0'..='9' | '+' | '-' => {
                let (token, length) = parse_number(next_offset, next_char, &mut char_iter)?;
                tokens.push(Token {
                    span: SourceSpan::new(next_offset, length),
                    token,
                })
            }
            _ => {
                return Err(ErrorKind::InvalidCharacter {
                    chr: next_char,
                    span: SourceSpan::new(next_offset, 1),
                });
            }
        }
    }
    Ok(tokens)
}

fn parse_number(
    start_offset: SourceOffset,
    first_char: char,
    char_iter: &mut Peekable<impl Iterator<Item = (usize, char)>>,
) -> Result<(RawToken, usize), ErrorKind> {
    let mut number_string = String::new();
    number_string.push(first_char);
    while char_iter.peek().is_some_and(|(_, c)| !c.is_whitespace()) {
        number_string.push(char_iter.next().unwrap().1);
    }
    let string_length = number_string.chars().count();

    if let Some((prefix @ ("" | "-" | "+"), hex_number)) = number_string.split_once("0x") {
        // starts with "0x" and optional sign – hex digit
        parse_with_radix::<16>(
            prefix,
            hex_number,
            &number_string,
            start_offset,
            string_length,
        )
    } else if let Some((prefix @ ("" | "-" | "+"), hex_number)) = number_string.split_once("0b") {
        // starts with "0b" and optional sign – binary digit
        parse_with_radix::<2>(
            prefix,
            hex_number,
            &number_string,
            start_offset,
            string_length,
        )
    } else if let Some((prefix @ ("" | "-" | "+"), hex_number)) = number_string.split_once("0o") {
        // starts with "0o" and optional sign – octal digit
        parse_with_radix::<8>(
            prefix,
            hex_number,
            &number_string,
            start_offset,
            string_length,
        )
    } else {
        // some kind of decimal digit, possibly floating-point

        // all digits, so an integer
        if number_string
            .chars()
            .enumerate()
            .all(|(i, c)| c.is_ascii_digit() || (i == 0 && ['+', '-'].contains(&c)))
        {
            parse_with_radix::<10>(
                "",
                &number_string,
                &number_string,
                start_offset,
                string_length,
            )
        } else {
            // probably a float
            Ok((
                RawToken::Decimal(number_string.parse().map_err(|inner| {
                    ErrorKind::InvalidFloat {
                        number_text: number_string,
                        inner,
                        span: (start_offset, string_length).into(),
                    }
                })?),
                string_length,
            ))
        }
    }
}

fn parse_with_radix<const RADIX: u32>(
    prefix: &str,
    number: &str,
    number_string: &String,
    start_offset: SourceOffset,
    string_length: usize,
) -> Result<(RawToken, usize), ErrorKind> {
    let mut number =
        i64::from_str_radix(number, RADIX).map_err(|inner| ErrorKind::InvalidInteger {
            span: (start_offset, string_length).into(),
            number_text: number_string.clone(),
            inner,
        })?;
    if prefix == "-" {
        number *= -1;
    }
    Ok((RawToken::Integer(number), string_length))
}
