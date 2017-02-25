package ast;

import java.util.List;


public class StructType implements Type {

    public final String structName;
    public final List<VarDecl> structVarDecls;

    public StructType(String structName, List<VarDecl> structVarDecls) {
        this.structName = structName;
        this.structVarDecls = structVarDecls;
    }

    public <T> T accept(ASTVisitor<T> v) {
        return v.visitStructType(this);
    }

    @Override
    public int getSize() {
        return 0;
    }
}
