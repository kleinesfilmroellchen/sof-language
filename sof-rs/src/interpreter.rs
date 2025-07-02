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

#[derive(Default, Clone, Copy)]
#[non_exhaustive]
pub struct Metrics {
    pub token_count: usize,
    pub gc_count: usize,
    pub call_count: usize,
}

pub fn run(tokens: Vec<Token>) -> Result<Metrics, Error> {
    let mut arena: StackArena = new_arena();
    run_on_arena(&mut arena, tokens)
}

pub fn new_arena() -> StackArena {
    Arena::new(|mc| {
        let main = Gc::new(mc, RefLock::new(VecDeque::with_capacity(64)));
        let utility = Gc::new(mc, RefLock::new(VecDeque::with_capacity(64)));
        Stack { main, utility }
    })
}

// TODO: tune this
pub const COLLECTION_THRESHOLD: f64 = 0.2;

pub fn run_on_arena(arena: &mut StackArena, tokens: Vec<Token>) -> Result<Metrics, Error> {
    let token_iter = tokens.into_iter();
    // Use a stack of token lists to be executed. At the end of each list is some kind of call return.
    let mut token_execution_stack = VecDeque::new();
    token_execution_stack.push_back((token_iter, CallReturnBehavior::BlockCall));
    let mut metrics = Metrics::default();

    while let Some((current_stack, stack_action)) = token_execution_stack.back_mut() {
        let Some(token) = current_stack.next() else {
            match *stack_action {
                CallReturnBehavior::BlockCall => {
                    debug!(
                        "[{}] returning from block, remaining call stack depth {}",
                        metrics.token_count,
                        token_execution_stack.len() - 1
                    );
                    token_execution_stack.pop_back();
                }
                CallReturnBehavior::FunctionCall => todo!("finalize function call"),
                CallReturnBehavior::Loop => {
                    arena.mutate(|mc, stack| {
                        let mut utility_stack = stack.utility.borrow_mut(mc);
                        // pop last conditional
                        let last_conditional_result = utility_stack.pop_back().unwrap();

                        if !matches!(last_conditional_result, Stackable::Boolean(true)) {
                            trace!(
                                "[{}] exiting while loop because of condition {:?}",
                                metrics.token_count, last_conditional_result
                            );
                            // pop utility data (loop body and conditional)
                            utility_stack.pop_back();
                            utility_stack.pop_back();
                            // remove the loop element
                            token_execution_stack.pop_back();
                        } else {
                            trace!("[{}] re-running while loop", metrics.token_count);
                            let conditional_callable =
                                utility_stack[utility_stack.len() - 1].clone();
                            let mut mut_stack = stack.main.borrow_mut(mc);
                            // add back callable
                            mut_stack.push_back(conditional_callable);
                            // remove the loop element
                            token_execution_stack.pop_back();
                            // push another round of loop execution to the stack
                            token_execution_stack.push_back((
                                vec![
                                    Token {
                                        inner: InnerToken::Command(Command::Call),
                                        span: (0, 0).into(),
                                    },
                                    Token {
                                        inner: InnerToken::WhileBody,
                                        span: (0, 0).into(),
                                    },
                                ]
                                .into_iter(),
                                CallReturnBehavior::Loop,
                            ));
                        }
                    });
                }
            }
            continue;
        };

        metrics.token_count += 1;
        trace!("[{}] executing token {token:?}", metrics.token_count);
        let actions: Vec<InterpreterAction> =
            arena.mutate(|mc, stack| execute_token(token.clone(), mc, stack.clone()))?;
        arena.mutate(|_, stack| {
            trace!("[{}] stack: {:?}", metrics.token_count, stack.main);
        });

        if arena.metrics().allocation_debt() >= COLLECTION_THRESHOLD {
            debug!(
                target: "sof::gc",
                "[{}] debt is {} at total allocation of {}, running GC.",
                metrics.token_count,
                arena.metrics().allocation_debt(),
                arena.metrics().total_allocation()
            );
            metrics.gc_count += 1;
            arena.collect_debt();
        }

        for action in actions {
            match action {
                InterpreterAction::None => {}
                InterpreterAction::ExecuteCall {
                    code,
                    return_behavior,
                } => {
                    debug!(
                        "[{}] entering call of type {return_behavior:?} and {} tokens, call stack depth {}",
                        metrics.token_count,
                        code.len(),
                        token_execution_stack.len() + 1
                    );
                    metrics.call_count += 1;
                    token_execution_stack.push_back((code.into_iter(), return_behavior));
                }
            }
        }
    }
    Ok(metrics)
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
    /// The call was a loop iteration.
    ///
    /// Read conditional and loop body from the utility stack and copy them to the main stack.
    /// return [execute(call, whilebody)] with return behavior "Loop" for the action stack
    /// such that first it will call the conditional and then run the body.
    /// The return behavior ensures this will repeat as needed.
    Loop,
}

