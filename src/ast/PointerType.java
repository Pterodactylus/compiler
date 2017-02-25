package ast;

/**
 * Created by Valentas on 10/14/2016.
 */
public class PointerType implements Type {

    public final Type type;

    public PointerType(Type type) {
        this.type = type;
    }

    @Override
    public <T> T accept(ASTVisitor<T> v) {
        return v.visitPointerType(this);
    }

    @Override
    public int getSize() {
        return type.getSize();
    }
}
