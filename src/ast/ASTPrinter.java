package ast;

import java.io.PrintWriter;

public class ASTPrinter implements ASTVisitor<Void> {

    private PrintWriter writer;

    public ASTPrinter(PrintWriter writer) {
            this.writer = writer;
    }

    @Override
    public Void visitBlock(Block b) {
        writer.print("Block(");
        String delimiter = "";
        for (VarDecl vd : b.varDeclsInBlock) {
            writer.print(delimiter);
            vd.accept(this);
            delimiter = ",";
        }
        for (Stmt stmt : b.stmtsInBlock) {
            writer.print(delimiter);
            stmt.accept(this);
            delimiter = ",";
        }
        writer.print(")");
        return null;
    }

    @Override
    public Void visitFunDecl(FunDecl fd) {
        writer.print("FunDecl(");
        fd.type.accept(this);
        writer.print(","+fd.name+",");
        for (VarDecl vd : fd.params) {
            vd.accept(this);
            writer.print(",");
        }
        fd.block.accept(this);
        writer.print(")");
        return null;
    }

    @Override
    public Void visitProgram(Program p) {
        writer.print("Program(");
        String delimiter = "";
        for (StructType st : p.structTypes) {
            writer.print(delimiter);
            delimiter = ",";
            st.accept(this);
        }
        for (VarDecl vd : p.varDecls) {
            writer.print(delimiter);
            delimiter = ",";
            vd.accept(this);
        }
        for (FunDecl fd : p.funDecls) {
            writer.print(delimiter);
            delimiter = ",";
            fd.accept(this);
        }
        writer.print(")");
	    writer.flush();
        return null;
    }

    @Override
    public Void visitVarDecl(VarDecl vd){
        writer.print("VarDecl(");
        vd.type.accept(this);
        writer.print(",");
        writer.print(vd.varName);
        writer.print(")");
        return null;
    }

    @Override
    public Void visitVarExpr(VarExpr v) {
        writer.print("VarExpr(");
        writer.print(v.name);
        writer.print(")");
        return null;
    }

    @Override
    public Void visitBaseType(BaseType bt) {
        writer.print(bt);
        writer.flush();
        return null;
    }

    @Override
    public Void visitStructType(StructType st) {
        writer.print("StructType(");
        writer.print(st.structName);
        String delimiter = ",";
        for (VarDecl vd : st.structVarDecls) {
            writer.print(delimiter);
            vd.accept(this);
        }
        writer.print(")");
        return null;
    }

    @Override
    public Void visitArrayType(ArrayType at) {
        writer.print("ArrayType(");
        at.type.accept(this);
        writer.print(",");
        writer.print(at.numElements);
        writer.print(")");
        return null;
    }

    @Override
    public Void visitPointerType(PointerType pt) {
        writer.print("PointerType(");
        pt.type.accept(this);
        writer.print(")");
        return null;
    }

    @Override
    public Void visitIntLiteral(IntLiteral il) {
        writer.print("IntLiteral(");
        writer.print(il.value);
        writer.print(")");
        return null;
    }

    @Override
    public Void visitChrLiteral(ChrLiteral cl) {
        writer.print("ChrLiteral(");
        writer.print(cl.value);
        writer.print(")");
        return null;
    }

    @Override
    public Void visitStrLiteral(StrLiteral sl) {
        writer.print("StrLiteral(");
        writer.print(sl.value);
        writer.print(")");
        return null;
    }

    @Override
    public Void visitFunCallExpr(FunCallExpr fce) {
        writer.print("FunCallExpr(");
        writer.print(fce.functionName);
        String delimiter = ",";
        for (Expr funcallArgExpr : fce.functionArgsExprs) {
            writer.print(delimiter);
            funcallArgExpr.accept(this);
            delimiter = ",";
        }
        writer.print(")");
        return null;
    }

    @Override
    public Void visitBinOpExpr(BinOp binOp) {
        writer.print("BinOp(");
        binOp.lhs.accept(this);
        writer.print(",");
        writer.print(binOp.op);
        writer.print(",");
        binOp.rhs.accept(this);
        writer.print(")");
        return null;
    }

    @Override
    public Void visitStmtExpr(Stmt stmt) {
        writer.print("Stmt(");

        writer.print(")");
        return null;
    }

    @Override
    public Void visitIfStmt(If ifStmt) {
        writer.print("If(");
        ifStmt.ifConditionExpr.accept(this);
        writer.print(",");
        ifStmt.ifStmtBlock.accept(this);
        if (ifStmt.elseStmtBlock != null) {
            writer.print(",");
            ifStmt.elseStmtBlock.accept(this);
        }
        writer.print(")");
        return null;
    }

    @Override
    public Void visitWhileStmt(While whileStmt) {
        writer.print("While(");
        whileStmt.whileConditionExpr.accept(this);
        writer.print(",");
        whileStmt.whileStmt.accept(this);
        writer.print(")");
        return null;
    }

    @Override
    public Void visitAssignStmt(Assign assignStmt) {
        writer.print("Assign(");
        assignStmt.lhs.accept(this);
        writer.print(",");
        assignStmt.rhs.accept(this);
        writer.print(")");
        return null;
    }

    @Override
    public Void visitReturnStmt(Return returnStmt) {
        writer.print("Return(");
        if (returnStmt.returnExpr == null) {
            writer.print("");
        } else {
            returnStmt.returnExpr.accept(this);
        }
        writer.print(")");
        return null;
    }

    @Override
    public Void visitExprStmt(ExprStmt exprStmt) {
        writer.print("ExprStmt(");
        exprStmt.exprStmt.accept(this);
        writer.print(")");
        return null;
    }

    @Override
    public Void visitArrayAccessExpr(ArrayAccessExpr arrayAccessExpr) {
        writer.print("ArrayAccessExpr(");
        arrayAccessExpr.arrayName.accept(this);
        writer.print(",");
        arrayAccessExpr.arrayIndex.accept(this);
        writer.print(")");
        return null;
    }

    @Override
    public Void visitSizeOfExpr(SizeOfExpr sizeOfExpr) {
        writer.print("SizeOfExpr(");
        sizeOfExpr.type.accept(this);
        writer.print(")");
        return null;
    }

    @Override
    public Void visitValueAtExpr(ValueAtExpr valueAtExpr) {
        writer.print("ValueAtExpr(");
        valueAtExpr.valueAtExp.accept(this);
        writer.print(")");
        return null;
    }

    @Override
    public Void visitTypecastExpr(TypecastExpr typecastExpr) {
        writer.print("TypecastExpr(");
        typecastExpr.type.accept(this);
        writer.print(",");
        typecastExpr.typecastedExpr.accept(this);
        writer.print(")");
        return null;
    }

    @Override
    public Void visitFieldAccessExpr(FieldAccessExpr fieldAccessExpr) {
        writer.print("FieldAccessExpr(");
        fieldAccessExpr.fieldAccessExpr.accept(this);
        writer.print(",");
        writer.print(fieldAccessExpr.fieldAccessName);
        writer.print(")");
        return null;
    }
}
