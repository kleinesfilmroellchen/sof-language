package klfr.sof.lang;

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
			interpreter.pushState();
			
			// set the region
			interpreter.setRegion(indexInFile, endIndex);
			
			while (interpreter.canExecute()) {
				interpreter.executeOnce();
			}
			interpreter.popState();
			
			return null;
		};
	}

}
