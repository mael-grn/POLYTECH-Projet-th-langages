package RegisterAllocator;

import Asm.*;
import Graph.UnorientedGraph;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

  /**
   * Analyse kill(B) pour un bloc B
   * @param block Le bloc à analyser
   * @return L'ensemble des registres tués dans le bloc
   */
  public static Set<Integer> killBlock(List<Instruction> block) {
    Set<Integer> kills = new HashSet<>();
    for (Instruction i : block) {
      Integer k = kill(i);
      if (k != null) kills.add(k);
    }
    return kills;
  }

  /**
   * Analyse gen(B) pour un bloc B
   * @param block Le bloc à analyser
   * @return L'ensemble des registres générés dans le bloc
   */
  public static Set<Integer> genBlock(List<Instruction> block) {
    Set<Integer> gens = new HashSet<>();
    for (Instruction i : block) {
      gens.addAll(gen(i));
    }
    return gens;
  }

  public static class LiveVars {
    public Set<Integer> entry;
    public Set<Integer> exit;

    public LiveVars(Set<Integer> entry, Set<Integer> exit) {
      this.entry = entry;
      this.exit = exit;
    }
  }

/**
 * Calcule les variables vivantes pour tous les blocs
 * @return Une liste de LiveVars pour chaque bloc
 */
public List<LiveVars> computeLiveVariables() {
    List<List<Instruction>> blocks = cfg.getBlocks();
    int numBlocks = blocks.size();
    List<LiveVars> liveVars = new ArrayList<>();
    
    // Initialisation
    for (int i = 0; i < numBlocks; i++) {
        liveVars.add(new LiveVars(new HashSet<>(), new HashSet<>()));
    }

    boolean changed = true;
    while (changed) {
        changed = false;
        for (int i = numBlocks - 1; i >= 0; i--) {
            List<Instruction> block = blocks.get(i);
            LiveVars currentLV = liveVars.get(i);
            
            Set<Integer> oldExit = new HashSet<>(currentLV.exit);
            Set<Integer> newExit = new HashSet<>();
            
            if (!block.isEmpty()) {
                Instruction lastInst = block.get(block.size() - 1);
                
                ArrayList<Instruction> succs = cfg.getGraph().getOutNeighbors(lastInst);
                
                if (succs != null) {
                    for (Instruction succInst : succs) {
                        int succBlockIndex = -1;
                        for (int j = 0; j < numBlocks; j++) {
                            if (blocks.get(j).contains(succInst)) {
                                succBlockIndex = j;
                                break;
                            }
                        }
                        
                        if (succBlockIndex != -1) {
                            newExit.addAll(liveVars.get(succBlockIndex).entry);
                        }
                    }
                }
            }
            
            currentLV.exit = newExit;
            
            Set<Integer> oldEntry = new HashSet<>(currentLV.entry);
            
            // LVentry = (LVexit - KILL) union GEN
            Set<Integer> killSet = killBlock(block);
            Set<Integer> genSet = genBlock(block);
            
            Set<Integer> newEntry = new HashSet<>(currentLV.exit);
            newEntry.removeAll(killSet);
            newEntry.addAll(genSet);
            
            currentLV.entry = newEntry;
            
            if (!oldExit.equals(currentLV.exit) || !oldEntry.equals(currentLV.entry)) {
                changed = true;
            }
        }
    }
    return liveVars;
  }
  public UnorientedGraph<Integer> buildInterferenceGraph() {
    UnorientedGraph<Integer> graph = new UnorientedGraph<>();

    List<List<Instruction>> blocks = cfg.getBlocks();
    List<LiveVars> liveVars = computeLiveVariables();

    for (int b = 0; b < blocks.size(); b++) {
        List<Instruction> block = blocks.get(b);

        // Copie locale de LVexit
        Set<Integer> live = new HashSet<>(liveVars.get(b).exit);

        // Parcours du bloc à l'envers
        for (int i = block.size() - 1; i >= 0; i--) {
            Instruction inst = block.get(i);
            Integer def = kill(inst);

            if (def != null) {
                graph.addNode(def);

                for (Integer v : live) {
                    graph.addNode(v);
                    if (!v.equals(def)) {
                        graph.addEdge(def, v);
                    }
                }
            }

            // Mise à jour du live
            live.remove(def);
            live.addAll(gen(inst));
        }
    }

    return graph;
}

}