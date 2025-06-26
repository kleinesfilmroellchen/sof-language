use std::collections::VecDeque;

use gc_arena::Arena;
use gc_arena::Gc;
use gc_arena::lock::GcRefLock;
use gc_arena::lock::RefLock;
use miette::SourceSpan;

use crate::error::Error;
use crate::parser::Command;
use crate::parser::InnerToken;
use crate::parser::Token;
use crate::runtime::CodeBlock;
use crate::runtime::Stack;
use crate::runtime::StackArena;
use crate::runtime::Stackable;

pub fn run(tokens: Vec<Token>) -> Result<(), Error> {
    let mut arena: StackArena = new_arena();
    run_on_arena(&mut arena, tokens)
}

pub fn new_arena() -> StackArena {
    Arena::new(|mc| {
        let stack = Gc::new(mc, RefLock::new(VecDeque::with_capacity(64)));
        Stack(stack)
    })
}

pub fn run_on_arena(arena: &mut StackArena, tokens: Vec<Token>) -> Result<(), Error> {
    let token_iter = tokens.into_iter();
    for token in token_iter {
        execute_token(token, arena)?;
        // arena.mutate_root(|_, stack| {
        //     let stack = stack.0.borrow();
        //     println!("stack state: {stack:#?}");
        // });
        arena.collect_debt();
    }
    Ok(())
}

