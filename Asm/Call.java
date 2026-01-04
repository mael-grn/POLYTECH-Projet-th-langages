package Asm;

public class Call extends Instruction {
    private String functionName;

    /**
     * Constructeur
     * @param label label de l'instruction
     * @param functionName nom de la fonction à appeler
     */
    public Call(String label, String functionName) {
        super(label,"CALL");
        this.functionName = functionName;
    }
    /**
     * Constructeur sans label
     * @param functionName nom de la fonction à appeler
     */
    public Call(String functionName) {
        super("","CALL");
        this.functionName = functionName;
    }
    /**
     * Conversion en String
     * @return String texte de l'instruction
     */
    public String toString() {
        return this.label+ (this.label==""?"":": ") + this.name + " " + this.functionName + "\n";
    }
}
