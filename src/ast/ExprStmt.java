package ast;

/**
 * Created by Valentas on 10/17/2016.
 */
public class ExprStmt extends Stmt {

    public final Expr exprStmt;

    public ExprStmt(Expr exprStmt) {
        this.exprStmt = exprStmt;
    }

    @Override
    public <T> T accept(ASTVisitor<T> v) {
        return v.visitExprStmt(this);
    }
}
