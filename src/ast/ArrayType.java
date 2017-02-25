package ast;

/**
 * Created by Valentas on 10/14/2016.
 */
public class ArrayType implements Type {

    public final Type type;
    public final int numElements;

    public ArrayType(Type type, int numElements) {
        this.type = type;
        this.numElements = numElements;
    }

    @Override
    public <T> T accept(ASTVisitor<T> v) {
        return v.visitArrayType(this);
    }

    @Override
    public int getSize() {
        return 4 * type.getSize() * numElements;
    }
}
