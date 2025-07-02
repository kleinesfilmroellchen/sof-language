use std::collections::VecDeque;

use gc_arena::Arena;
use gc_arena::Gc;
use gc_arena::Mutation;
use gc_arena::lock::GcRefLock;
use gc_arena::lock::RefLock;
use log::debug;
use log::trace;
use miette::SourceSpan;

use crate::error::Error;
use crate::parser::Command;
use crate::parser::InnerToken;
use crate::parser::Token;
use crate::runtime::CodeBlock;
use crate::runtime::InnerStack;
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

// TODO: tune this
pub const COLLECTION_THRESHOLD: f64 = 0.2;

pub fn run_on_arena(arena: &mut StackArena, tokens: Vec<Token>) -> Result<(), Error> {
    let token_iter = tokens.into_iter();
    // Use a stack of token lists to be executed. At the end of each list is some kind of call return.
    let mut token_execution_stack = VecDeque::new();
    token_execution_stack.push_back((token_iter, CallReturnBehavior::BlockCall));
    let mut exec_count = 0usize;
    while let Some((current_stack, stack_action)) = token_execution_stack.back_mut() {
        let Some(token) = current_stack.next() else {
            match *stack_action {
                CallReturnBehavior::BlockCall => {
                    debug!(
                        "[{exec_count}] returning from block, remaining call stack depth {}",
                        token_execution_stack.len() - 1
                    );
                }
                CallReturnBehavior::FunctionCall => todo!("finalize function call"),
            }
            token_execution_stack.pop_back();
            continue;
        };
        exec_count += 1;
        trace!("[{exec_count}] executing token {token:?}");
        let action = arena.mutate(|mc, stack| execute_token(token.clone(), mc, stack.clone()))?;
        if arena.metrics().allocation_debt() >= COLLECTION_THRESHOLD {
            debug!(
                "[{exec_count}] debt is {} at total allocation of {}, running GC.",
                arena.metrics().allocation_debt(),
                arena.metrics().total_allocation()
            );
            arena.collect_debt();
        }
        match action {
            InterpreterAction::None => {}
            InterpreterAction::ExecuteCall {
                code,
                return_behavior,
            } => {
                debug!(
                    "[{exec_count}] entering call of type {return_behavior:?} and {} tokens, call stack depth {}",
                    code.len(),
                    token_execution_stack.len() + 1
                );
                token_execution_stack.push_back((code.into_iter(), return_behavior));
            }
        }
    }
    Ok(())
}

/// Signals to the interpreter loop which action to take.
#[derive(Clone, Debug, Default)]
pub(crate) enum InterpreterAction {
    /// Take no further action and execute the next token in the current list.
    #[default]
    None,
    /// Execute the given list of tokens before further tokens in the current list.
    /// Then follow the return behavior given.
    ExecuteCall {
        code: Vec<Token>,
        return_behavior: CallReturnBehavior,
    },
}

impl From<()> for InterpreterAction {
    fn from(_: ()) -> Self {
        Self::None
    }
}

/// Different possible behaviors after a call return, each corresponding to a kind of call that SOF can do.
#[derive(Clone, Copy, Debug, Default)]
pub(crate) enum CallReturnBehavior {
    /// Don’t do anything.
    #[default]
    BlockCall,
    /// Pop nametable and push return value if available.
    FunctionCall,
}

