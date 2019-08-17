package klfr.sof.lang;

import java.util.regex.Matcher;
import klfr.sof.Interpreter;

public class CodeBlock implements Callable {

	private static final long serialVersionUID = 1L;

	private int	indexInFile;
	private int	endIndex;

	private String code;

	public CodeBlock(int indexInFile, int endIndex, String code) {
		this.indexInFile = indexInFile;
		this.endIndex = endIndex;
		this.code = code;
	}

	@Override
	public String getDebugDisplay() {
		return "CodeBlock{" + code + "}";
	}

	public String toString() {
		return "[CodeBlock " + this.hashCode() + "]";
	}

	@Override
	public Stackable clone() {
		return new CodeBlock(this.indexInFile, this.endIndex, this.code);
	}

	@Override
	public CallProvider getCallProvider() {
		return (interpreter) -> {
			Matcher interpreterMatcher = interpreter.getMatcher();
			int returnIndexOfLastMatch = interpreterMatcher.start();
			int returnStart = interpreterMatcher.regionStart(), returnEnd = interpreterMatcher.regionEnd();
			
			// set the region
			interpreterMatcher.region(indexInFile, endIndex);
			
			while (interpreter.canExecute()) {
				interpreter.executeOnce();
			}
			//reset the region to previous state
			interpreterMatcher.region(returnStart, returnEnd);
			//finds the last match again
			interpreterMatcher.find(returnIndexOfLastMatch);
			
			return null;
		};
	}

}
