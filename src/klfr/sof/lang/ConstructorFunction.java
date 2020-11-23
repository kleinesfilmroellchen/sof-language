package klfr.sof.lang;

import klfr.sof.ast.TokenListNode;

/**
 * A special function that is a constructor, i.e. a function responsible for creating ("constructing") a new object.
 * The internal structure is not particularly special, it simply acts as a marker to the interpreter system
 * that some special logic needs to act before and after the object creation.
 */
public class ConstructorFunction extends SOFunction {
	private static final long serialVersionUID = 1L;

	public ConstructorFunction(TokenListNode code, long arguments) {
		super(code, arguments);
	}
	
}
