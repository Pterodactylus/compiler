package ast;

public interface ASTVisitor<T> {
    public T visitBaseType(BaseType bt);
    public T visitStructType(StructType st);
    public T visitBlock(Block b);
    public T visitFunDecl(FunDecl p);
    public T visitProgram(Program p);
    public T visitVarDecl(VarDecl vd);
    public T visitVarExpr(VarExpr v);
    public T visitArrayType(ArrayType at);
    public T visitPointerType(PointerType pt);
    public T visitIntLiteral(IntLiteral il);
    public T visitChrLiteral(ChrLiteral cl);
    public T visitStrLiteral(StrLiteral sl);
    public T visitFunCallExpr(FunCallExpr fce);
    public T visitBinOpExpr(BinOp binOp);
    public T visitStmtExpr(Stmt stmt);
    public T visitIfStmt(If ifStmt);
    public T visitWhileStmt(While whileStmt);
    public T visitAssignStmt(Assign assignStmt);
    public T visitReturnStmt(Return returnStmt);
    public T visitExprStmt(ExprStmt exprStmt);
    public T visitArrayAccessExpr(ArrayAccessExpr arrayAccessExpr);
    public T visitSizeOfExpr(SizeOfExpr sizeOfExpr);
    public T visitValueAtExpr(ValueAtExpr valueAtExpr);
    public T visitTypecastExpr(TypecastExpr typecastExpr);
    public T visitFieldAccessExpr(FieldAccessExpr fieldAccessExpr);

    // to complete ... (should have one visit method for each concrete AST node class)
}
