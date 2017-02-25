package sem;

import ast.VarDecl;

/**
 * Created by Valentas on 10/22/2016.
 */
public class VarDeclSymbol extends Symbol {

    public VarDecl vd;

    public VarDeclSymbol(String name, VarDecl vd) {
        super(name);
        this.vd = vd;
    }
}
