package klfr.sof.lang;

public class CodeBlock implements Callable {

	private static final long serialVersionUID = 1L;

	protected int indexInFile;
	protected int endIndex;

	protected String code;

	public CodeBlock(int indexInFile, int endIndex, String code) {
		this.indexInFile = indexInFile;
		this.endIndex = endIndex;
		this.code = code;
	}

	@Override
	public String getDebugDisplay() {
		return "CodeBlock{" + getCode() + "}";
	}

	public String toString() {
		return "[CodeBlock " + this.hashCode() + "]";
	}

	/** Returns the actual code that the code block refers to. */
	public String getCode() {
		return code.substring(indexInFile, endIndex);
	}

	@Override
	public Stackable clone() {
		return new CodeBlock(this.indexInFile, this.endIndex, this.code);
	}

	@Override
	@SuppressWarnings("deprecation")
	public CallProvider getCallProvider() {
		return (interpreter) -> {
			interpreter.internal.pushState();

			interpreter.internal.setRegion(indexInFile, endIndex - 1);
			interpreter.internal.setExecutionPos(indexInFile);

			while (interpreter.canExecute()) {
				interpreter.executeOnce();
			}

			interpreter.internal.popState();

			return null;
		};
	}

}
