package ast;

/**
 * Created by Valentas on 10/19/2016.
 */
public class FieldAccessExpr extends Expr {

    public final Expr fieldAccessExpr;
    public final String fieldAccessName;

    public FieldAccessExpr(Expr fieldAccessExpr, String fieldAccessName) {
        this.fieldAccessExpr = fieldAccessExpr;
        this.fieldAccessName = fieldAccessName;
    }

    @Override
    public <T> T accept(ASTVisitor<T> v) {
        return v.visitFieldAccessExpr(this);
    }
}
