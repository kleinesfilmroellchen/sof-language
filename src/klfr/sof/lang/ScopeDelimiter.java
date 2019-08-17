package klfr.sof.lang;

/**
 * Special type of Nametable that marks the end of a local scope, as denoted by code blocks ( <code>{}</code> ).<br><br>
 * Any stack access cannot exceed a scope delimiter.<br><br>
 * The scope delimiter also stores the local names (as it is a nametable itself).
 * @author klfr
 */
public class ScopeDelimiter extends Nametable {
}
