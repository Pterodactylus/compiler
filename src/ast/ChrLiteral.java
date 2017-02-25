package ast;

/**
 * Created by Valentas on 10/17/2016.
 */
public class ChrLiteral extends Expr {

    public final char value;

    public ChrLiteral(char value) {
        this.value = value;
    }

    @Override
    public <T> T accept(ASTVisitor<T> v) {
        return v.visitChrLiteral(this);
    }

}
