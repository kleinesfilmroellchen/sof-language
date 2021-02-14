package klfr.sof;

import java.io.Serializable;
import java.io.File;
import klfr.sof.ast.*;

/**
 * An SOF source file or module, consisting of the file itself, the (unprocessed) source code and the parsed syntax tree.
 * For parsing purposes (circular dependency), the AST can be assigned with a setter as long as it is null.
 * Apart from that, this class is immutable.
 */
// just barely I can't use records here, ffs
public class SOFFile implements Serializable {
	private static final long serialVersionUID = 1L;
	
	/** The source file, purely symbolic. */
	private final File sourceFile;
	/** The code that was compiled to the AST */
	private final String code;
	/** The AST that was compiled from the code. This is not final on purpose. */
	private Node ast;

	/**
	 * Create a new SOF file.
	 * @param sourceFile The real file that the code comes from.
	 * @param code The unprocessed source code.
	 * @param ast The AST that was compiled from the source code.
	 */
	public SOFFile(final File sourceFile, final String code, final Node ast) {
		this.sourceFile = sourceFile;
		this.code = code;
		this.ast = ast;
	}

	/**
	 * The actual file where this SOF file's code comes from.
	 * May be symbolic, because it is never read.
	 * Therefore, this method makes no guarantees that this file exists.
	 * @return The Java file where this code comes from.
	 */
	public File sourceFile() {
		return sourceFile;
	}

	/**
	 * Returns the unedited source code that was compiled into this file's AST.
	 * @return The unedited source code that was compiled into this file's AST.
	 */
	public String code() {
		return code;
	}

	/**
	 * Returns the Abstract Syntax Tree (AST) that was compiled from this file's source code.
	 * @return The Abstract Syntax Tree (AST) that was compiled from this file's source code.
	 */
	public Node ast() {
		return ast;
	}

	/**
	 * Set the AST of this file.
	 * <strong>Will not set the AST of this SOFFile if it was already set before.</strong>
	 * <br/><br/>
	 * Implementation note: This is necessary to resolve the double-dependency issue in Parser.
	 * 
	 * @param ast The node to use as the AST.
	 */
	public void setAST(Node ast) {
		if (this.ast == null)
			this.ast = ast;
	}

	@Override
	public String toString() {
		return "SOFFile(" + sourceFile.toString() + ", '" + code.substring(0, Math.min(code.length(), 15)) + "', " + ast + ")";
	}
	
}
