package ast;

import java.util.List;

/**
 * Created by Valentas on 10/17/2016.
 */
public class FunCallExpr extends Expr {

    public final String functionName;
    public final List<Expr> functionArgsExprs;

    public FunDecl fd; // filled in by name analyzer.

    public FunCallExpr(String functionName, List<Expr> functionArgsExprs) {
        this.functionName = functionName;
        this.functionArgsExprs = functionArgsExprs;
    }

    @Override
    public <T> T accept(ASTVisitor<T> v) {
        return v.visitFunCallExpr(this);
    }
}
