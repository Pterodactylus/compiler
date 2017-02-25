package ast;

/**
 * Created by Valentas on 10/17/2016.
 */
public class If extends Stmt {

    public final Expr ifConditionExpr;
    public final Stmt ifStmtBlock;
    public Stmt elseStmtBlock;

    public If(Expr ifConditionExpr, Stmt ifStmtBlock, Stmt elseStmtBlock) {
        this.ifConditionExpr = ifConditionExpr;
        this.ifStmtBlock = ifStmtBlock;
        this.elseStmtBlock = elseStmtBlock;
    }

    @Override
    public <T> T accept(ASTVisitor<T> v) {
        return v.visitIfStmt(this);
    }
}
