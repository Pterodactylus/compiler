package sem;

/**
 * Created by Valentas on 10/23/2016.
 */
public class StructDeclSymbol extends Symbol {

    public Scope structScope;

    public StructDeclSymbol(String name, Scope outer) {
        super(name);
        this.structScope = new Scope(outer);
    }
}
