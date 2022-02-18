package klfr.sof.test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.util.*;
import java.util.stream.*;

import org.junit.jupiter.api.*;

import klfr.sof.*;
import klfr.sof.ast.TokenListNode;
import klfr.sof.exceptions.*;
import klfr.sof.lang.*;
import klfr.sof.lang.Stackable.DebugStringExtensiveness;
import klfr.sof.lang.TransparentData.TransparentType;
import klfr.sof.lang.functional.*;
import klfr.sof.lang.oop.*;
import klfr.sof.lang.oop.Object;
import klfr.sof.lang.primitive.*;

@DisplayName("Test outputting and printing")
public class PrintTests extends SofTestSuper {

	@DisplayName("Printing and outputting of Stackable types")
	@TestFactory
	Stream<DynamicTest> testPrintStackables() throws SOFException {
		final var codeBlock = new CodeBlock(new TokenListNode(List.of(), 12, new SOFFile(null, "source", null)));
		return List
				.of(new Nametable(), new Identifier("identifier"), new TransparentData(TransparentType.CurryPipe), IntPrimitive.createIntPrimitive(42l), FloatPrimitive.createFloatPrimitive(20.4d), BoolPrimitive.createBoolPrimitive(true),
						new Object(), new ListPrimitive(List.of(new Identifier("blah"), new Identifier("blah2"))), new FunctionDelimiter(), new MethodDelimiter(), Function.fromCodeBlock(codeBlock, 3, new Nametable()), codeBlock,
						StringPrimitive.createStringPrimitive("string"), new CurriedFunction(Function.fromCodeBlock(codeBlock, 6, new Nametable()), List.of(new Identifier("moreblah")), new Nametable()))
				.stream().map(object -> dynamicTest(object.typename(), () -> {
					for (final var extensiveness : new DebugStringExtensiveness[] { DebugStringExtensiveness.Full, DebugStringExtensiveness.Compact, DebugStringExtensiveness.Type })
						assertDoesNotThrow(() -> object.toDebugString(extensiveness));
					assertDoesNotThrow(() -> object.print());
					assertDoesNotThrow(() -> object.toString());
				}));
	}

}
