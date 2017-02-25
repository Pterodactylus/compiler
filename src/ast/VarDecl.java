package ast;

public class VarDecl implements ASTNode {
    public final Type type;
    public final String varName;

    public boolean isGlobal; // Filled in by TypeChecker.
    public boolean isParameter; // Filled in by TypeChecker.

    public int fpOffset; // Filled in by CodeGenerator. Access function params.

    public int offset; // Filled in by CodeGenerator. Access local variables.

    public VarDecl(Type type, String varName) {
	    this.type = type;
	    this.varName = varName;
    }

    public <T> T accept(ASTVisitor<T> v) {
	return v.visitVarDecl(this);
    }
}
