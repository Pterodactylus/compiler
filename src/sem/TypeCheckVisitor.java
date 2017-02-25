package sem;

import ast.*;

import java.util.ArrayList;
import java.util.Arrays;

public class TypeCheckVisitor extends BaseSemanticVisitor<Type> {

	/**
	 * This is used when we have a return statement inside a function block.
	 * When function declaration is encountered, we need to set this variable to
	 * the type of FunDecl. Then compare it's value if we encounter return statement inside the block.
	 */
	private Type currentFunctionReturnType;

	@Override
	public Type visitBaseType(BaseType bt) {
		return bt;
	}

	@Override
	public Type visitStructType(StructType st) {

		return null;
	}

	@Override
	public Type visitBlock(Block b) {

		// No need to go through VarDecl.
		for (VarDecl vd : b.varDeclsInBlock) {
			vd.accept(this);
		}

		// Go through statements in the block.
		for (Stmt blockStmt : b.stmtsInBlock) {
			blockStmt.accept(this);
		}
		return null;
	}

	@Override
	public Type visitFunDecl(FunDecl fd) {
		Type functionReturnType = fd.type;

		// Store function return type to environment variable.
		currentFunctionReturnType = functionReturnType;

		// No need to parse types because if there is a function call, then funcall will have
		// FunDecl associated with it to compare types.

		for (VarDecl vd : fd.params) {
			vd.accept(this);
			vd.isParameter = true; // Set parameter flag for CodeGenerator.
		}

		Type functionBlockType = fd.block.accept(this);

		// Check if function return type we just visited is not void. If it is not void and return
		// statement was not found, then it is an error.
		return functionBlockType;
	}


	@Override
	public Type visitProgram(Program p) {

		// Initialize type environment.
		currentFunctionReturnType = null;

		for (StructType st : p.structTypes) {
			st.accept(this);
		}

		for (VarDecl vd : p.varDecls) {
			vd.accept(this);
			vd.isGlobal = true; // Set global variables flag for CodeGenerator.
		}

		for (FunDecl fd : p.funDecls) {
			fd.accept(this);
		}

		FunDecl print_s_decl = new FunDecl(BaseType.VOID, "print_s",
				Arrays.asList(new VarDecl(new PointerType(BaseType.CHAR), "s")), null);
		FunDecl print_i_decl = new FunDecl(BaseType.VOID, "print_i",
				Arrays.asList(new VarDecl(BaseType.INT, "i")), null);
		FunDecl print_c_decl = new FunDecl(BaseType.VOID, "print_c",
				Arrays.asList(new VarDecl(BaseType.CHAR, "c")), null);
		FunDecl read_c_decl = new FunDecl(BaseType.CHAR, "read_c", new ArrayList<VarDecl>(), null);
		FunDecl read_i_decl = new FunDecl(BaseType.INT, "read_i", new ArrayList<VarDecl>(), null);
		p.funDecls.add(print_c_decl);
		p.funDecls.add(print_s_decl);
		p.funDecls.add(print_i_decl);
		p.funDecls.add(read_i_decl);
		p.funDecls.add(read_c_decl);

		return null;
	}

	@Override
	public Type visitVarDecl(VarDecl vd) {
		Type varDeclType = vd.type.accept(this);
//		System.out.println(varDeclType);
		if (varDeclType == BaseType.VOID) {
			error("Can't declare a variable of type void.");
			return null;
		} else if (varDeclType instanceof PointerType) {
			Type wherePointerPoints = ((PointerType) varDeclType).type.accept(this);
			if (wherePointerPoints == BaseType.VOID) {
				error("Can't declare a variable pointer of void type.");
				return null;
			}
		}
		return varDeclType;
	}

	@Override
	public Type visitVarExpr(VarExpr v) {
		if (v.vd == null) {
			error("VarExpr " + v + " was not declared before.");
			return null;
		}
		Type varExprType = v.vd.type;
		return varExprType;
	}

	@Override
	public Type visitArrayType(ArrayType at) {
		return at;
	}

	@Override
	public Type visitPointerType(PointerType pt) {
		return pt;
	}

