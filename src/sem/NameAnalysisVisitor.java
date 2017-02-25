package sem;

import ast.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

public class NameAnalysisVisitor extends BaseSemanticVisitor<Void> {

    private Scope scope;

	@Override
	public Void visitBaseType(BaseType bt) {
		return null;
	}


	/**
	 * Visiting struct happens in 2 cases.
	 *   1. Visiting struct declaration. In this case, we must have at least one
	 *      var decl inside struct scope.
	 *   2. Visiting struct as var declaration. It can only be declared if it was defined
	 *      before. So case 1. must happen before 2. can take place.
	 *
	 *   1. happens only at the start of program. We are also not allowed to re-declare struct
	 *      and any var declarations inside struct that are of type struct type must have that type
	 *      of struct already declared. C is not Java, it does not reorder program.
	 * @param st
	 * @return
	 */
	@Override
	public Void visitStructType(StructType st) {

		String structName = st.structName;

		// When struct does not have any var declarations, it is struct declaration, case 1.
		if (!st.structVarDecls.isEmpty()) {
			// Make sure that global scope does not have struct named like structName already.
			if (scope.lookupCurrent(structName) != null) {
				error("Struct " + structName + " was already declared before.");
				return null;
			}

			// Define struct symbol and a scope of it. Make it point to global file scope.
			StructDeclSymbol structDeclSymbol = new StructDeclSymbol(structName, scope);

			// Add new struct declaration symbol to the global scope.
			scope.put(structDeclSymbol);

			// Parse the var declaration(s) inside the struct declaration. To do this, save
			// current scope, switch to struct scope, add all var declarations, then restore the
			// scope as it was before.

			Scope oldScope = scope;
			scope = structDeclSymbol.structScope;

			for (VarDecl structVarDecl : st.structVarDecls) {
				// Check if var declaration inside struct was not declared before.
				if (wasDeclaredBefore(structVarDecl.varName)) {
					error("VarDecl " + structVarDecl.varName + " was already declared before.");
					break; // Leave the VarDecl processing
				}
				structVarDecl.accept(this);
			}
			scope = oldScope;
		} else {
			// TODO: case 2, where we have struct definition as var decl somewhere in the program.

			// Make sure that struct definition only occurs if it was declared before in current or upper scopes.
			if (scope.lookup(structName) == null) {
				error("VarDecl of StructType " + structName + " was not declared at any other scope.");
				return null;
			}
		}

        return null;
	}

	private boolean wasDeclaredBefore(String symbolAlias) {
		return scope.lookupCurrent(symbolAlias) != null;
	}

	/**
	 * Function block consists of var declarations and statements.
	 * @param b
	 * @return
	 */
	@Override
	public Void visitBlock(Block b) {


        Scope oldScope = scope;
        scope = new Scope(oldScope);

		// Current scope is block. Add all var declarations(if any) to the current scope.
		// Must check if var was not declared before as well as if any struct types are available.

		for (VarDecl functionBlockVarDecl : b.varDeclsInBlock) {
			if (wasDeclaredBefore(functionBlockVarDecl.varName)) {
				error("VarDecl " + functionBlockVarDecl.varName + " was declated in parameters list.");
				return null;
			}
			functionBlockVarDecl.accept(this);
		}

		// Done parsing var declarations. Now move on to statements. Still in the same scope.

		for (Stmt functionBlockStmts : b.stmtsInBlock) {
			functionBlockStmts.accept(this);
		}


		// Restore scope.
		scope = oldScope;

		return null;
	}

	/**
	 * Function declaration consists of function type, function name, parameters and
	 * the block of var declarations and statements. The block is a new scope to be dealt
	 * with so any var decls or statements go into new scope.
	 * @param fd
	 * @return
	 */
	@Override
	public Void visitFunDecl(FunDecl fd) {
		fd.type.accept(this);

		String functionName = fd.name;

		// Check if function name is not the same as some other symbol declared before.
		if (wasDeclaredBefore(functionName)) {
			error("Function declaration: " + functionName + " has a name that was declared before in this scope.");
			return null;
		}

		// Function name is suppsoed to be added to current scope. Function will also have
		// it's own scope and can refer to upper scope.
		FunDeclSymbol funDeclSymbol = new FunDeclSymbol(functionName, scope, fd);
		scope.put(funDeclSymbol);

		// Switch scopes. Then parse function arguments. All of them should be added to function
		// scope. Need to make sure that all params have unique names, i.e. was not declared in params list
		// before.
		Scope oldScope = scope;
		scope = funDeclSymbol.functionScope;

		for (VarDecl functionParam : fd.params) {
			if (wasDeclaredBefore(functionParam.varName)) {
				error("Function param " + functionParam.varName + " was declared before in function " + functionName);
				return null;
			}
			functionParam.accept(this);
		}

		// After parsing parameters, need to continue into function block.
        visitFunctionDeclarationBlock(fd.block);

		scope = oldScope;

		return null;
	}

