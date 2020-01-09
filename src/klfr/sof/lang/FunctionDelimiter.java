package klfr.sof.lang;

/**
 * A special nametable that marks the end of a function scope, also called FNT
 * 
 * @author klfr
 * @version 0.1a1
 *
 */
public class FunctionDelimiter extends Nametable {
	private static final long serialVersionUID = 1L;
	public Stackable returnValue = null;
}
