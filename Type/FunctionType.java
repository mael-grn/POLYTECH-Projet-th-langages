package Type;
import java.util.ArrayList;
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
        if (!(t instanceof FunctionType other)){
            throw new RuntimeException("Cannot unify array type with non-array type");
        }
        return this.returnType.unify(other.returnType);
    }

    @Override
    public Type substitute(UnknownType v, Type t) {
        return new FunctionType(returnType.substitute(v,t), argsTypes);
    }

    @Override
    public boolean contains(UnknownType v) {
        return (argsTypes.contains(v) && returnType.contains(v));
        //Done
    }

    @Override
    public boolean equals(Object t) {

        //Done
        if (t== this){return true;}
        if (!(t instanceof FunctionType other)) {return false;}
        return (this.argsTypes == other.argsTypes && this.returnType == other.returnType);
    }

    @Override
    public String toString() {
        //Done
        return String.valueOf(this.returnType);
    }

}
