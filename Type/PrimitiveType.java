package Type;
import java.util.Collections;
import java.util.Map;

public  class PrimitiveType extends Type {
    private final Type.Base type;
    
    /**
     * Constructeur
     * @param type type de base
     */
    public PrimitiveType(Type.Base type) {
        this.type = type;
    }

    /**
     * Getter du type
     * @return type
     */
    public Type.Base getType() {
        return type;
    }

    @Override
    public Map<UnknownType, Type> unify(Type t) {
        if (t instanceof UnknownType) return t.unify(this);

        if (!(t instanceof PrimitiveType other)) {
            throw new RuntimeException("Incompatible types: Primitive vs " + t);
        }

        if (this.getType() != other.getType()) {
            // C'est ici que l'erreur doit être lancée !
            throw new RuntimeException("Échec d'unification : " + this.type + " vs " + other.type);
        }
        return Collections.emptyMap();
    }

    @Override
    public Type substitute(UnknownType v, Type t) {
        // Done
        return this;
    }

    @Override
    public boolean contains(UnknownType v) {
        return false;
    }
    // Done

    @Override
    public boolean equals(Object t) {
        // Done
        if (this==t) return true;
        if (!(t instanceof PrimitiveType other )) return false;
        return this.getType() == other.getType();
    }

    @Override
    public String toString() {
        return String.valueOf(this.getType());
    }
    // Done

}
