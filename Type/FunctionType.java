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
        if (!(t instanceof FunctionType other)) {
            throw new UnsupportedOperationException("Impossible d'unifier une fonction avec " + t);
        }

        // Vérifier le nombre d'arguments
        if (this.argsTypes.size() != other.argsTypes.size()) {
            throw new UnsupportedOperationException("Nombre d'arguments différent");
        }

        Map<UnknownType, Type> substitution = new HashMap<>();

        // Unifier les types de retour
        Map<UnknownType, Type> retSubst = this.returnType.unify(other.returnType);
        substitution.putAll(retSubst);

        // Appliquer la substitution aux arguments
        ArrayList<Type> thisSubstArgs = new ArrayList<>();
        ArrayList<Type> otherSubstArgs = new ArrayList<>();

        for (Type arg : this.argsTypes) {
            Type substArg = arg;
            for (Map.Entry<UnknownType, Type> entry : substitution.entrySet()) {
                substArg = substArg.substitute(entry.getKey(), entry.getValue());
            }
            thisSubstArgs.add(substArg);
        }

        for (Type arg : other.argsTypes) {
            Type substArg = arg;
            for (Map.Entry<UnknownType, Type> entry : substitution.entrySet()) {
                substArg = substArg.substitute(entry.getKey(), entry.getValue());
            }
            otherSubstArgs.add(substArg);
        }

        // Unifier chaque paire d'arguments
        for (int i = 0; i < thisSubstArgs.size(); i++) {
            Map<UnknownType, Type> argSubst = thisSubstArgs.get(i).unify(otherSubstArgs.get(i));
            substitution.putAll(argSubst);
        }

        return substitution;
    }

    @Override
    public Type substitute(UnknownType v, Type t) {
        // Substituer dans le type de retour
        Type newReturnType = this.returnType.substitute(v, t);

        // Substituer dans chaque type d'argument
        ArrayList<Type> newArgsTypes = new ArrayList<>();
        for (Type argType : this.argsTypes) {
            newArgsTypes.add(argType.substitute(v, t));
        }

        return new FunctionType(newReturnType, newArgsTypes);
    }

    @Override
    public boolean contains(UnknownType v) {
        // Vérifier le type de retour
        if (returnType.contains(v)) return true;

        // Vérifier chaque argument
        for (Type argType : argsTypes) {
            if (argType.contains(v)) return true;
        }

        return false;
    }

    @Override
    public boolean equals(Object t) {
        if (t == this) return true;
        if (!(t instanceof FunctionType other)) return false;

        if (!this.returnType.equals(other.returnType)) return false;
        if (this.argsTypes.size() != other.argsTypes.size()) return false;

        for (int i = 0; i < this.argsTypes.size(); i++) {
            if (!this.argsTypes.get(i).equals(other.argsTypes.get(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        //Done
        return "(" + this.argsTypes + " -> " +  this.returnType + ")";
    }

}
