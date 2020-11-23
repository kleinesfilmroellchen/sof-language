package klfr.sof.module;

import java.util.*;

import klfr.sof.*;
import klfr.sof.lang.*;
import klfr.sof.ast.*;
import klfr.sof.ast.PrimitiveTokenNode.PrimitiveToken;

/**
 * A module interpreter is a slightly modified interpreter that handles the
 * export keyword to store an exported binding. The exported bindings are then
 * accessible with the {@link ModuleInterpreter#getExports()} method.
 */
public class ModuleInterpreter extends Interpreter {
	private static final long serialVersionUID = 1L;

	private final Map<Identifier, Stackable> exports = new TreeMap<>();

	public ModuleInterpreter(IOInterface io, ModuleDiscoverer md) {
		super(io, md);
	}

	/**
	 * Retrieve all of the exports that this module interpreter collected so far.
	 * 
	 * @return A map, like an SOF nametable, containing the exported bindings.
	 */
	public Map<Identifier, Stackable> getExports() {
		return new TreeMap<>(exports);
	}

	@Override
	protected boolean handle(PrimitiveTokenNode pt) throws CompilerException {
		if (pt.symbol() == PrimitiveToken.Export) {
			final var exportId = this.stack.popTyped(Identifier.class);
			// use the calling methodology for future consistency
			this.doCall(exportId);
			final var exportBinding = this.stack.pop();
			this.exports.put(exportId, exportBinding);
			return true;
		} else if (pt.symbol() == PrimitiveToken.DefineExport_Sugar) {
			// does both globaldef and export
			final var id = this.stack.popTyped(Identifier.class);
			final var toBeDefined = this.stack.pop();
			this.stack.globalNametable().put(id, toBeDefined);
			this.exports.put(id, toBeDefined);
			return true;
		} else
			return super.handle(pt);
	}
	
}
