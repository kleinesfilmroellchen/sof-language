package klfr.sof.lang;

import klfr.sof.ast.TokenListNode;

/**
 * Function type, one of the most important callable types. Functions are the
 * most primitive scoped callable.
 */
public class SOFunction extends CodeBlock {

    private static final long serialVersionUID = 1L;

    protected final int arguments;

    /**
     * Create a function with this code, with given numbers of arguments.
     * 
     * @param code      code of the function
     * @param arguments number of arguments the function recieves
     */
    public SOFunction(TokenListNode code, int arguments) {
        super(code);
        this.arguments = arguments;
    }

    @Override
    public String toDebugString(DebugStringExtensiveness e) {
        switch (e) {
            case Compact:
                return String.format("[Function/%d %dn ]", this.arguments, this.code.count());
            case Full:
                return String.format("[Function/%d { %s } %h]", this.arguments, this.code, this.hashCode());
            case Type:
                return "Function";
            default:
                return Stackable.toDebugString(this, e);
        }

    }

    @Override
    public String print() {
        return String.format("{ %d argument Function }", this.arguments);
    }

    @Override
    public Stackable clone() {
        return new SOFunction(this.code, this.arguments);
    }

    public static SOFunction fromCodeBlock(CodeBlock origin, int arguments) {
        return new SOFunction(origin.code, arguments);
    }

}