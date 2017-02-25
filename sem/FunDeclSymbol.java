package sem;

import ast.FunDecl;

/**
 * Created by Valentas on 10/22/2016.
 */
public class FunDeclSymbol extends Symbol {

    public Scope functionScope;

    public FunDecl fd;

    public FunDeclSymbol(String name, Scope outer, FunDecl fd) {
        super(name);
        functionScope = new Scope(outer);
        this.fd = fd;
    }
}
