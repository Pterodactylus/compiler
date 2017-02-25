package ast;

/**
 * Created by Valentas on 10/19/2016.
 */
public class SizeOfExpr extends Expr {

    public final Type type;

    public SizeOfExpr(Type type) {
        this.type = type;
    }

    @Override
    public <T> T accept(ASTVisitor<T> v) {
        return v.visitSizeOfExpr(this);
    }
}
