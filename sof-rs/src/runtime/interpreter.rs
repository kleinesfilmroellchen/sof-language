use std::cell::LazyCell;
use std::sync::Arc;

use gc_arena::lock::{GcRefLock, RefLock};
use gc_arena::{Arena, Mutation};
use log::{debug, info, trace};
use smallvec::{SmallVec, smallvec};

use crate::arc_iter::ArcVecIter;
use crate::error::Error;
use crate::identifier::Identifier;
use crate::lib::DEFAULT_REGISTRY;
use crate::runtime::stackable::{CodeBlock, Function, TokenVec};
use crate::runtime::util::{SwitchCase, SwitchCases, UtilityData};
use crate::runtime::{Stack, StackArena, Stackable};
use crate::token::{Command, InnerToken, Token};

#[derive(Default, Clone, Copy)]
#[non_exhaustive]
#[allow(clippy::struct_field_names)] // may in the future contain other data not called `count`
pub struct Metrics {
	pub token_count: usize,
	pub gc_count:    usize,
	pub call_count:  usize,
}

pub fn run(tokens: TokenVec) -> Result<Metrics, Error> {
	let mut arena: StackArena = new_arena();
	run_on_arena(&mut arena, tokens)
}

pub fn new_arena() -> StackArena {
	Arena::new(|mc| Stack::new(mc))
}

// TODO: tune this
pub const COLLECTION_THRESHOLD: f64 = 50.;

