package klfr.sof.test;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import klfr.sof.CompilationError;
import klfr.sof.lang.Operator;
import klfr.sof.lang.Primitive;

@SuppressWarnings("unchecked")
class OperatorTest {
	
	private Operator operationTest;
	
	@BeforeEach
	void setUp() {
		// simple operation that computes 2 * a * b
		operationTest = (x,y) -> Operator.numericOperation((Primitive<? extends Number>)x, (Primitive<? extends Number>)y, (a, b) -> a * 2 * b,
				(a, b) -> a * 2 * b);
	}

	@Test
	void testNumericOperation() {
		// test with two longs
		assertDoesNotThrow( () ->
			assertEquals(Long.valueOf(2*2*3), ((Primitive<Long>) operationTest.call(
					new Primitive<Long>(2l),
					new Primitive<Long>(3l)))
				.getValue()));
		// test with one long and one double: should return double
		assertDoesNotThrow( () ->
			assertEquals(Double.valueOf(2*2*3), ((Primitive<Double>) operationTest.call(
					new Primitive<Double>(2.0d),
					new Primitive<Long>(3l)))
				.getValue()));
		// test with two doubles
		assertDoesNotThrow( () ->
			assertEquals(Double.valueOf(2*2*3), ((Primitive<Double>) operationTest.call(
					new Primitive<Double>(2.0d),
					new Primitive<Double>(3.0d)))
				.getValue()));
		// test with illegal arguments: e.g. string and number
		assertThrows(ClassCastException.class, () -> operationTest.call(
				new Primitive<String>("abc"),
				new Primitive<Long>(4l)));
	}
	
	@Test
	void testBuiltinOperations() {
		assertDoesNotThrow(() -> 
			assertEquals(new Primitive<Long>(Long.valueOf(2+3)), Operator.add.call(
				new Primitive<Long>(2l), new Primitive<Long>(3l))));
		assertDoesNotThrow(() -> 
			assertEquals(new Primitive<Long>(Long.valueOf(2*3)), Operator.multiply.call(
				new Primitive<Long>(2l), new Primitive<Long>(3l))));
		assertDoesNotThrow(() -> 
			assertEquals(new Primitive<Long>(Long.valueOf(2-3)), Operator.subtract.call(
				new Primitive<Long>(2l), new Primitive<Long>(3l))));
		assertDoesNotThrow(() -> 
			assertEquals(new Primitive<Long>(Long.valueOf(15/3)), Operator.divide.call(
				new Primitive<Long>(15l), new Primitive<Long>(3l))));
		// check if division by zero is caught
		assertThrows(CompilationError.class, () -> 
			Operator.divide.call(
				new Primitive<Long>(15l), new Primitive<Long>(0l)));
	}

}
