package ast;

/**
 * Created by Valentas on 10/17/2016.
 */
public class BinOp extends Expr {

    public final Expr lhs;
    public Op op;
    public final Expr rhs;

    public BinOp(Expr lhs, Op op, Expr rhs) {
        this.lhs = lhs;
        this.op = op;
        this.rhs = rhs;
    }

    @Override
    public <T> T accept(ASTVisitor<T> v) {
        return v.visitBinOpExpr(this);
    }
}
