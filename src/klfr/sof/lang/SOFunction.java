package klfr.sof.lang;

import java.util.LinkedList;

/**
 * Function type, one of the most important callable types.
 * Functions are the most primitive scoped callable.
 */
public class SOFunction extends CodeBlock {

    private static final long serialVersionUID = 1L;

    protected final int arguments;

    /**
     * Create a function with start and end indices referring to the code, with
     * given numbers of arguments
     * 
     * @param startIndex index in the code where the function starts
     * @param endIndex   index in the code where the function ends (exclusive)
     * @param code       code of the entire file containing the function
     * @param arguments  number of arguments the function recieves
     */
    public SOFunction(int startIndex, int endIndex, String code, int arguments) {
        super(startIndex, endIndex, code);
        this.arguments = arguments;
    }

    @Override
	@SuppressWarnings("deprecation")
    public CallProvider getCallProvider() {
        return interpreter -> {
			interpreter.internal.pushState();

			interpreter.internal.setRegion(indexInFile, endIndex-1);
            interpreter.internal.setExecutionPos(indexInFile);

            var stack = interpreter.internal.stack();

            // pop arguments and store temporarily
            var args = new LinkedList<Stackable>();
            for (var i = 0; i<arguments; ++i) {
                args.add(stack.pop());
            }
            
            // create the FNT
            var fnt = new FunctionDelimiter();
            // place it followed by the arguments on the stack
            stack.push(fnt);
            var it = args.descendingIterator();
            while (it.hasNext()) stack.push(it.next());
            
            // execute
			while (interpreter.canExecute()) {
				interpreter.executeOnce();
			}

            // clean up: remove all elements above the fnt
            Stackable current = null;
            while (current != fnt)
                current = stack.forcePop();

            interpreter.internal.popState();
            
            // is set by the return PT
            return fnt.returnValue;

        };
    }

    @Override
    public String getDebugDisplay() {
        return String.format("Function/%d{", this.arguments) + this.getCode() + "}";
    }

    @Override
    public String toString() {
        return "[Function " + this.hashCode() + "]";
    }

    @Override
    public Stackable clone() {
        return new SOFunction(this.indexInFile, this.endIndex, this.code, this.arguments);
    }
    public static SOFunction fromCodeBlock(CodeBlock origin, int arguments) {
        return new SOFunction(origin.indexInFile, origin.endIndex, origin.code, arguments);
    }

    
}