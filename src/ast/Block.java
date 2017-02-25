package ast;

import java.util.List;

public class Block extends Stmt {

    public final List<VarDecl> varDeclsInBlock;
    public final List<Stmt> stmtsInBlock;

    public Block(List<VarDecl> varDeclsInBlock, List<Stmt> stmtsInBlock) {
        this.varDeclsInBlock = varDeclsInBlock;
        this.stmtsInBlock = stmtsInBlock;
    }

    public <T> T accept(ASTVisitor<T> v) {
	    return v.visitBlock(this);
    }
}