#[inline]
fn no_action() -> Result<Vec<InterpreterAction>, Error> {
    Ok(vec![InterpreterAction::None])
}

fn execute_token<'a>(
    token: Token,
    mc: &Mutation<'a>,
    stack: Stack<'a>,
) -> Result<Vec<InterpreterAction>, Error> {
    match token.inner {
        InnerToken::Literal(literal) => {
            let mut mut_stack = stack.main.borrow_mut(mc);
            mut_stack.push_back(literal.as_stackable());
            no_action()
        }
        InnerToken::CodeBlock(tokens) => {
            let mut mut_stack = stack.main.borrow_mut(mc);
            mut_stack.push_back(Stackable::CodeBlock(GcRefLock::new(
                mc,
                CodeBlock { code: tokens }.into(),
            )));
            no_action()
        }
        InnerToken::Command(Command::Plus) => {
            let mut mut_stack = stack.main.borrow_mut(mc);
            let rhs = pop_stack(&mut mut_stack, token.span)?;
            let lhs = pop_stack(&mut mut_stack, token.span)?;
            let result = lhs.add(rhs, token.span)?;
            mut_stack.push_back(result);
            no_action()
        }
        InnerToken::Command(Command::Minus) => {
            let mut mut_stack = stack.main.borrow_mut(mc);
            let rhs = pop_stack(&mut mut_stack, token.span)?;
            let lhs = pop_stack(&mut mut_stack, token.span)?;
            let result = lhs.subtract(rhs, token.span)?;
            mut_stack.push_back(result);
            no_action()
        }
        InnerToken::Command(Command::Multiply) => {
            let mut mut_stack = stack.main.borrow_mut(mc);
            let rhs = pop_stack(&mut mut_stack, token.span)?;
            let lhs = pop_stack(&mut mut_stack, token.span)?;
            let result = lhs.multiply(rhs, token.span)?;
            mut_stack.push_back(result);
            no_action()
        }
        InnerToken::Command(Command::Divide) => {
            let mut mut_stack = stack.main.borrow_mut(mc);
            let rhs = pop_stack(&mut mut_stack, token.span)?;
            let lhs = pop_stack(&mut mut_stack, token.span)?;
            let result = lhs.divide(rhs, token.span)?;
            mut_stack.push_back(result);
            no_action()
        }
        InnerToken::Command(Command::Modulus) => {
            let mut mut_stack = stack.main.borrow_mut(mc);
            let rhs = pop_stack(&mut mut_stack, token.span)?;
            let lhs = pop_stack(&mut mut_stack, token.span)?;
            let result = lhs.modulus(rhs, token.span)?;
            mut_stack.push_back(result);
            no_action()
        }
        InnerToken::Command(Command::LeftShift) => {
            let mut mut_stack = stack.main.borrow_mut(mc);
            let rhs = pop_stack(&mut mut_stack, token.span)?;
            let lhs = pop_stack(&mut mut_stack, token.span)?;
            let result = lhs.shift_left(rhs, token.span)?;
            mut_stack.push_back(result);
            no_action()
        }
        InnerToken::Command(Command::RightShift) => {
            let mut mut_stack = stack.main.borrow_mut(mc);
            let rhs = pop_stack(&mut mut_stack, token.span)?;
            let lhs = pop_stack(&mut mut_stack, token.span)?;
            let result = lhs.shift_right(rhs, token.span)?;
            mut_stack.push_back(result);
            no_action()
        }
        InnerToken::Command(Command::Equal) => {
            let mut mut_stack = stack.main.borrow_mut(mc);
            let rhs = pop_stack(&mut mut_stack, token.span)?;
            let lhs = pop_stack(&mut mut_stack, token.span)?;
            let result = Stackable::Boolean(lhs.eq(&rhs));
            mut_stack.push_back(result);
            no_action()
        }
        InnerToken::Command(Command::NotEqual) => {
            let mut mut_stack = stack.main.borrow_mut(mc);
            let rhs = pop_stack(&mut mut_stack, token.span)?;
            let lhs = pop_stack(&mut mut_stack, token.span)?;
            let result = Stackable::Boolean(lhs.ne(&rhs));
            mut_stack.push_back(result);
            no_action()
        }
        InnerToken::Command(
            command @ (Command::Greater
            | Command::GreaterEqual
            | Command::Less
            | Command::LessEqual),
        ) => {
            let mut mut_stack = stack.main.borrow_mut(mc);
            let rhs = pop_stack(&mut mut_stack, token.span)?;
            let lhs = pop_stack(&mut mut_stack, token.span)?;
            let result = lhs.compare(rhs, command, token.span)?;
            mut_stack.push_back(Stackable::Boolean(command == result));
            no_action()
        }
        InnerToken::Command(Command::Not) => {
            let mut mut_stack = stack.main.borrow_mut(mc);
            let value = pop_stack(&mut mut_stack, token.span)?;
            let result = value.negate(token.span)?;
            mut_stack.push_back(result);
            no_action()
        }
        InnerToken::Command(Command::And) => {
            let mut mut_stack = stack.main.borrow_mut(mc);
            let rhs = pop_stack(&mut mut_stack, token.span)?;
            let lhs = pop_stack(&mut mut_stack, token.span)?;
            let result = lhs.and(rhs, token.span)?;
            mut_stack.push_back(result);
            no_action()
        }
        InnerToken::Command(Command::Or) => {
            let mut mut_stack = stack.main.borrow_mut(mc);
            let rhs = pop_stack(&mut mut_stack, token.span)?;
            let lhs = pop_stack(&mut mut_stack, token.span)?;
            let result = lhs.or(rhs, token.span)?;
            mut_stack.push_back(result);
            no_action()
        }
        InnerToken::Command(Command::Xor) => {
            let mut mut_stack = stack.main.borrow_mut(mc);
            let rhs = pop_stack(&mut mut_stack, token.span)?;
            let lhs = pop_stack(&mut mut_stack, token.span)?;
            let result = lhs.xor(rhs, token.span)?;
            mut_stack.push_back(result);
            no_action()
        }
        InnerToken::Command(Command::Assert) => {
            let mut mut_stack = stack.main.borrow_mut(mc);
            let value = pop_stack(&mut mut_stack, token.span)?;
            if matches!(value, Stackable::Boolean(false)) {
                Err(Error::AssertionFailed { span: token.span })
            } else {
                no_action()
            }
        }
        InnerToken::Command(Command::Pop) => {
            let mut mut_stack = stack.main.borrow_mut(mc);
            let _ = pop_stack(&mut mut_stack, token.span)?;
            no_action()
        }
        InnerToken::Command(Command::Dup) => {
            let mut mut_stack = stack.main.borrow_mut(mc);
            let value = pop_stack(&mut mut_stack, token.span)?;
            mut_stack.push_back(value.clone());
            mut_stack.push_back(value);
            no_action()
        }
        InnerToken::Command(Command::Swap) => {
            let mut mut_stack = stack.main.borrow_mut(mc);
            let first = pop_stack(&mut mut_stack, token.span)?;
            let second = pop_stack(&mut mut_stack, token.span)?;
            mut_stack.push_back(second);
            mut_stack.push_back(first);
            no_action()
        }
        InnerToken::Command(Command::Call) => {
            // make sure to end mutable stack borrow before call occurs, which will likely borrow mutably again
            let callable = {
                let mut mut_stack = stack.main.borrow_mut(mc);
                pop_stack(&mut mut_stack, token.span)?
            };
            callable.enter_call(mc, stack.clone(), token.span)
        }
        InnerToken::Command(Command::If) => {
            let mut mut_stack = stack.main.borrow_mut(mc);
            let conditional = pop_stack(&mut mut_stack, token.span)?;
            let callable = pop_stack(&mut mut_stack, token.span)?;
            if matches!(conditional, Stackable::Boolean(true)) {
                trace!("    executing if body");
                callable.enter_call(mc, stack.clone(), token.span)
            } else {
                no_action()
            }
        }
        InnerToken::Command(Command::Ifelse) => {
            let mut mut_stack = stack.main.borrow_mut(mc);
            let callable_else = pop_stack(&mut mut_stack, token.span)?;
            let conditional = pop_stack(&mut mut_stack, token.span)?;
            let callable_if = pop_stack(&mut mut_stack, token.span)?;
            if matches!(conditional, Stackable::Boolean(true)) {
                trace!("    executing if body");
                callable_if.enter_call(mc, stack.clone(), token.span)
            } else {
                trace!("    executing else body");
                callable_else.enter_call(mc, stack.clone(), token.span)
            }
        }
        InnerToken::Command(Command::While) => {
            let mut mut_stack = stack.main.borrow_mut(mc);
            let conditional_callable = pop_stack(&mut mut_stack, token.span)?;
            let loop_body = pop_stack(&mut mut_stack, token.span)?;
            mut_stack.push_back(conditional_callable.clone());
            let mut utility_stack = stack.utility.borrow_mut(mc);
            utility_stack.push_back(loop_body);
            utility_stack.push_back(conditional_callable);
            trace!("    starting while");
            Ok(vec![InterpreterAction::ExecuteCall {
                code: vec![
                    Token {
                        inner: InnerToken::Command(Command::Call),
                        span: token.span,
                    },
                    Token {
                        inner: InnerToken::WhileBody,
                        span: token.span,
                    },
                ],
                return_behavior: CallReturnBehavior::Loop,
            }])
        }
        InnerToken::Command(Command::DoWhile) => {
            let mut mut_stack = stack.main.borrow_mut(mc);
            let conditional_callable = pop_stack(&mut mut_stack, token.span)?;
            let loop_body = pop_stack(&mut mut_stack, token.span)?;
            let mut utility_stack = stack.utility.borrow_mut(mc);
            trace!("    starting do-while, call body once");
            let mut actions = loop_body.enter_call(mc, stack, token.span)?;
            // set up utility stack for loop return behavior logic; last iteration is treated as true to not immediately exit
            utility_stack.push_back(loop_body);
            utility_stack.push_back(conditional_callable);
            utility_stack.push_back(Stackable::Boolean(true));

            // insert the while body logic at the start so it is executed after the initial loop body action(s)
            // the body action is *empty* so the return behavior is immediately run and it figures out the state of things from the utility stack setup
            actions.insert(
                0,
                InterpreterAction::ExecuteCall {
                    code: vec![],
                    return_behavior: CallReturnBehavior::Loop,
                },
            );
            Ok(actions)
        }
        // almost like if, but with the special utility stack
        InnerToken::WhileBody => {
            let mut utility_stack = stack.utility.borrow_mut(mc);
            let mut mut_stack = stack.main.borrow_mut(mc);
            let conditional = pop_stack(&mut mut_stack, token.span)?;
            let loop_body = utility_stack[utility_stack.len() - 2].clone();
            // signal to the loop end what the conditional looks like
            utility_stack.push_back(conditional.clone());
            if matches!(conditional, Stackable::Boolean(true)) {
                trace!("    executing while body");
                loop_body.enter_call(mc, stack.clone(), token.span)
            } else {
                trace!("    --- end of while body, condition is false");
                no_action()
            }
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