	private Void visitFunctionDeclarationBlock(Block functionDeclBlock) {

        for (VarDecl functionVarDecl : functionDeclBlock.varDeclsInBlock) {
            if (wasDeclaredBefore(functionVarDecl.varName)) {
                error("VarDecl " + functionVarDecl.varName + " was declated in parameters list.");
                return null;
            }
            functionVarDecl.accept(this);
        }

        for (Stmt functionStmt : functionDeclBlock.stmtsInBlock) {
            if (functionStmt instanceof Block) {
                visitBlock((Block) functionStmt);
            } else {
                functionStmt.accept(this);
            }
        }

        return null;
    }


	@Override
	public Void visitProgram(Program p) {


		// Initialize global scope. This is the root of all scopes.
		scope = new Scope();

		// Include all minic lib functions.

		FunDecl print_s_decl = new FunDecl(BaseType.VOID, "print_s",
				Arrays.asList(new VarDecl(new PointerType(BaseType.CHAR), "s")), null);
		FunDeclSymbol print_s_symbol = new FunDeclSymbol("print_s", scope, print_s_decl);
		scope.put(print_s_symbol);

		FunDecl print_i_decl = new FunDecl(BaseType.INT, "print_i",
				Arrays.asList(new VarDecl(BaseType.INT, "i")), null);
		FunDeclSymbol print_i_symbol = new FunDeclSymbol("print_i", scope, print_i_decl);
		scope.put(print_i_symbol);

		FunDecl print_c_decl = new FunDecl(BaseType.CHAR, "print_c",
				Arrays.asList(new VarDecl(BaseType.CHAR, "c")), null);
		FunDeclSymbol print_c_symbol = new FunDeclSymbol("print_c", scope, print_c_decl);
		scope.put(print_c_symbol);

		FunDecl read_c_decl = new FunDecl(BaseType.CHAR, "read_c", new ArrayList<VarDecl>(), null);
		FunDeclSymbol read_c_symbol = new FunDeclSymbol("read_c", scope, read_c_decl);
		scope.put(read_c_symbol);

		FunDecl read_i_decl = new FunDecl(BaseType.INT, "read_i", new ArrayList<VarDecl>(), null);
		FunDeclSymbol read_i_symbol = new FunDeclSymbol("read_i", scope, read_i_decl);
		scope.put(read_i_symbol);

		FunDecl mcmalloc_decl = new FunDecl(new PointerType(BaseType.VOID), "mcmalloc",
				Arrays.asList(new VarDecl(BaseType.INT, "size")), null);
		FunDeclSymbol mcalloc_symbol = new FunDeclSymbol("mcmalloc", scope, mcmalloc_decl);
		scope.put(mcalloc_symbol);


        for (StructType st : p.structTypes) {
            st.accept(this);
        }

        for (VarDecl vd : p.varDecls) {
            vd.accept(this);
        }

        for (FunDecl fd : p.funDecls) {
            fd.accept(this);
        }


        return null;
	}

	private void printSymbolTable() {
		Map<String, Symbol> symbolTable = scope.getSymbolTable();
		for (Map.Entry<String, Symbol> entry : symbolTable.entrySet()) {
			System.out.println(entry.getKey() + " " + entry.getValue());
		}
		System.out.println("===END OF ENTRIES IN SYMBOL TABLE===\n");
	}

	/**
	 * Var declaration processing involves visiting it's type and var declaration name.
	 * Then create a VarDeclSymbol and add it to the current scope. Calling visitor methods
	 * should take care of the right current scope as well as checking beforehand whether
	 * variable was declared before.
	 * @param vd
	 * @return
	 */
	@Override
	public Void visitVarDecl(VarDecl vd) {
        vd.type.accept(this);
		String varDeclName = vd.varName;

		// Check if varDeclName was already used in this scope somewhere else.
		if (wasDeclaredBefore(varDeclName)) {
			error("Cannot declare variable " + varDeclName + " because it's name was already used in this scope.");
			return null;
		}

		VarDeclSymbol varDeclSymbol = new VarDeclSymbol(varDeclName, vd);
		scope.put(varDeclSymbol);
        return null;
	}

