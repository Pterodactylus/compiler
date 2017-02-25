package ast;

/**
 * Created by Valentas on 10/17/2016.
 */
public class Return extends Stmt {

    public final Expr returnExpr;

    public Return(Expr returnExpr) {
        this.returnExpr = returnExpr;
    }

    @Override
    public <T> T accept(ASTVisitor<T> v) {
        return v.visitReturnStmt(this);
    }

}
