package ast;

/**
 * Created by Valentas on 10/17/2016.
 */
public class StrLiteral extends Expr {

    public final String value;

    public StrLiteral(String value) {
        this.value = value;
    }

    @Override
    public <T> T accept(ASTVisitor<T> v) {
        return v.visitStrLiteral(this);
    }

}
