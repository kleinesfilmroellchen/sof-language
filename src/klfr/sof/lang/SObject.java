package klfr.sof.lang;

public class SObject extends Nametable {

	private Identifier name;

	public String getName() {
		return name.getValue();
	}

	public SObject(Identifier name) {
		this.name = name;
	}

	public String getDebugDisplay() {
		return getName() + ":" + System.lineSeparator() + super.getDebugDisplay();
	}

	public String toString() {
		return "[" + getName() + " Object]";
	}

}
