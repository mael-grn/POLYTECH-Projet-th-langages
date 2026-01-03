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
        // Si c'est une inconnue, on inverse pour utiliser UnknownType.unify
        if (t instanceof UnknownType) {
            return t.unify(this);
        }

        // Si ce n'est pas un tableau et pas une inconnue, là on peut rouspéter
        if (!(t instanceof ArrayType other)) {
            throw new RuntimeException("Cannot unify array type with non-array type");
        }

        // Si c'est un autre tableau, on unifie le contenu
        return this.getTabType().unify(other.getTabType());
    }

    @Override
    public Type substitute(UnknownType v, Type t) {
        // Done
        return new ArrayType(getTabType().substitute(v, t));
    }

    @Override
    public boolean contains(UnknownType v) {
        // Done
        return this.getTabType().contains(v);
    }

    @Override
    public boolean equals(Object t) {
        // Done
        if (this == t) return true;
        if (!(t instanceof ArrayType other)) return false;
        return this.getTabType().equals(other.getTabType());

    }

    @Override
    public String toString() {
        // Done
        return "Array(" + this.getTabType().toString() + ")";
    }

    
}
