package ast;

/**
 * Created by Valentas on 10/19/2016.
 */
public class ArrayAccessExpr extends Expr {

    public final Expr arrayName;
    public final Expr arrayIndex;

    public ArrayAccessExpr(Expr arrayName, Expr arrayIndex) {
        this.arrayName = arrayName;
        this.arrayIndex = arrayIndex;
    }

    @Override
    public <T> T accept(ASTVisitor<T> v) {
        return v.visitArrayAccessExpr(this);
    }
}
