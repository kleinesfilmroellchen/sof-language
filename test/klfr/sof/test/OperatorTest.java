package klfr.sof.test;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import klfr.sof.lang.Callable;
import klfr.sof.lang.FloatPrimitive;
import klfr.sof.lang.IntPrimitive;
import klfr.sof.CompilerException;
import klfr.sof.lang.BuiltinPTs;
import klfr.sof.lang.Primitive;

@SuppressWarnings("unchecked")
class OperatorTest extends SofTestSuper {

	// private Callable operationTest;

	// @BeforeEach
	// void setUp() {
	// // simple operation that computes 2 * a * b
	// operationTest = Callable.fromFunction((a,b) -> {
	// if (a instanceof IntPrimitive && b instanceof IntPrimitive) {
	// return ((IntPrimitive) a).multiply((IntPrimitive)
	// b).multiply(IntPrimitive.createIntPrimitive(2l));
	// }
	// if (a instanceof FloatPrimitive && b instanceof FloatPrimitive) {
	// return ((FloatPrimitive) a).multiply((FloatPrimitive)
	// b).multiply(FloatPrimitive.createFloatPrimitive(2d));
	// }
	// if (a instanceof FloatPrimitive && b instanceof IntPrimitive) {
	// return FloatPrimitive.createFloatPrimitive(((FloatPrimitive) a).value() *
	// ((IntPrimitive) b).value() * 2);
	// }
	// if (b instanceof FloatPrimitive && a instanceof IntPrimitive) {
	// return FloatPrimitive.createFloatPrimitive(((FloatPrimitive) b).value() *
	// ((IntPrimitive) a).value() * 2);
	// }
	// throw CompilerException.makeIncomplete("Type",
	// String.format("Type error in test callable. Operand types %s and %s.",
	// a.typename(), b.typename()));
	// });
	// }

	@Test
	void testBuiltinOperations() {
		// TODO: test all builtin operations
		fail("Not yet implemented");
	}

}