	@Override
	public Type visitIntLiteral(IntLiteral il) {
		return BaseType.INT;
	}

	@Override
	public Type visitChrLiteral(ChrLiteral cl) {
		return BaseType.CHAR;
	}

	@Override
	public Type visitStrLiteral(StrLiteral sl) {
		return new ArrayType(BaseType.CHAR, sl.value.length() + 1);
	}

	@Override
	public Type visitFunCallExpr(FunCallExpr fce) {
		// Obtain reference to corresponding FunDecl.
		FunDecl fd = fce.fd;

		if (fd == null) {
			return null;
		}


		// Make sure number of arguments in declaration and call match.
		if (fce.functionArgsExprs.size() != fd.params.size()) {
			error("Function call have too few/many args.");
			return null;
		}

		// Make sure that types of fun call arguments match function declaration arguments.
		for (int i = 0; i < fce.functionArgsExprs.size(); i++) {
			Type functionCallArgType = fce.functionArgsExprs.get(i).accept(this);
			Type functionDeclArgType = fd.params.get(i).accept(this);
			if (!isSameType(functionCallArgType, functionDeclArgType)) {
				error("Function call " + fce.functionName + " arguments do not match it's declaration types.");
				return null;
			}
		}

		// Argument types match. Now obtain fun decl return type. Fill in type for funcall expr.
		Type functionDeclReturnType = fd.type;
		fce.type = functionDeclReturnType;
		return functionDeclReturnType;
    }

    private boolean isSameType(Type t1, Type t2) {
		if (t1 instanceof BaseType && t2 instanceof BaseType) {
			return t1 == t2;
		} else if (t1 instanceof PointerType && t2 instanceof PointerType) {
			Type ptr_t1 = ((PointerType) t1).type.accept(this);
			Type ptr_t2 = ((PointerType) t2).type.accept(this);
			return isSameType(ptr_t1, ptr_t2);
		} else if (t1 instanceof ArrayType && t2 instanceof ArrayType) {
			if (((ArrayType) t1).numElements != ((ArrayType) t2).numElements) {
				error("Arrays of not equal size");
				return false;
			}
			Type arr_t1 = ((ArrayType) t1).type.accept(this);
			Type arr_t2 = ((ArrayType) t2).type.accept(this);
			return isSameType(arr_t1, arr_t2);
		}
		return false;
	}

	@Override
	public Type visitBinOpExpr(BinOp binOp) {
		Type lhsType = binOp.lhs.accept(this);
		Type rhsType = binOp.rhs.accept(this);

		if (binOp.op == Op.NE || binOp.op == Op.EQ) {
			// This is the case for lhsType == rhsType
			if (isSameType(lhsType, rhsType)) {
				return BaseType.INT;
			} else {
				error("==, != lhs and rhs are of different types.");
				return null;
			}
		} else {
			// Arithmetic, logical ops, etc.
			if (lhsType == BaseType.INT && rhsType == BaseType.INT) {
				binOp.type = BaseType.INT;
				return BaseType.INT;
			}
			error(binOp.op + " is only defined for ints.");
			return null;
		}

	}

	@Override
	public Type visitStmtExpr(Stmt stmt) {
		return null;
	}

	@Override
	public Type visitIfStmt(If ifStmt) {
		Type ifCondType = ifStmt.ifConditionExpr.accept(this);
		if (!isSameType(ifCondType, BaseType.INT)) {
			error("If condition should have int in it.");
			return null;
		}

		ifStmt.ifStmtBlock.accept(this);
		if (ifStmt.elseStmtBlock != null) {
			ifStmt.elseStmtBlock.accept(this);
		}

		return null;
	}

	@Override
	public Type visitWhileStmt(While whileStmt) {
		Type whileCondType = whileStmt.whileConditionExpr.accept(this);
		if (!isSameType(whileCondType, BaseType.INT)) {
			error("While condition should have int in it.");
			return null;
		}

		whileStmt.whileConditionExpr.accept(this);

		return null;
	}

