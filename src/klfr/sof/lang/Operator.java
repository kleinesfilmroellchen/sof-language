package klfr.sof.lang;

import java.util.function.BiFunction;
import klfr.sof.CompilerException;

/**
 * Base class for operator functions. Operator functions differ from normal
 * functions in that they don't need to be called with the call operator '.' and
 * from normal Callables in that they are not aware of any state and don't
 * modify state.<br>
 * <br>
 * Because this is a functional interface, operators can be constructed with a
 * Lambda expression.
 * @author klfr
 */
@FunctionalInterface
@SuppressWarnings("unchecked")
public interface Operator {
	/**
	 * Execute a pure, stateless computation on the arguments and return a result.
	 * As the input arguments are copied before invocation, their modification will
	 * never have any effect outside this function.<br>
	 * <br>
	 * Implementors are free to apply unsafe class casting, as the callers catch the
	 * resulting exceptions.
	 * @param leftArg The left argument, or the argument that is lower on the stack.
	 * @param rightArg The right argument, or the argument that is higher on the
	 * stack.
	 * @throws ClassCastException if the arguments are not of the correct type
	 * (auto-thrown by the JVM if unchecked type casting fails).
	 * @throws CompilerException if other stuff fails, such as arithmetic exceptions.
	 */
	public Stackable call(Stackable leftArg, Stackable rightArg) throws ClassCastException, CompilerException;

	/**
	 * Executes a numeric operation on the two argument primitives
	 * @param a left operand
	 * @param b right operand
	 * @param longOperation Operation to execute if both numbers are long/integer
	 * types
	 * @param doubleOperation Operation to execute if any one of the numbers are
	 * double/decimal type
	 * @return The result of the operation. Operations should be the same, i.e.
	 * supply the same lambda twice.
	 */
	public static Primitive<? extends Number> numericOperation(Primitive<? extends Number> a,
			Primitive<? extends Number> b, BiFunction<Long, Long, Long> longOperation,
			BiFunction<Double, Double, Double> doubleOperation) {
		//oh heck java wtf is this madness
		Class<? extends Number> bc = b.getValue().getClass(), ac = a.getValue().getClass();
		if (bc.equals(Long.class) && ac.equals(Long.class)) {
			return new Primitive<Long>(longOperation.apply(a.getValue().longValue(), b.getValue().longValue()));
		} else {
			// cast all other types to double
			return new Primitive<Double>(doubleOperation.apply(a.getValue().doubleValue(), b.getValue().doubleValue()));
		}
	}

	public static final Operator divide = (a, b) -> {
		try {
			return numericOperation((Primitive<? extends Number>) a, (Primitive<? extends Number>) b, (x, y) -> x / y,
					(x, y) -> x / y);
		} catch (ArithmeticException e) {
			throw CompilerException.fromIncompleteInfo("Arithmetic", "Divide by zero.");
		}
	};

	public static final Operator add = (a, b) -> numericOperation((Primitive<? extends Number>) a,
			(Primitive<? extends Number>) b, (x, y) -> x + y,
			(x, y) -> x + y);

	public static final Operator multiply = (a, b) -> numericOperation((Primitive<? extends Number>) a,
			(Primitive<? extends Number>) b, (x, y) -> x * y,
			(x, y) -> x * y);

	public static final Operator subtract = (a, b) -> numericOperation((Primitive<? extends Number>) a,
			(Primitive<? extends Number>) b, (x, y) -> x - y,
			(x, y) -> x - y);
}
