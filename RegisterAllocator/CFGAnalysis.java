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
      // Opération UAL, MEM, IO : le registre tué est le registre destination
      case UAL ual -> ual.getDest();
      case UALi uaLi -> uaLi.getDest();
      case Mem mem when i.getName().equals("LD") -> mem.getDest();
      case Asm.IO io when i.getName().equals("IN") || i.getName().equals("READ") -> io.getReg();

      // Pas de registre tué pour les autres cas
      default -> null;
    };
  }

  /**
   * Analyse gen(i)
   * @param i L'instruction à analyser
   * @return La liste des numéros de registres générés (=utilisés) par l'instruction
   */
  public static Set<Integer> gen(Instruction i) {
    if (i == null) throw new RuntimeException("Instruction is null");

    Set<Integer> genList = new HashSet<>();

    switch (i) {
      case UAL ual:
        // Dans une opération UAL, les registres générés sont les deux sources
        genList.add(ual.getSr1());
        genList.add(ual.getSr2());
        break;
      case UALi uaLi:
        // Dans une opération UALi, le registre généré est la source
        genList.add(uaLi.getSr());
        break;
      case Mem mem when i.getName().equals("ST"):
        // Lors d'un STORE, le registre source et l'adresse sont utilisés
        genList.add(mem.getDest());
        genList.add(mem.getAddress());
        break;
      case Mem mem when i.getName().equals("LD"):
        // Lors d'un LOAD, seule l'adresse est utilisée
        genList.add(mem.getAddress());
        break;
      case Asm.IO io when i.getName().equals("OUT") || i.getName().equals("PRINT"):
        // Pour les opérations OUT/PRINT, le registre source est utilisé
        genList.add(io.getReg());
        break;
      case CondJump cj:
        // Pour les sauts conditionnels, les deux registres sources sont utilisés
        genList.add(cj.getSr1());
        genList.add(cj.getSr2());
      default: break;
    }

    return genList;
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
   * @return Une liste de LiveVars (une LiveVars pour chaque instruction)
   */
  public List<LiveVars> computeLiveVariables() {
    List<Instruction> instructions = cfg.getInstructions();

    // Initialiser les ensembles de variables vivantes (vides)
    List<LiveVars> liveVarsList = new ArrayList<>();
    for (Instruction _ : instructions) {
      liveVarsList.add(new LiveVars(new HashSet<>(), new HashSet<>()));
    }

    // Algorithme de saturation jusqu'à ce qu'il n'y ait plus de changement
    boolean changed;
    do {
      changed = false;
      for (Instruction instr : instructions) {
        int index = instructions.indexOf(instr);

        // Calcul de LVEntry
        Set<Integer> prevExit = liveVarsList.get(index).exit;
        Set<Integer> newEntry = new HashSet<>(prevExit); // LVEntry(i) = LVExit(i) au départ
        Integer killedReg = kill(instr);
        if (killedReg != null) {
          newEntry.remove(killedReg); // LVEntry(i) = LVExit(i) - kill(i)
        }
        Set<Integer> genReg = gen(instr);
        newEntry.addAll(genReg); // LVEntry(i) = LVEntry(i) + gen(i)


        // Calcul de LVExit
        Set<Integer> newExit = new HashSet<>();
        List<Instruction> successeurs = cfg.getGraph().getOutNeighbors(instr);
        if (successeurs != null) {
          for (Instruction succ : successeurs) {
            int succIndex = instructions.indexOf(succ);
            Set<Integer> succEntry = liveVarsList.get(succIndex).entry;
            newExit.addAll(succEntry);
          }
        }

        // Mettre à jour les ensembles si ils ont changé
        if (!newExit.equals(liveVarsList.get(index).exit)) {
          liveVarsList.get(index).exit = newExit;
          changed = true;
        }

        if (!newEntry.equals(liveVarsList.get(index).entry)) {
          liveVarsList.get(index).entry = newEntry;
          changed = true;
        }
      }
    } while (changed);

    return liveVarsList;
  }


  public UnorientedGraph<Integer> buildInterferenceGraph() {
    UnorientedGraph<Integer> interferenceGraph = new UnorientedGraph<Integer>();

    List<Instruction> instructions = cfg.getInstructions();
    List<LiveVars> liveVars = computeLiveVariables();

    for (Instruction inst : instructions) {
      int i = instructions.indexOf(inst);

      // Copie locale de LVexit
      Set<Integer> live = new HashSet<>(liveVars.get(i).exit);

      Integer killReg = kill(inst);

      if (killReg != null) {
        interferenceGraph.addVertex(killReg);

        for (Integer v : live) {
          interferenceGraph.addVertex(v);
          if (!v.equals(killReg)) {
            interferenceGraph.addEdge(killReg, v);
          }
        }
      }

      // Mise à jour du live
      live.remove(killReg);
      live.addAll(gen(inst));
    }

    return interferenceGraph;
  }

}