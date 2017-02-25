package ast;

/**
 * Created by Valentas on 10/19/2016.
 */
public class ValueAtExpr extends Expr {

    public final Expr valueAtExp;

    public ValueAtExpr(Expr valueAtExp) {
        this.valueAtExp = valueAtExp;
    }

    @Override
    public <T> T accept(ASTVisitor<T> v) {
        return v.visitValueAtExpr(this);
    }
}
