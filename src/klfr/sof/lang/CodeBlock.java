package klfr.sof.lang;

@StackableName("Code block")
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
	public String toDebugString(DebugStringExtensiveness e) {
		switch (e) {
			case Full:
				return String.format("[CodeBlock { %s } %h]", getCode(), hashCode());
			case Compact:
				return "[CodeBlk { " + (code.length() > 16 ? (code.substring(0, 12) + " ...") : code) + " } ]";
			default:
				return Stackable.toDebugString(this, e);
		}
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
