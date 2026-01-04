package RegisterAllocator;

import Asm.*;
import Graph.UnorientedGraph;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class RegisterAllocator {
  private final Program program;

  private final static boolean DEBUG_MODE = false;

  private static final int MAX_REGISTERS = 32;
  private static final int STACK_POINTER = 1;

  // Utile pour éviter d'écraser les registres réservés
  // comme R0 (valeur de retour des fonctions) et R1 (pointeur de pile)
  private static final int COLOR_OFFSET = 2;

  // Valeur par défaut pour le pointeur de pile (SP)
  // la taille de la mémoire est de 65536 mots
  private static final int DEFAULT_SP_VALUE = 65536;

  private UnorientedGraph<Integer> interferenceGraph;

  public RegisterAllocator(Program program) {
    this.program = program;
  }

  /**
   * Calcule le graphe de contrôle, les Living Variables, puis le graphe d'interférence
   * dans le but de minimiser le nombre de registres utilisés dans le programme.
   * @return le programme minimisé avec un nombre réduit de registres
   */
  public Program minimizeRegisters() {
    // Programme vide qui contiendra les instructions avec les registres renommés
    Program minimizedProg = new Program();

    // Construction du graphe de contrôle
    ControlGraph cfg = new ControlGraph(program);

    if (DEBUG_MODE) System.out.println(cfg.toDot());

    // Construction du graphe d'interférence
    CFGAnalysis cfgAnalyzer = new CFGAnalysis(cfg);

    this.interferenceGraph = cfgAnalyzer.buildInterferenceGraph();

    int nbColors = this.interferenceGraph.color();

    if (DEBUG_MODE) {
      System.out.println(nbColors + " colors");
      for (int i = COLOR_OFFSET; i <= MAX_REGISTERS+COLOR_OFFSET; i++) {
        if (this.interferenceGraph.getColor(i) == -1) continue;
        System.out.println("Reg " + i + " -> Color " + this.interferenceGraph.getColor(i));
      }
    }

    // TODO :  Gérer le cas où on n'a pas assez de registres

    // Initialisation du pointeur de pile (SP)
    minimizedProg.addInstruction(new Asm.UAL(Asm.UAL.Op.XOR, STACK_POINTER, STACK_POINTER, STACK_POINTER));
    minimizedProg.addInstruction(new Asm.UALi(Asm.UALi.Op.ADD, STACK_POINTER, STACK_POINTER, DEFAULT_SP_VALUE));

    // Calcul des nouvelles instructions avec les registres renommés, et ajout au nouveau programme
    for (Instruction i : program.getInstructions()) {
      Instruction newInst = this.renameRegisters(i);

      boolean isCall = i instanceof Asm.JumpCall jumpCallInst && jumpCallInst.getName().equals("CALL");

      // les registres vivants avant l'appel de fonction
      Set<Integer> livingRegistersBeforeCall = isCall
              ? cfgAnalyzer.computeLiveVariables().get(program.getInstructions().indexOf(i)).entry
              : null;

      // On sauvegarde les registres sur la pile avant les appels de fonction
      if (isCall) minimizedProg.addInstructions(pushRegistersBeforeCall(newInst, livingRegistersBeforeCall));

      // Ajout de l'instruction renommée avec la coloration des registres
      minimizedProg.addInstruction(newInst);

      // On restaure les registres depuis la pile après les appels de fonction
      if (isCall) minimizedProg.addInstructions(popRegistersAfterCall(newInst, livingRegistersBeforeCall));
    }

    return minimizedProg;
  }

  /**
   * Renomme les registres d'une instruction en utilisant les couleurs du graphe d'interférence.
   * @param instr l'instruction dont les registres doivent être renommés
   * @return l'instruction avec les registres renommés
   */
  private Instruction renameRegisters(Instruction instr) {
    String instLabel = instr.getLabel();
    Instruction retInst;

    switch (instr) {
      case UAL ualInst -> {
        int dest = interferenceGraph.getColor(ualInst.getDest()) + COLOR_OFFSET;
        int sr1 = interferenceGraph.getColor(ualInst.getSr1()) + COLOR_OFFSET;
        int sr2 = interferenceGraph.getColor(ualInst.getSr2()) + COLOR_OFFSET;
        retInst = new UAL(UAL.Op.valueOf(ualInst.getName()), dest, sr1, sr2);
      }
      case UALi ualiInst -> {
        int dest = interferenceGraph.getColor(ualiInst.getDest()) + COLOR_OFFSET;
        int sr = interferenceGraph.getColor(ualiInst.getSr()) + COLOR_OFFSET;
        retInst = new UALi(UALi.Op.valueOf(ualiInst.getName()), dest, sr, ualiInst.getImm());
      }
      case Mem memInst -> {
        int dest = interferenceGraph.getColor(memInst.getDest()) + COLOR_OFFSET;
        int address = interferenceGraph.getColor(memInst.getAddress()) + COLOR_OFFSET;
        retInst = new Mem(Mem.Op.valueOf(memInst.getName()), dest, address);
      }
      case Asm.IO ioInst -> {
        int reg = interferenceGraph.getColor(ioInst.getReg()) + COLOR_OFFSET;
        retInst = new Asm.IO(Asm.IO.Op.valueOf(ioInst.getName()), reg);
      }
      case CondJump condJumpInst -> {
        int sr1 = interferenceGraph.getColor(condJumpInst.getSr1()) + COLOR_OFFSET;
        int sr2 = interferenceGraph.getColor(condJumpInst.getSr2()) + COLOR_OFFSET;
        retInst = new CondJump(CondJump.Op.valueOf(condJumpInst.getName()), sr1, sr2, condJumpInst.getAddress());
      }
      case JumpCall jcInst ->
        retInst = new JumpCall(JumpCall.Op.valueOf(jcInst.getName()), jcInst.getAddress());
      case Ret _ ->
        retInst = new Ret();
      case Stop _ ->
        retInst = new Stop();

      default ->
        throw new RuntimeException("(RegisterAllocator.RegisterAllocator) Instruction type not supported for register renaming: " + instr.getClass().getName());
    }

    retInst.setLabel(instLabel);
    return retInst;
  }


  /**
   * Sauvegarde les registres vivants avant un appel de fonction
   * @param instCall L'instruction d'appel de fonction
   * @param livingRegisters Les registres vivants avant l'appel
   * @return Le programme contenant les instructions de sauvegarde des registres
   */
  private Program pushRegistersBeforeCall(Instruction instCall, Set<Integer> livingRegisters) {
    Program prog = new Program();

    List<Integer> pushedRegs = new ArrayList<>();

    for (Integer reg : livingRegisters) {
      // Ne pas sauvegarder le registre du pointeur de pile et le registre de retour
      if (reg < COLOR_OFFSET) continue;

      // Éviter de sauvegarder plusieurs fois le même registre
      int color = this.interferenceGraph.getColor(reg);
      if (pushedRegs.contains(color + COLOR_OFFSET)) continue;

      pushedRegs.add(color + COLOR_OFFSET);

      prog.addInstruction(new Asm.UALi(Asm.UALi.Op.SUB, STACK_POINTER, STACK_POINTER, 1));
      prog.addInstruction(new Asm.Mem(Asm.Mem.Op.ST, color + COLOR_OFFSET, STACK_POINTER));
    }

    if (DEBUG_MODE)
      System.out.println("Pushed registers before call: " + pushedRegs);
    return prog;
  }


  /**
   * Restaure les registres vivants après un appel de fonction
   * @param instCall L'instruction d'appel de fonction
   * @param livingRegisters Les registres vivants avant l'appel
   * @return Le programme contenant les instructions de restauration des registres
   */
  private Program popRegistersAfterCall(Instruction instCall, Set<Integer> livingRegisters) {
    Program prog = new Program();

    List<Integer> poppedRegs = new ArrayList<>();

    for (Integer reg : livingRegisters) {
      // Ne pas sauvegarder le registre du pointeur de pile et le registre de retour
      if (reg < COLOR_OFFSET) continue;

      // Éviter de sauvegarder plusieurs fois le même registre
      int color = this.interferenceGraph.getColor(reg);
      if (poppedRegs.contains(color + COLOR_OFFSET)) continue;

      poppedRegs.add(color + COLOR_OFFSET);

      prog.addInstruction(new Asm.Mem(Asm.Mem.Op.ST, color + COLOR_OFFSET, STACK_POINTER));
      prog.addInstruction(new Asm.UALi(Asm.UALi.Op.ADD, STACK_POINTER, STACK_POINTER, 1));
    }


    return prog;
  }

}