	@Override
	public Type visitAssignStmt(Assign assignStmt) {
		Expr lhsExpr = assignStmt.lhs;
		if (!(lhsExpr instanceof VarExpr || lhsExpr instanceof ArrayAccessExpr ||
				lhsExpr instanceof FieldAccessExpr || lhsExpr instanceof ValueAtExpr)) {
			error("Assign to incompatbile type: " + lhsExpr);
			return null;
		}
		Type lhs = assignStmt.lhs.accept(this);
		if (lhs == BaseType.VOID || lhs instanceof ArrayType) {
			error("Assignment is impossible on type: " + lhs);
			return null;
		}

		Type rhs = assignStmt.rhs.accept(this);


		if (isSameType(lhs, rhs)) {
			return null;
		}

		error("Assignment failed because lhs is of type: " + lhs + " and rhs: " + rhs);
		return null;
	}

	@Override
	public Type visitReturnStmt(Return returnStmt) {
		// Obtain return type of current function block because we encountered return statement.
		Type functionReturnType = currentFunctionReturnType;


		// Obtain the type of return statement(if any is available)
		Type returnType = BaseType.VOID;
		if (returnStmt.returnExpr != null) {
			returnType = returnStmt.returnExpr.accept(this);
		}

		if (returnType == null) {
			return null;
		}


		if (isSameType(functionReturnType, returnType)) {
			// Return statement type match function return type.
			return functionReturnType;
		}

		error("Return statement has type " + returnType + " while function return type is " + functionReturnType);
		return null;
	}

	@Override
	public Type visitExprStmt(ExprStmt exprStmt) {
		exprStmt.exprStmt.accept(this);
		return null;
	}

	@Override
	public Type visitArrayAccessExpr(ArrayAccessExpr arrayAccessExpr) {
		Type arrayType = arrayAccessExpr.arrayName.accept(this);
		if (!(arrayType instanceof ArrayType || arrayType instanceof PointerType)) {
			error("Array is not if array type or pointer type.");
			return null;
		}

		// Grab the type inside array type.
		Type typeInsideArray = null;
		if (arrayType instanceof ArrayType) {
			typeInsideArray = ((ArrayType) arrayType).type.accept(this);
		} else {
			typeInsideArray = ((PointerType) arrayType).type.accept(this);
		}

		Type index = arrayAccessExpr.arrayIndex.accept(this);
		if (!(isSameType(index, BaseType.INT))) {
			error("Array index must be of type INT.");
			return null;

		}
		return typeInsideArray;

	}

	@Override
	public Type visitSizeOfExpr(SizeOfExpr sizeOfExpr) {
		return BaseType.INT;
	}

	@Override
	public Type visitValueAtExpr(ValueAtExpr valueAtExpr) {
		Type valueAtType = valueAtExpr.valueAtExp.accept(this);
		if (!(valueAtType instanceof PointerType)) {
			error("Value at is only defined for pointer types.");
			return null;
		}

		Type ptr_type = ((PointerType) valueAtType).type.accept(this);
		return ptr_type;

	}

	@Override
	public Type visitTypecastExpr(TypecastExpr typecastExpr) {
		Type cast = typecastExpr.type;
		Type toBeCasted = typecastExpr.typecastedExpr.accept(this);

		if (toBeCasted.equals(BaseType.CHAR)) {
			if (cast.equals(BaseType.INT)) {
				return BaseType.INT;
			} else {
				error("Can only cast to INT type.");
			}
		} else if (toBeCasted instanceof ArrayType) {
			// Extract array type.
			Type arrayType = ((ArrayType) toBeCasted).type;
			if (cast instanceof PointerType) {
				Type castType = ((PointerType) cast).type;
				if (isSameType(castType, arrayType)) {
					return new PointerType(castType);
				} else {
					error ("Casting error.");
				}
			}
		} else if (toBeCasted instanceof PointerType) {
			if (cast instanceof PointerType) {
				Type castType = ((PointerType) cast).type;
				return new PointerType(castType);
			}
		} else {
			error("Casting failed.");
		}
		return toBeCasted;
	}

	@Override
	public Type visitFieldAccessExpr(FieldAccessExpr fieldAccessExpr) {
		return null;
	}

	// To be completed...


}
