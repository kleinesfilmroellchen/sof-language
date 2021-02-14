package klfr.sof.module;

import java.util.*;

import klfr.sof.*;
import klfr.sof.lang.*;
import klfr.sof.lib.NativeFunctionRegistry;
import klfr.sof.ast.*;
import klfr.sof.ast.PrimitiveTokenNode.PrimitiveToken;
import klfr.sof.exceptions.*;

/**
 * A module interpreter is a slightly modified interpreter that handles the
 * export keyword to store an exported binding. The exported bindings are then
 * accessible with the {@link ModuleInterpreter#getExports()} method.
 */
public class ModuleInterpreter extends Interpreter {
	private static final long serialVersionUID = 1L;

	/** The export bindings that are defined and later returned to the module importer. */
	private final Map<Identifier, Stackable> exports = new TreeMap<>();

	/**
	 * Create a new module interpreter.
	 * @param io The I/O interface that this module interpreter should use.
	 * @param md The module discoverer used by this module interpreter to in turn load other modules.
	 * @param registry The native function registry to execute native functions.
	 */
	public ModuleInterpreter(IOInterface io, ModuleDiscoverer md, NativeFunctionRegistry registry) {
		super(io, md, registry);
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
	protected boolean handle(PrimitiveTokenNode pt) throws CompilerException, IncompleteCompilerException {
		if (pt.symbol() == PrimitiveToken.Export) {
			final var exportId = this.stack.popTyped(Identifier.class);
			// use the calling methodology for future consistency
			this.doCall(exportId);
			final var exportBinding = this.stack.popSafe();
			this.exports.put(exportId, exportBinding);
			return true;
		} else if (pt.symbol() == PrimitiveToken.DefineExport_Sugar) {
			// does both globaldef and export
			final var id = this.stack.popTyped(Identifier.class);
			final var toBeDefined = this.stack.popSafe();
			this.stack.globalNametable().put(id, toBeDefined);
			this.exports.put(id, toBeDefined);
			return true;
		} else
			return super.handle(pt);
	}
	
}
