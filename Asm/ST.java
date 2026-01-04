package Asm;

public class ST extends Instruction {

    private final int srcReg;   // registre contenant la valeur à stocker
    private final int addrReg;  // registre contenant l'adresse mémoire

    /**
     * Constructeur sans label
     */
    public ST(int srcReg, int addrReg) {
        super("", "ST");
        this.srcReg = srcReg;
        this.addrReg = addrReg;
    }

    /**
     * Constructeur avec label
     */
    public ST(String label, int srcReg, int addrReg) {
        super(label, "ST");
        this.srcReg = srcReg;
        this.addrReg = addrReg;
    }

    @Override
    public String toString() {
        return label + (label.isEmpty() ? "" : ": ")
                + name + " R" + srcReg + " R" + addrReg + "\n";
    }
}
