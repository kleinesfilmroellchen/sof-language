use std::collections::VecDeque;

use gc_arena::Arena;
use gc_arena::Gc;
use gc_arena::lock::GcRefLock;
use gc_arena::lock::RefLock;

use crate::ErrorKind;
use crate::parser::Command;
use crate::parser::InnerToken;
use crate::parser::Token;
use crate::runtime::CodeBlock;
use crate::runtime::Stack;
use crate::runtime::StackArena;
use crate::runtime::Stackable;

pub fn run(tokens: Vec<Token>) -> Result<(), ErrorKind> {
    let mut arena: StackArena = Arena::new(|mc| {
        let stack = Gc::new(mc, RefLock::new(VecDeque::with_capacity(64)));
        Stack(stack)
    });

    run_on_arena(&mut arena, tokens)
}

fn run_on_arena(arena: &mut StackArena, tokens: Vec<Token>) -> Result<(), ErrorKind> {
    let token_iter = tokens.into_iter();
    for token in token_iter {
        execute_token(token, arena)?;
        arena.mutate_root(|_, stack| {
            let stack = stack.0.borrow();
            println!("stack state: {stack:#?}");
        })
    }
    Ok(())
}

fn execute_token(token: Token, arena: &mut StackArena) -> Result<(), ErrorKind> {
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
            let rhs = mut_stack.pop_back().expect("proper error");
            let lhs = mut_stack.pop_back().expect("proper error");
            let result = lhs.add(rhs, mc)?;
            mut_stack.push_back(result);
            Ok(())
        }),
        _ => todo!(),
    }
}