pub fn run_on_arena(arena: &mut StackArena, tokens: TokenVec) -> Result<Metrics, Error> {
	let token_iter = ArcVecIter::new(tokens);
	// Use a stack of token lists to be executed. At the end of each list is some kind of call return.
	let mut token_execution_stack = Vec::new();
	token_execution_stack.push((token_iter, CallReturnBehavior::BlockCall));
	let mut metrics = Metrics::default();

	// true if we’re currently in the process of unwinding a return call up to the next function
	let mut is_unwinding_return = false;
	while let Some((current_stack, stack_action)) = token_execution_stack.last_mut() {
		let Some(token) = current_stack.next() else {
			match *stack_action {
				CallReturnBehavior::BlockCall => {
					debug!(
						"[{}] returning from block, remaining call stack depth {}",
						metrics.token_count,
						token_execution_stack.len() - 1
					);
					token_execution_stack.pop();
				},
				CallReturnBehavior::FunctionCall => {
					debug!(
						"[{}] returning from function, remaining call stack depth {}",
						metrics.token_count,
						token_execution_stack.len() - 1
					);
					is_unwinding_return = false;
					token_execution_stack.pop();
					arena.mutate_root(|_mc, stack| {
						let value = stack.pop_nametable((0, 0).into())?;
						if let Some(return_value) = &value.borrow().return_value {
							stack.push(return_value.clone());
						}
						Ok(())
					})?;
				},
				CallReturnBehavior::Loop => {
					arena.mutate_root(|_mc, stack| {
						let utility_stack = &mut stack.utility;

						let Some(UtilityData::While { conditional_callable, conditional_result, .. }) =
							utility_stack.last_mut()
						else {
							panic!("incorrect while data")
						};

						if !*conditional_result || is_unwinding_return {
							trace!("[{}] exiting while loop because condition is false", metrics.token_count);
							// pop utility data (loop body and conditional)
							utility_stack.pop();
							// remove the loop element
							token_execution_stack.pop();
						} else {
							trace!("[{}] re-running while loop", metrics.token_count);
							// add back callable
							let conditional_callable = conditional_callable.clone();
							stack.push(conditional_callable);
							// remove the loop element
							token_execution_stack.pop();
							// push another round of loop execution to the stack
							token_execution_stack.push((
								ArcVecIter::new(
									vec![
										Token { inner: InnerToken::Command(Command::Call), span: (0, 0).into() },
										Token { inner: InnerToken::WhileBody, span: (0, 0).into() },
									]
									.into(),
								),
								CallReturnBehavior::Loop,
							));
						}
					});
				},
			}
			continue;
		};

		metrics.token_count += 1;
		trace!("[{}] executing token {token:?}", metrics.token_count);
		let actions: ActionVec = arena.mutate_root(|mc, stack| execute_token(token, mc, stack))?;
		arena.mutate(|_, stack| {
			trace!("[{}] stack: {:?}", metrics.token_count, stack);
		});

		// only check debt occasionally, as it is expensive to just request it
		if metrics.token_count.is_multiple_of(128) {
			// perform garbage collection if necessary
			if arena.metrics().allocation_debt() >= COLLECTION_THRESHOLD {
				debug!(
					target: "sof::gc",
					"[{}] debt is {} at total GC allocation of {}, running GC.",
					metrics.token_count,
					arena.metrics().allocation_debt(),
					arena.metrics().total_gc_allocation()
				);
				metrics.gc_count += 1;
				arena.collect_debt();
			}
		}

		for action in actions {
			match action {
				InterpreterAction::None => {},
				InterpreterAction::ExecuteCall { code, return_behavior } => {
					debug!(
						"[{}] entering call of type {return_behavior:?} and {} tokens, call stack depth {}",
						metrics.token_count,
						code.len(),
						token_execution_stack.len() + 1
					);
					metrics.call_count += 1;
					token_execution_stack.push((ArcVecIter::new(code), return_behavior));
				},
				InterpreterAction::Return => {
					debug!(
						"[{}] immediate function return, call stack depth {}",
						metrics.token_count,
						token_execution_stack.len() + 1
					);
					// Many execution stacks, such as loops, need to do complex cleanup.
					// Therefore, we simply signal every single stack to exit by
					// (1) emptying all stack’s token lists
					// (2) setting the global return flag that causes loops (and other constructs) to terminate
					// unconditionally.
					let mut stack_position = token_execution_stack.len() - 1;
					loop {
						let (list, behavior) = &mut token_execution_stack[stack_position];
						*list = ArcVecIter::new(Arc::default());
						// this was our function call, stop here; and failsafe for global token list
						if *behavior == CallReturnBehavior::FunctionCall || stack_position == 0 {
							break;
						}
						stack_position -= 1;
					}
					is_unwinding_return = true;
				},
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
	ExecuteCall { code: TokenVec, return_behavior: CallReturnBehavior },
	/// Return from current function scope.
	Return,
}

impl From<()> for InterpreterAction {
	fn from((): ()) -> Self {
		Self::None
	}
}

pub type ActionVec = SmallVec<[InterpreterAction; 2]>;

/// Different possible behaviors after a call return, each corresponding to a kind of call that SOF can do.
#[derive(Clone, Copy, Debug, Default, PartialEq, Eq)]
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
#[allow(clippy::unnecessary_wraps)] // must have this signature
fn no_action() -> Result<ActionVec, Error> {
	Ok(smallvec![InterpreterAction::None])
}

macro_rules! binary_op {
	($op:ident, $stack:ident, $mc:ident, $token:ident) => {{
		let rhs = $stack.pop($token.span)?;
		let lhs = $stack.pop($token.span)?;
		if let Stackable::Identifier(lhs_ident) = &lhs {
			let left_value = $stack.lookup(lhs_ident, $token.span)?;
			let result = left_value.$op(&rhs, $token.span)?;
			$stack.top_nametable().borrow_mut($mc).define(lhs_ident.clone(), result);
		} else {
			let result = lhs.$op(&rhs, $token.span)?;
			$stack.push(result);
		}
		no_action()
	}};
}

fn execute_token<'a>(token: &Token, mc: &Mutation<'a>, stack: &mut Stack<'a>) -> Result<ActionVec, Error> {
	match &token.inner {
		InnerToken::Literal(literal) => {
			stack.push(literal.as_stackable());
			no_action()
		},
		InnerToken::CodeBlock(tokens) => {
			stack.push(Stackable::CodeBlock(GcRefLock::new(mc, CodeBlock { code: tokens.clone() }.into())));
			no_action()
		},
		InnerToken::Command(Command::Plus) => binary_op!(add, stack, mc, token),
		InnerToken::Command(Command::Minus) => binary_op!(subtract, stack, mc, token),
		InnerToken::Command(Command::Multiply) => binary_op!(multiply, stack, mc, token),
		InnerToken::Command(Command::Divide) => binary_op!(divide, stack, mc, token),
		InnerToken::Command(Command::Modulus) => binary_op!(modulus, stack, mc, token),
		InnerToken::Command(Command::LeftShift) => binary_op!(shift_left, stack, mc, token),
		InnerToken::Command(Command::RightShift) => binary_op!(shift_right, stack, mc, token),
		InnerToken::Command(Command::Equal) => {
			let rhs = stack.pop(token.span)?;
			let lhs = stack.pop(token.span)?;
			debug!("{rhs} = {lhs}");
			let result = Stackable::Boolean(lhs.eq(&rhs));
			stack.push(result);
			no_action()
		},
		InnerToken::Command(Command::NotEqual) => {
			let rhs = stack.pop(token.span)?;
			let lhs = stack.pop(token.span)?;
			let result = Stackable::Boolean(lhs.ne(&rhs));
			stack.push(result);
			no_action()
		},
		InnerToken::Command(
			command @ (Command::Greater | Command::GreaterEqual | Command::Less | Command::LessEqual),
		) => {
			let rhs = stack.pop(token.span)?;
			let lhs = stack.pop(token.span)?;
			let result = lhs.compare(&rhs, *command, token.span)?;
			stack.push(Stackable::Boolean(*command == result));
			no_action()
		},
		InnerToken::Command(Command::Not) => {
			let value = stack.pop(token.span)?;
			let result = value.negate(token.span)?;
			stack.push(result);
			no_action()
		},
		InnerToken::Command(Command::And) => {
			let rhs = stack.pop(token.span)?;
			let lhs = stack.pop(token.span)?;
			let result = lhs.and(rhs, token.span)?;
			stack.push(result);
			no_action()
		},
		InnerToken::Command(Command::Or) => {
			let rhs = stack.pop(token.span)?;
			let lhs = stack.pop(token.span)?;
			let result = lhs.or(rhs, token.span)?;
			stack.push(result);
			no_action()
		},
		InnerToken::Command(Command::Xor) => {
			let rhs = stack.pop(token.span)?;
			let lhs = stack.pop(token.span)?;
			let result = lhs.xor(rhs, token.span)?;
			stack.push(result);
			no_action()
		},
		InnerToken::Command(Command::Assert) => {
			let value = stack.pop(token.span)?;
			if matches!(value, Stackable::Boolean(false)) {
				Err(Error::AssertionFailed { span: token.span })
			} else {
				no_action()
			}
		},
		InnerToken::Command(Command::Pop) => {
			let _ = stack.pop(token.span)?;
			no_action()
		},
		InnerToken::Command(Command::Dup) => {
			let value = stack.pop(token.span)?;
			stack.push(value.clone());
			stack.push(value);
			no_action()
		},
		InnerToken::Command(Command::Swap) => {
			let first = stack.pop(token.span)?;
			let second = stack.pop(token.span)?;
			stack.push(first);
			stack.push(second);
			no_action()
		},
		InnerToken::Command(Command::Call) => {
			// make sure to end mutable stack borrow before call occurs, which will likely borrow mutably again
			let callable = { stack.pop(token.span)? };
			callable.enter_call(mc, stack, token.span)
		},
		InnerToken::Command(Command::If) => {
			let conditional = stack.pop(token.span)?;
			let callable = stack.pop(token.span)?;
			if matches!(conditional, Stackable::Boolean(true)) {
				trace!("    executing if body");
				callable.enter_call(mc, stack, token.span)
			} else {
				no_action()
			}
		},
		InnerToken::Command(Command::Ifelse) => {
			let callable_else = stack.pop(token.span)?;
			let conditional = stack.pop(token.span)?;
			let callable_if = stack.pop(token.span)?;
			if matches!(conditional, Stackable::Boolean(true)) {
				trace!("    executing if body");
				callable_if.enter_call(mc, stack, token.span)
			} else {
				trace!("    executing else body");
				callable_else.enter_call(mc, stack, token.span)
			}
		},
		InnerToken::Command(Command::While) => {
			let conditional_callable = stack.pop(token.span)?;
			let loop_body = stack.pop(token.span)?;
			stack.utility.push(UtilityData::While {
				body:                 loop_body,
				conditional_callable: conditional_callable.clone(),
				conditional_result:   false,
			});
			trace!("    starting while");
			// add back conditional callable, which will immediately be called by our token sequence below
			stack.push(conditional_callable);
			Ok(smallvec![InterpreterAction::ExecuteCall {
				code:            vec![Token { inner: InnerToken::Command(Command::Call), span: token.span }, Token {
					inner: InnerToken::WhileBody,
					span:  token.span,
				},]
				.into(),
				return_behavior: CallReturnBehavior::Loop,
			}])
		},
		InnerToken::Command(Command::DoWhile) => {
			let conditional_callable = stack.pop(token.span)?;
			let loop_body = stack.pop(token.span)?;
			trace!("    starting do-while, call body once");
			let mut actions = loop_body.enter_call(mc, stack, token.span)?;
			// set up utility stack for loop return behavior logic; last iteration is treated as true to not immediately
			// exit
			stack.utility.push(UtilityData::While { body: loop_body, conditional_callable, conditional_result: true });
			#[allow(clippy::declare_interior_mutable_const, clippy::items_after_statements)]
			const EMPTY_VEC: LazyCell<TokenVec> = LazyCell::new(TokenVec::default);

			// insert the while body logic at the start so it is executed after the initial loop body action(s)
			// the body action is *empty* so the return behavior is immediately run and it figures out the state of
			// things from the utility stack setup
			actions.insert(0, InterpreterAction::ExecuteCall {
				#[allow(clippy::borrow_interior_mutable_const)]
				code:                                                 EMPTY_VEC.clone(),
				return_behavior:                                      CallReturnBehavior::Loop,
			});
			Ok(actions)
		},
		// almost like if, but with the special utility stack
		InnerToken::WhileBody => {
			let stack_conditional = stack.pop(token.span)?;
			let Some(UtilityData::While { body, conditional_result, .. }) = stack.utility.last_mut() else {
				panic!("incorrect while data")
			};
			// signal to the loop end what the conditional looks like
			*conditional_result = matches!(stack_conditional, Stackable::Boolean(true));
			if *conditional_result {
				trace!("    executing while body");
				body.clone().enter_call(mc, stack, token.span)
			} else {
				trace!("    --- end of while body, condition is false");
				no_action()
			}
		},
		InnerToken::Command(Command::Def) => {
			let next_nametable = stack.top_nametable();
			let name_stackable = stack.pop(token.span)?;
			let Stackable::Identifier(name) = name_stackable else {
				return Err(Error::InvalidType {
					operation: Command::Def,
					value:     name_stackable.to_string().into(),
					span:      token.span,
				});
			};
			let value = stack.pop(token.span)?;
			next_nametable.borrow_mut(mc).define(name, value);
			no_action()
		},
		InnerToken::Command(Command::Globaldef) => {
			let global_nametable = stack.global_nametable();
			let name_stackable = stack.pop(token.span)?;
			let Stackable::Identifier(name) = name_stackable else {
				return Err(Error::InvalidType {
					operation: Command::Globaldef,
					value:     name_stackable.to_string().into(),
					span:      token.span,
				});
			};
			let value = stack.pop(token.span)?;
			global_nametable.borrow_mut(mc).define(name, value);
			no_action()
		},
		InnerToken::Command(Command::Function) => {
			let argument_count = stack.pop(token.span)?;
			let body = stack.pop(token.span)?;
			let Stackable::Integer(argument_count) = argument_count else {
				return Err(Error::InvalidType {
					operation: Command::Function,
					value:     argument_count.to_string().into(),
					span:      token.span,
				});
			};
			let Stackable::CodeBlock(body) = body else {
				return Err(Error::InvalidType {
					operation: Command::Function,
					value:     body.to_string().into(),
					span:      token.span,
				});
			};
			stack.push(Stackable::Function(GcRefLock::new(
				mc,
				RefLock::new(Function::new(
					argument_count
						.try_into()
						.map_err(|_| Error::InvalidArgumentCount { argument_count, span: token.span })?,
					body.borrow().code.clone(),
				)),
			)));
			no_action()
		},
		InnerToken::Command(Command::Return) => {
			let return_value = stack.pop(token.span)?;
			let top_nametable = stack.top_nametable();
			top_nametable.borrow_mut(mc).set_return_value(return_value);
			Ok(smallvec![InterpreterAction::Return])
		},
		InnerToken::Command(Command::ReturnNothing) => Ok(smallvec![InterpreterAction::Return]),
		InnerToken::Command(Command::DescribeS) => {
			// TODO: make this a bit nicer
			info!("{stack:#?}");
			no_action()
		},
		InnerToken::Command(Command::Describe) => {
			info!("{:#?}", stack.raw_peek());
			no_action()
		},
		InnerToken::Command(Command::Switch) => {
			const SWITCH_MARKER: LazyCell<Identifier> = LazyCell::new(|| Identifier::new("switch::"));
			let default_case = stack.pop(token.span)?;
			let mut cases = SwitchCases(SmallVec::new());
			loop {
				let maybe_next_conditional = stack.pop(token.span)?;
				if let Stackable::Identifier(ref ident) = maybe_next_conditional
					&& *ident == *SWITCH_MARKER
				{
					break;
				}
				let body = stack.pop(token.span)?;
				cases.0.push(SwitchCase { conditional: maybe_next_conditional, body });
			}
			if cases.0.is_empty() {
				default_case.enter_call(mc, stack, token.span)
			} else {
				let SwitchCase { conditional, body } = cases.0.remove(0);
				stack.utility.push(UtilityData::Switch { remaining_cases: cases, default_case, next_body: body });
				stack.push(conditional);
				Ok(smallvec![InterpreterAction::ExecuteCall {
					code:            vec![
						Token { inner: InnerToken::Command(Command::Call), span: token.span },
						Token { inner: InnerToken::SwitchBody, span: token.span },
					]
					.into(),
					return_behavior: CallReturnBehavior::BlockCall,
				}])
			}
		},
		InnerToken::SwitchBody => {
			let should_execute_body = stack.pop(token.span)?;
			let Some(UtilityData::Switch { remaining_cases, default_case, next_body }) = stack.utility.last_mut()
			else {
				panic!("incorrect switch data")
			};
			// found a true case, remove the utility data and call the next body
			if matches!(should_execute_body, Stackable::Boolean(true)) {
				trace!("    found true switch case, entering body");
				let result = next_body.clone().enter_call(mc, stack, token.span);
				stack.utility.pop();
				result
			} else {
				// if possible, grab next case data, update next body, and call callable
				if let Some(SwitchCase { conditional, body }) = remaining_cases.0.first() {
					// very particular order of operation to not step on mutable borrows
					*next_body = body.clone();
					let conditional_clone = conditional.clone();
					remaining_cases.0.remove(0);
					stack.push(conditional_clone);
					// run the SwitchBody logic again after the conditional call, to evaluate the next body we just
					// prepared
					Ok(smallvec![InterpreterAction::ExecuteCall {
						code:            vec![
							Token { inner: InnerToken::Command(Command::Call), span: token.span },
							Token { inner: InnerToken::SwitchBody, span: token.span },
						]
						.into(),
						return_behavior: CallReturnBehavior::BlockCall,
					}])
				} else {
					trace!("    falling back to default switch body");
					// call default body
					default_case.clone().enter_call(mc, stack, token.span)
				}
			}
		},
		InnerToken::Command(Command::NativeCall) => {
			let function_name = stack.pop(token.span)?;
			let Stackable::String(name) = function_name else {
				return Err(Error::InvalidType {
					operation: Command::NativeCall,
					value:     function_name.to_string().into(),
					span:      token.span,
				});
			};
			DEFAULT_REGISTRY.call_function(&name, stack, token.span)?;
			no_action()
		},
		InnerToken::Command(_) => todo!(),
	}
}