fn execute_token<'a>(
    token: Token,
    mc: &Mutation<'a>,
    stack: Stack<'a>,
) -> Result<InterpreterAction, Error> {
    match token.inner {
        InnerToken::Literal(literal) => {
            let mut mut_stack = stack.0.borrow_mut(mc);
            mut_stack.push_back(literal.as_stackable());
            Ok(().into())
        }
        InnerToken::CodeBlock(tokens) => {
            let mut mut_stack = stack.0.borrow_mut(mc);
            mut_stack.push_back(Stackable::CodeBlock(GcRefLock::new(
                mc,
                CodeBlock { code: tokens }.into(),
            )));
            Ok(().into())
        }
        InnerToken::Command(Command::Plus) => {
            let mut mut_stack = stack.0.borrow_mut(mc);
            let rhs = pop_stack(&mut mut_stack, token.span)?;
            let lhs = pop_stack(&mut mut_stack, token.span)?;
            let result = lhs.add(rhs, token.span)?;
            mut_stack.push_back(result);
            Ok(().into())
        }
        InnerToken::Command(Command::Minus) => {
            let mut mut_stack = stack.0.borrow_mut(mc);
            let rhs = pop_stack(&mut mut_stack, token.span)?;
            let lhs = pop_stack(&mut mut_stack, token.span)?;
            let result = lhs.subtract(rhs, token.span)?;
            mut_stack.push_back(result);
            Ok(().into())
        }
        InnerToken::Command(Command::Multiply) => {
            let mut mut_stack = stack.0.borrow_mut(mc);
            let rhs = pop_stack(&mut mut_stack, token.span)?;
            let lhs = pop_stack(&mut mut_stack, token.span)?;
            let result = lhs.multiply(rhs, token.span)?;
            mut_stack.push_back(result);
            Ok(().into())
        }
        InnerToken::Command(Command::Divide) => {
            let mut mut_stack = stack.0.borrow_mut(mc);
            let rhs = pop_stack(&mut mut_stack, token.span)?;
            let lhs = pop_stack(&mut mut_stack, token.span)?;
            let result = lhs.divide(rhs, token.span)?;
            mut_stack.push_back(result);
            Ok(().into())
        }
        InnerToken::Command(Command::Modulus) => {
            let mut mut_stack = stack.0.borrow_mut(mc);
            let rhs = pop_stack(&mut mut_stack, token.span)?;
            let lhs = pop_stack(&mut mut_stack, token.span)?;
            let result = lhs.modulus(rhs, token.span)?;
            mut_stack.push_back(result);
            Ok(().into())
        }
        InnerToken::Command(Command::LeftShift) => {
            let mut mut_stack = stack.0.borrow_mut(mc);
            let rhs = pop_stack(&mut mut_stack, token.span)?;
            let lhs = pop_stack(&mut mut_stack, token.span)?;
            let result = lhs.shift_left(rhs, token.span)?;
            mut_stack.push_back(result);
            Ok(().into())
        }
        InnerToken::Command(Command::RightShift) => {
            let mut mut_stack = stack.0.borrow_mut(mc);
            let rhs = pop_stack(&mut mut_stack, token.span)?;
            let lhs = pop_stack(&mut mut_stack, token.span)?;
            let result = lhs.shift_right(rhs, token.span)?;
            mut_stack.push_back(result);
            Ok(().into())
        }
        InnerToken::Command(Command::Equal) => {
            let mut mut_stack = stack.0.borrow_mut(mc);
            let rhs = pop_stack(&mut mut_stack, token.span)?;
            let lhs = pop_stack(&mut mut_stack, token.span)?;
            let result = Stackable::Boolean(lhs.eq(&rhs));
            mut_stack.push_back(result);
            Ok(().into())
        }
        InnerToken::Command(Command::NotEqual) => {
            let mut mut_stack = stack.0.borrow_mut(mc);
            let rhs = pop_stack(&mut mut_stack, token.span)?;
            let lhs = pop_stack(&mut mut_stack, token.span)?;
            let result = Stackable::Boolean(lhs.ne(&rhs));
            mut_stack.push_back(result);
            Ok(().into())
        }
        InnerToken::Command(
            command @ (Command::Greater
            | Command::GreaterEqual
            | Command::Less
            | Command::LessEqual),
        ) => {
            let mut mut_stack = stack.0.borrow_mut(mc);
            let rhs = pop_stack(&mut mut_stack, token.span)?;
            let lhs = pop_stack(&mut mut_stack, token.span)?;
            let result = lhs.compare(rhs, command, token.span)?;
            mut_stack.push_back(Stackable::Boolean(command == result));
            Ok(().into())
        }
        InnerToken::Command(Command::Not) => {
            let mut mut_stack = stack.0.borrow_mut(mc);
            let value = pop_stack(&mut mut_stack, token.span)?;
            let result = value.negate(token.span)?;
            mut_stack.push_back(result);
            Ok(().into())
        }
        InnerToken::Command(Command::And) => {
            let mut mut_stack = stack.0.borrow_mut(mc);
            let rhs = pop_stack(&mut mut_stack, token.span)?;
            let lhs = pop_stack(&mut mut_stack, token.span)?;
            let result = lhs.and(rhs, token.span)?;
            mut_stack.push_back(result);
            Ok(().into())
        }
        InnerToken::Command(Command::Or) => {
            let mut mut_stack = stack.0.borrow_mut(mc);
            let rhs = pop_stack(&mut mut_stack, token.span)?;
            let lhs = pop_stack(&mut mut_stack, token.span)?;
            let result = lhs.or(rhs, token.span)?;
            mut_stack.push_back(result);
            Ok(().into())
        }
        InnerToken::Command(Command::Xor) => {
            let mut mut_stack = stack.0.borrow_mut(mc);
            let rhs = pop_stack(&mut mut_stack, token.span)?;
            let lhs = pop_stack(&mut mut_stack, token.span)?;
            let result = lhs.xor(rhs, token.span)?;
            mut_stack.push_back(result);
            Ok(().into())
        }
        InnerToken::Command(Command::Assert) => {
            let mut mut_stack = stack.0.borrow_mut(mc);
            let value = pop_stack(&mut mut_stack, token.span)?;
            if matches!(value, Stackable::Boolean(false)) {
                Err(Error::AssertionFailed { span: token.span })
            } else {
                Ok(().into())
            }
        }
        InnerToken::Command(Command::Pop) => {
            let mut mut_stack = stack.0.borrow_mut(mc);
            let _ = pop_stack(&mut mut_stack, token.span)?;
            Ok(().into())
        }
        InnerToken::Command(Command::Call) => {
            // make sure to end mutable stack borrow before call occurs, which will likely borrow mutably again
            let callable = {
                let mut mut_stack = stack.0.borrow_mut(mc);
                pop_stack(&mut mut_stack, token.span)?
            };
            callable.call(mc, stack.clone(), token.span)
        }
        _ => todo!(),
    }
}

fn pop_stack<'a>(stack: &mut InnerStack<'a>, span: SourceSpan) -> Result<Stackable<'a>, Error> {
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
