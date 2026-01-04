package Type;
import java.util.HashMap;
import java.util.Map;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

public class UnknownType extends Type {
    private final String varName;
    private int varIndex;
    private static int newVariableCounter = 0;

    /**
     * Constructeur sans nom
     */
    public UnknownType(){
        this.varIndex = newVariableCounter++;
        this.varName = "#";
    }

    /**
     * Constructeur à partir d'un nom de variable et un numéro
     * @param s nom de variable
     * @param n numéro de la variable
     */
    public UnknownType(String s, int n)  {
        this.varName = s;        
        this.varIndex = n;
    }

    /**
     * Constructeur à partir d'un ParseTree (standardisation du nom de variable)
     * @param ctx ParseTree
     */
    public UnknownType(ParseTree ctx) {
        this.varName = ctx.getText();
        if (ctx instanceof TerminalNode) {
            this.varIndex = ((TerminalNode)ctx).getSymbol().getStartIndex();
        } else {
            if (ctx instanceof ParserRuleContext) {
                this.varIndex = ((ParserRuleContext)ctx).getStart().getStartIndex();
            }
            else {
                throw new Error("Illegal UnknownType construction");
            }
        }
    }

    /**
     * Getter du nom de variable de type
     * @return variable de type
     */
    public String getVarName() {
        return varName;
    }

    /**
     * Getter du numéro de variable de type
     * @return numéro de variable de type
     */
    public int getVarIndex() {
        return varIndex;
    }

    /**
     * Setter du numéro de variable de type
     * @param n numéro de variable de type
     */
    public void setVarIndex(int n) {
        this.varIndex = n;
    }

    @Override
    public Map<UnknownType, Type> unify(Type t) {
        // DONE
        Map<UnknownType, Type> substitution = new HashMap<>();

        // si c'est le même type pas de substitution nécessaire
        if (this.equals(t)) {
            return substitution;
        }

        // éviter les types récursifs comme A = TAB[A]
        if (t.contains(this)) {
            throw new RuntimeException("ERREUR : Type récursif détecté: " + this + " apparaît dans " + t);
        }

        substitution.put(this, t);
        return substitution;
    }

    @Override
    public Type substitute(UnknownType v, Type t) {
        // DONE
        // si c'est la variable qu'on veut remplacer
        if (this.equals(v)) {
            return t;
        }
        // sinon, on ne change rien
        return this;
    }

    @Override
    public boolean contains(UnknownType v) {
        // DONE
        // Un UnknownType contient v s'il est égal à v
        return this.equals(v);
    }

    @Override
    public boolean equals(Object t) {
        // DONE
        if (this == t) return true;
        if (!(t instanceof UnknownType)) return false;
        UnknownType other = (UnknownType) t;
        return this.varName.equals(other.varName) && this.varIndex == other.varIndex;
    }

    @Override
    public String toString() {
        // DONE
        return "AUTO"; //varName + varIndex
    }

}
