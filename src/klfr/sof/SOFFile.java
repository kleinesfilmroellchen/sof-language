package klfr.sof;

import java.io.Serializable;
import java.io.File;
import klfr.sof.ast.*;

/**
 * An SOF source file or module, consisting of the file itself, the (unprocessed) source code and the parsed syntax tree.
 * For parsing purposes (circular dependency), the AST can be assigned with a setter as long as it is null.
 */
// just barely I can't use records here, ffs
public class SOFFile implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private final File sourceFile;
	private final String code;
	// is not final on purpose
	private Node ast;

	public SOFFile(File sourceFile, String code, Node ast) {
		this.sourceFile = sourceFile;
		this.code = code;
		this.ast = ast;
	}

	public File sourceFile() {
		return sourceFile;
	}

	public String code() {
		return code;
	}

	public Node ast() {
		return ast;
	}

	/**
	 * Will not set the AST of this SOFFile if it was already set before.
	 */
	public void setAST(Node ast) {
		if (this.ast == null)
			this.ast = ast;
	}
	
}
