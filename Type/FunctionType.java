package Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class FunctionType extends Type {
    private final Type returnType;
    private final ArrayList<Type> argsTypes;
    
    /**
     * Constructeur
     * @param returnType type de retour
     * @param argsTypes liste des types des arguments
     */
    public FunctionType(Type returnType, ArrayList<Type> argsTypes) {
        this.returnType = returnType;
        this.argsTypes = argsTypes;
    }

    /**
     * Getter du type de retour
     * @return type de retour
     */
    public Type getReturnType() {
        return returnType;
    }

    /**
     * Getter du type du i-eme argument
     * @param i entier
     * @return type du i-eme argument
     */
    public Type getArgsType(int i) {
        return argsTypes.get(i);
    }

    /**
     * Getter du nombre d'arguments
     * @return nombre d'arguments
     */
    public int getNbArgs() {
        return argsTypes.size();
    }

    @Override
    public Map<UnknownType, Type> unify(Type t) {
        // Done
        if (!(t instanceof FunctionType other)) {
            throw new UnsupportedOperationException("Impossible d'unifier une fonction avec " + t);
        }
        Map<UnknownType, Type> s1 = this.returnType.unify(other.returnType);
        Type r1 = this.returnType;
        Type r2 = other.returnType;

        for (Map.Entry<UnknownType, Type> entry : s1.entrySet()) {
            r1 = r1.substitute(entry.getKey(), entry.getValue());
            r2 = r2.substitute(entry.getKey(), entry.getValue());
        }
        Map<UnknownType, Type> s2 = r1.unify(r2);
        Map<UnknownType, Type> result = new HashMap<>(s1);
        result.replaceAll((key, type) -> {
            Type updated = type;
            for (Map.Entry<UnknownType, Type> entry : s2.entrySet()) {
                updated = updated.substitute(entry.getKey(), entry.getValue());
            }
            return updated;
        });
        result.putAll(s2);
        return result;
    }

    @Override
    public Type substitute(UnknownType v, Type t) {
        // Done
        return new FunctionType(this.returnType.substitute(v,t), this.argsTypes);
    }

    @Override
    public boolean contains(UnknownType v) {
        return argsTypes.contains(v) || returnType.contains(v);
        //Done
    }

    @Override
    public boolean equals(Object t) {

        //Done
        if (t== this){return true;}
        if (!(t instanceof FunctionType other)) {return false;}
        return this.returnType == other.returnType && this.argsTypes == other.argsTypes;
    }

    @Override
    public String toString() {
        //Done
        return "(" + this.returnType + " -> " + this.argsTypes + ")";
    }

}
