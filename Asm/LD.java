package Asm;

public class LD extends Instruction {

    private final int destReg;   // registre de destination
    private final int addrReg;   // registre contenant l'adresse mémoire

    /**
     * Constructeur sans label
     */
    public LD(int destReg, int addrReg) {
        super("", "LD");
        this.destReg = destReg;
        this.addrReg = addrReg;
    }

    /**
     * Constructeur avec label
     */
    public LD(String label, int destReg, int addrReg) {
        super(label, "LD");
        this.destReg = destReg;
        this.addrReg = addrReg;
    }

    @Override
    public String toString() {
        return label + (label.isEmpty() ? "" : ": ")
                + name + " R" + destReg + " R" + addrReg + "\n";
    }
}
