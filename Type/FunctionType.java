package Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
        if (!(t instanceof FunctionType other)) {
            throw new UnsupportedOperationException("Impossible d'unifier une fonction avec " + t);
        }

        // Vérifier le nombre d'arguments
        if (this.argsTypes.size() != other.argsTypes.size()) {
            throw new UnsupportedOperationException("Nombre d'arguments différent");
        }

        Map<UnknownType, Type> substitutions = new HashMap<>();

        // Unifier tous les arguments
        for (int i = 0; i < this.argsTypes.size(); i++) {
            Type arg1 = this.argsTypes.get(i).substituteAll(substitutions);
            Type arg2 = other.argsTypes.get(i).substituteAll(substitutions);
            Map<UnknownType, Type> argSubs = arg1.unify(arg2);

            // Mettre à jour les substitutions existantes
            for (Map.Entry<UnknownType, Type> entry : substitutions.entrySet()) {
                substitutions.put(entry.getKey(), entry.getValue().substituteAll(argSubs));
            }
            substitutions.putAll(argSubs);
        }

        // Unifier le type de retour
        Type ret1 = this.returnType.substituteAll(substitutions);
        Type ret2 = other.returnType.substituteAll(substitutions);
        Map<UnknownType, Type> retSubs = ret1.unify(ret2);

        // Mettre à jour toutes les substitutions
        for (Map.Entry<UnknownType, Type> entry : substitutions.entrySet()) {
            substitutions.put(entry.getKey(), entry.getValue().substituteAll(retSubs));
        }
        substitutions.putAll(retSubs);

        return substitutions;
    }

    @Override
    public Type substitute(UnknownType v, Type t) {
        ArrayList<Type> newArgs = new ArrayList<>();
        for (Type argType : this.argsTypes) {
            newArgs.add(argType.substitute(v, t));
        }
        return new FunctionType(this.returnType.substitute(v, t), newArgs);
    }

    @Override
    public boolean contains(UnknownType v) {
        if (returnType.contains(v)) {
            return true;
        }
        for (Type argType : argsTypes) {
            if (argType.contains(v)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object t) {
        if (this == t) {
            return true;
        }
        if (!(t instanceof FunctionType other)) {
            return false;
        }
        return this.returnType.equals(other.returnType) && this.argsTypes.equals(other.argsTypes);
    }

    @Override
    public String toString() {
        return "(" + this.returnType + " -> " + this.argsTypes + ")";
    }
}