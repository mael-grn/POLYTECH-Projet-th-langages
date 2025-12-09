package Type;
import java.util.Map;

public class ArrayType extends Type{
    private final Type tabType;
    
    /**
     * Constructeur
     * @param t type des éléments du tableau
     */
    public ArrayType(Type t) {
        this.tabType = t;
    }

    /**
     * Getter du type des éléments du tableau
     * @return type des éléments du tableau
     */
    public Type getTabType() {
       return tabType;
    }

    @Override
    public Map<UnknownType, Type> unify(Type t) {
        // TODO Auto-generated method stub
        if (!(t instanceof ArrayType other)) {
            throw new RuntimeException("Cannot unify array type with non-array type");
        }

        return this.tabType.unify(other.tabType);

    }

    @Override
    public Type substitute(UnknownType v, Type t) {
        // Done
        return new ArrayType(tabType.substitute(v, t));
    }

    @Override
    public boolean contains(UnknownType v) {
        // Done
        return tabType.contains(v);
    }

    @Override
    public boolean equals(Object t) {
        // Done
        if (this == t) return true;
        if (!(t instanceof ArrayType other)) return false;
        return this.tabType.equals(other.tabType);

    }

    @Override
    public String toString() {
        // Done
        return "Array(" + tabType.toString() + ")";
    }

    
}
