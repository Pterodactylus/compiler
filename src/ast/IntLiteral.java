package ast;

/**
 * Created by Valentas on 10/17/2016.
 */
public class IntLiteral extends Expr {

    public final int value;

    public IntLiteral(int value) {
        this.value = value;
    }

    @Override
    public <T> T accept(ASTVisitor<T> v) {
        return v.visitIntLiteral(this);
    }
}