fn execute_token(token: Token, arena: &mut StackArena) -> Result<(), Error> {
    match token.inner {
        InnerToken::Literal(literal) => arena.mutate_root(|mc, stack| {
            let mut mut_stack = stack.0.borrow_mut(mc);
            mut_stack.push_back(literal.as_stackable());
            Ok(())
        }),
        InnerToken::CodeBlock(tokens) => arena.mutate_root(|mc, stack| {
            let mut mut_stack = stack.0.borrow_mut(mc);
            mut_stack.push_back(Stackable::CodeBlock(GcRefLock::new(
                mc,
                CodeBlock { code: tokens }.into(),
            )));
            Ok(())
        }),
        InnerToken::Command(Command::Plus) => arena.mutate_root(|mc, stack| {
            let mut mut_stack = stack.0.borrow_mut(mc);
            let rhs = pop_stack(&mut mut_stack, token.span)?;
            let lhs = pop_stack(&mut mut_stack, token.span)?;
            let result = lhs.add(rhs, token.span)?;
            mut_stack.push_back(result);
            Ok(())
        }),
        InnerToken::Command(Command::Minus) => arena.mutate_root(|mc, stack| {
            let mut mut_stack = stack.0.borrow_mut(mc);
            let rhs = pop_stack(&mut mut_stack, token.span)?;
            let lhs = pop_stack(&mut mut_stack, token.span)?;
            let result = lhs.subtract(rhs, token.span)?;
            mut_stack.push_back(result);
            Ok(())
        }),
        InnerToken::Command(Command::Multiply) => arena.mutate_root(|mc, stack| {
            let mut mut_stack = stack.0.borrow_mut(mc);
            let rhs = pop_stack(&mut mut_stack, token.span)?;
            let lhs = pop_stack(&mut mut_stack, token.span)?;
            let result = lhs.multiply(rhs, token.span)?;
            mut_stack.push_back(result);
            Ok(())
        }),
        InnerToken::Command(Command::Divide) => arena.mutate_root(|mc, stack| {
            let mut mut_stack = stack.0.borrow_mut(mc);
            let rhs = pop_stack(&mut mut_stack, token.span)?;
            let lhs = pop_stack(&mut mut_stack, token.span)?;
            let result = lhs.divide(rhs, token.span)?;
            mut_stack.push_back(result);
            Ok(())
        }),
        InnerToken::Command(Command::Modulus) => arena.mutate_root(|mc, stack| {
            let mut mut_stack = stack.0.borrow_mut(mc);
            let rhs = pop_stack(&mut mut_stack, token.span)?;
            let lhs = pop_stack(&mut mut_stack, token.span)?;
            let result = lhs.modulus(rhs, token.span)?;
            mut_stack.push_back(result);
            Ok(())
        }),
        InnerToken::Command(Command::LeftShift) => arena.mutate_root(|mc, stack| {
            let mut mut_stack = stack.0.borrow_mut(mc);
            let rhs = pop_stack(&mut mut_stack, token.span)?;
            let lhs = pop_stack(&mut mut_stack, token.span)?;
            let result = lhs.shift_left(rhs, token.span)?;
            mut_stack.push_back(result);
            Ok(())
        }),
        InnerToken::Command(Command::RightShift) => arena.mutate_root(|mc, stack| {
            let mut mut_stack = stack.0.borrow_mut(mc);
            let rhs = pop_stack(&mut mut_stack, token.span)?;
            let lhs = pop_stack(&mut mut_stack, token.span)?;
            let result = lhs.shift_right(rhs, token.span)?;
            mut_stack.push_back(result);
            Ok(())
        }),
        InnerToken::Command(Command::Equal) => arena.mutate_root(|mc, stack| {
            let mut mut_stack = stack.0.borrow_mut(mc);
            let rhs = pop_stack(&mut mut_stack, token.span)?;
            let lhs = pop_stack(&mut mut_stack, token.span)?;
            let result = Stackable::Boolean(lhs.eq(&rhs));
            mut_stack.push_back(result);
            Ok(())
        }),
        InnerToken::Command(Command::NotEqual) => arena.mutate_root(|mc, stack| {
            let mut mut_stack = stack.0.borrow_mut(mc);
            let rhs = pop_stack(&mut mut_stack, token.span)?;
            let lhs = pop_stack(&mut mut_stack, token.span)?;
            let result = Stackable::Boolean(lhs.ne(&rhs));
            mut_stack.push_back(result);
            Ok(())
        }),
        InnerToken::Command(Command::Not) => arena.mutate_root(|mc, stack| {
            let mut mut_stack = stack.0.borrow_mut(mc);
            let value = pop_stack(&mut mut_stack, token.span)?;
            let result = value.negate(token.span)?;
            mut_stack.push_back(result);
            Ok(())
        }),
        InnerToken::Command(Command::And) => arena.mutate_root(|mc, stack| {
            let mut mut_stack = stack.0.borrow_mut(mc);
            let rhs = pop_stack(&mut mut_stack, token.span)?;
            let lhs = pop_stack(&mut mut_stack, token.span)?;
            let result = lhs.and(rhs, token.span)?;
            mut_stack.push_back(result);
            Ok(())
        }),
        InnerToken::Command(Command::Or) => arena.mutate_root(|mc, stack| {
            let mut mut_stack = stack.0.borrow_mut(mc);
            let rhs = pop_stack(&mut mut_stack, token.span)?;
            let lhs = pop_stack(&mut mut_stack, token.span)?;
            let result = lhs.or(rhs, token.span)?;
            mut_stack.push_back(result);
            Ok(())
        }),
        InnerToken::Command(Command::Xor) => arena.mutate_root(|mc, stack| {
            let mut mut_stack = stack.0.borrow_mut(mc);
            let rhs = pop_stack(&mut mut_stack, token.span)?;
            let lhs = pop_stack(&mut mut_stack, token.span)?;
            let result = lhs.xor(rhs, token.span)?;
            mut_stack.push_back(result);
            Ok(())
        }),
        InnerToken::Command(Command::Assert) => arena.mutate_root(|mc, stack| {
            let mut mut_stack = stack.0.borrow_mut(mc);
            let value = pop_stack(&mut mut_stack, token.span)?;
            if matches!(value, Stackable::Boolean(false)) {
                Err(Error::AssertionFailed { span: token.span })
            } else {
                Ok(())
            }
        }),
        _ => todo!(),
    }
}

fn pop_stack<'a>(
    stack: &mut VecDeque<Stackable<'a>>,
    span: SourceSpan,
) -> Result<Stackable<'a>, Error> {
    let value = stack
        .pop_back()
        .ok_or_else(|| Error::MissingValue { span })?;
    if matches!(value, Stackable::Nametable(_)) {
        stack.push_back(value);
        Err(Error::MissingValue { span })
    } else {
        Ok(value)
    }
}
