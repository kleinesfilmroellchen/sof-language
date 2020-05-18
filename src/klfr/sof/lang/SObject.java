package klfr.sof.lang;

public class SObject extends Nametable {

	private static final long serialVersionUID = 1L;
	private Identifier name;

	public String getName() {
		return name.getValue();
	}

	public SObject(Identifier name) {
		this.name = name;
	}

	@Override
	public String toDebugString(DebugStringExtensiveness e) {
		switch (e) {
			case Full:
				return "Object " + getName() + ":" + System.lineSeparator() + super.toDebugString(e);
			case Compact:
				return "Obj(" + super.toDebugString(e) + ")";
			default:
				return super.toDebugString(e);
		}
	}

}