	@Override
	public Void visitVarExpr(VarExpr v) {
		Symbol symbol = scope.lookup(v.name);
		if (scope.lookup(v.name) == null) {
			error("VarExpr " + v.name + " was not declared anywhere.");
			return null;
		}

		// Fill in var decl.
		v.vd = ((VarDeclSymbol) symbol).vd;
		// All is good.
		return null;
	}

	@Override
	public Void visitArrayType(ArrayType at) {
		return null;
	}

	@Override
	public Void visitPointerType(PointerType pt) {
		return null;
	}

	@Override
	public Void visitIntLiteral(IntLiteral il) {
		return null;
	}

	@Override
	public Void visitChrLiteral(ChrLiteral cl) {
		return null;
	}

	@Override
	public Void visitStrLiteral(StrLiteral sl) {
		return null;
	}

	/**
	 * Check if function being called exists somewhere up the scope hierarchy.
	 * This seems to indicate that we cannot define mutual recursion.
	 *
	 * def f() {g()}
	 * def g() {f()}
	 * @param fce
	 * @return
	 */
	@Override
	public Void visitFunCallExpr(FunCallExpr fce) {
		String functionName = fce.functionName;
		Symbol symbol = scope.lookup(functionName);
		if (symbol == null) {
			error("Function call " + functionName + " invalid because it was not defined before use.");
			return null;
		} else if (!(symbol instanceof FunDeclSymbol)) {
			error("Function name was declared before as non-function symbol.");
			return null;
		}

		// Now check if function call args are either terminals or declared before.
		for (Expr functionArg : fce.functionArgsExprs) {
			functionArg.accept(this);
		}
		fce.fd = ((FunDeclSymbol) symbol).fd;
		return null;
	}

	@Override
	public Void visitBinOpExpr(BinOp binOp) {
		Expr lhs = binOp.lhs;
		lhs.accept(this);
		Expr rhs = binOp.rhs;
		rhs.accept(this);
		return null;
	}

	@Override
	public Void visitStmtExpr(Stmt stmt) {
		return null;
	}

	@Override
	public Void visitIfStmt(If ifStmt) {
		ifStmt.ifConditionExpr.accept(this);
		ifStmt.ifStmtBlock.accept(this);
		if (ifStmt.elseStmtBlock != null) {
			ifStmt.elseStmtBlock.accept(this);
		}
		return null;
	}

	@Override
	public Void visitWhileStmt(While whileStmt) {
		whileStmt.whileConditionExpr.accept(this);
		whileStmt.whileStmt.accept(this);
		return null;
	}

	@Override
	public Void visitAssignStmt(Assign assignStmt) {
		assignStmt.lhs.accept(this);
		assignStmt.rhs.accept(this);
		return null;
	}

	@Override
	public Void visitReturnStmt(Return returnStmt) {
		if (returnStmt.returnExpr != null) {
			returnStmt.returnExpr.accept(this);
		}
		return null;
	}

	@Override
	public Void visitExprStmt(ExprStmt exprStmt) {
		exprStmt.exprStmt.accept(this);
		return null;
	}

	@Override
	public Void visitArrayAccessExpr(ArrayAccessExpr arrayAccessExpr) {
        arrayAccessExpr.arrayName.accept(this);

		return null;
	}

	@Override
	public Void visitSizeOfExpr(SizeOfExpr sizeOfExpr) {
		sizeOfExpr.type.accept(this);
		return null;
	}

	@Override
	public Void visitValueAtExpr(ValueAtExpr valueAtExpr) {
		valueAtExpr.valueAtExp.accept(this);
		return null;
	}

	@Override
	public Void visitTypecastExpr(TypecastExpr typecastExpr) {
		typecastExpr.type.accept(this);

		typecastExpr.typecastedExpr.accept(this);
		return null;
	}

	@Override
	public Void visitFieldAccessExpr(FieldAccessExpr fieldAccessExpr) {
		fieldAccessExpr.fieldAccessExpr.accept(this);
		String fieldName = fieldAccessExpr.fieldAccessName;

		return null;
	}


}
