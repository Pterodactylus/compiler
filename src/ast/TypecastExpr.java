package ast;

/**
 * Created by Valentas on 10/19/2016.
 */
public class TypecastExpr extends Expr {

    public final Type type;
    public final Expr typecastedExpr;

    public TypecastExpr(Type type, Expr typecastedExpr) {
        this.type = type;
        this.typecastedExpr = typecastedExpr;
    }

    @Override
    public <T> T accept(ASTVisitor<T> v) {
        return v.visitTypecastExpr(this);
    }
}
