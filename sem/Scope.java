package sem;

import java.util.HashMap;
import java.util.Map;

public class Scope {
	private Scope outer;
	private Map<String, Symbol> symbolTable;
	
	public Scope(Scope outer) { 
		this.outer = outer;
		symbolTable = new HashMap<>();
	}
	
	public Scope() { this(null); }
	
	public Symbol lookup(String name) {
		// Check if current scope contains the symbol alias.
		Symbol symbolInCurrentScope = lookupCurrent(name);
		if (symbolInCurrentScope != null) {
			return symbolInCurrentScope;
		}

		// Otherwise, keep looking at upper scopes.
		Scope upper = outer;
		while (upper != null) {
			Symbol lookedUpSymbol = upper.lookupCurrent(name);
			if (lookedUpSymbol != null) {
				return lookedUpSymbol;
			}
			upper = upper.outer;
		}

		// Symbol was not found in any scope.
		return null;
	}
	
	public Symbol lookupCurrent(String name) {
		if (symbolTable.containsKey(name)) {
			return symbolTable.get(name);
		}
		return null;
	}
	
	public void put(Symbol sym) {
		symbolTable.put(sym.name, sym);
	}

	public Map<String, Symbol> getSymbolTable() {
		return symbolTable;
	}
}
