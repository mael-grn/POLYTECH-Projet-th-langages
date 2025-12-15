package RegisterAllocator;

import Asm.*;

import java.util.ArrayList;
import java.util.List;

public class CFGAnalysis {
  private final ControlGraph cfg;

  public CFGAnalysis(ControlGraph cfg) {
    this.cfg = cfg;
  }

  /**
   * Analyse kill(i)
   * @param i L'instruction à analyser
   * @return Le numéro du registre tué (=écrasé), ou null si aucun registre n'est tué
   */
  public static Integer kill(Instruction i) {
    if (i == null) throw new RuntimeException("Instruction is null");

    return switch (i) {
      case UAL ual -> ual.getDest();
      case UALi uaLi -> uaLi.getDest();
      case Mem mem when i.getName().equals("LD") -> mem.getDest();
      case Asm.IO io when i.getName().equals("IN") || i.getName().equals("READ") -> io.getReg();
      default -> null;
    };
  }

  /**
   * Analyse gen(i)
   * @param i L'instruction à analyser
   * @return La liste des numéros de registres générés (=utilisés) par l'instruction
   */
  public static List<Integer> gen(Instruction i) {
    if (i == null) throw new RuntimeException("Instruction is null");

    List<Integer> genList = new ArrayList<>();

    Integer gen1 = null;
    Integer gen2 = null;
    switch (i) {
      case UAL ual:
        gen1 = ual.getSr1();
        gen2 = ual.getSr2();
        break;
      case UALi uaLi:
        gen1 = uaLi.getSr();
        break;
      case Mem mem when i.getName().equals("ST"):
        gen1 = mem.getDest();
        gen2 = mem.getAddress();
        break;
      case Mem mem when i.getName().equals("LD"):
        gen1 = mem.getAddress();
        break;
      case Asm.IO io when i.getName().equals("OUT") || i.getName().equals("PRINT"):
        gen1 = io.getReg();
        break;
      case CondJump cj:
        gen1 = cj.getSr1();
        gen2 = cj.getSr2();
      default: break;
    }

    if (gen1 != null) genList.add(gen1);
    if (gen2 != null && !genList.contains(gen2)) {
      genList.add(gen2);
    }

    return genList;
  }
}
