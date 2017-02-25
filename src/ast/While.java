package ast;

/**
 * Created by Valentas on 10/17/2016.
 */
public class While extends Stmt {

    public final Expr whileConditionExpr;
    public final Stmt whileStmt;

    public While(Expr whileConditionExpr, Stmt whileStmt) {
        this.whileConditionExpr = whileConditionExpr;
        this.whileStmt = whileStmt;
    }

    @Override
    public <T> T accept(ASTVisitor<T> v) {
        return v.visitWhileStmt(this);
    }
}
