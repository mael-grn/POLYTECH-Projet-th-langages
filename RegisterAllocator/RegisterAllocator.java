package RegisterAllocator;

import Asm.Program;
import Asm.Instruction;
import Graph.UnorientedGraph;

public class RegisterAllocator {
  private final Program program;
  private static final int MAX_REGISTERS = 32;

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

    // Construction du graphe d'interférence
    CFGAnalysis cfgAnalyzer = new CFGAnalysis(cfg);
    this.interferenceGraph = cfgAnalyzer.buildInterferenceGraph();

    /* int nbColors = this.interferenceGraph.color();

    // TODO :  Gérer le case où on n'a pas assez de registres

    System.out.println(nbColors + " colors");

    for (int i = 0; i <= 4; i++) {
      System.out.println("Reg " + i + " -> Color " + this.interferenceGraph.getColor(i));
    } */

    // Calcul des nouvelles instructions avec les registres renommés, et ajout au nouveau programme
    for (Instruction i : program.getInstructions()) {
      Instruction newInst = this.renameRegisters(i);
      minimizedProg.addInstruction(newInst);
    }

    return minimizedProg;
  }

  /**
   * Renomme les registres d'une instruction en utilisant les couleurs du graphe d'interférence.
   * @param instr l'instruction dont les registres doivent être renommés
   * @return l'instruction avec les registres renommés
   */
  private Instruction renameRegisters(Instruction instr) {
    switch (instr) {
      case Asm.UAL ualInst -> {
        ualInst.setDest(interferenceGraph.getColor(ualInst.getDest()));
        ualInst.setSr1(interferenceGraph.getColor(ualInst.getSr1()));
        ualInst.setSr2(interferenceGraph.getColor(ualInst.getSr2()));
      }
      case Asm.UALi ualiInst -> {
        ualiInst.setDest(interferenceGraph.getColor(ualiInst.getDest()));
        ualiInst.setSr(interferenceGraph.getColor(ualiInst.getSr()));
      }
      case Asm.Mem memInst -> {
        memInst.setDest(interferenceGraph.getColor(memInst.getDest()));
        memInst.setAddress(interferenceGraph.getColor(memInst.getAddress()));
      }
      case Asm.IO ioInst -> {
        ioInst.setReg(interferenceGraph.getColor(ioInst.getReg()));
      }
      case Asm.CondJump condJumpInst -> {
        condJumpInst.setSr1(interferenceGraph.getColor(condJumpInst.getSr1()));
        condJumpInst.setSr2(interferenceGraph.getColor(condJumpInst.getSr2()));
      }

      default -> {} // Pas de changement pour les autres instructions
    }

    return instr;
  }
}
