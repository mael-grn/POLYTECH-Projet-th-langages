package Type;
import java.util.Collection;
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
        // Done
        if (t instanceof UnknownType) {
            return t.unify(this);
        }
        if (!(t instanceof PrimitiveType other)) {
            throw new UnsupportedOperationException("Cannot unify Primitive type with non-primitive type");
        }
        if (this.getType() != other.getType()) {
            throw new UnsupportedOperationException("Échec d'unification : types primitifs différents");
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