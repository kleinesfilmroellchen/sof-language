package klfr.sof.lang;

import java.util.Optional;

/**
 * A special nametable that marks the end of a function scope, also called FNT
 * 
 * @author klfr
 * @version 0.1a1
 *
 */
public class FunctionDelimiter extends Nametable {
	private static final long serialVersionUID = 1L;
	public Optional<Stackable> returnValue = Optional.empty();

	/**
	 * Pushes the return value of this function delimiter to the given stack. If
	 * this function delimiter never recieved a return value, don't push anything.
	 * 
	 * @param toPushTo The stack to modify.
	 * @return Whether the stack was modified, i.e. whether there was a return
	 *         value.
	 */
	public boolean pushReturnValue(Stack toPushTo) {
		if (returnValue.isPresent()) {
			toPushTo.push(returnValue.get());
			return true;
		}
		return false;
	}
}
