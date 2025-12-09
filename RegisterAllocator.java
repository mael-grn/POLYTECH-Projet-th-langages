import Asm.*;
import Graph.OrientedGraph;

import java.util.ArrayList;
import java.util.HashMap;


public class RegisterAllocator {

  private Program program;

  public RegisterAllocator(Program p) {
    this.program = p;
  }

  /**
   * Génération du graphe de contrôle à partir du programme
   */
  public OrientedGraph<Instruction> generateControlGraph() {
    ArrayList<Instruction> instructions = program.getInstructions();
    OrientedGraph<Instruction> graph = new OrientedGraph<Instruction>();

    // Une hashmap avec en clé les labels, et en valeur les instructions correspondantes
    HashMap<String, Instruction> instructionsWithLabel = new HashMap<>();

    // Une hashmap avec en clé les instructions RET et en valeur la liste des CALL qui y font référence
    HashMap<Ret, ArrayList<Instruction>> retMap = new HashMap<>();

    // Premier passage : on ajoute chaque instruction sur un sommet du graphe
    // et on relie chaque instruction à la suivante (sauf pour les JMP/CALL)
    Instruction prevInstruction = null;
    for (Instruction i : instructions) {
      // On remplit la HashMap des instructions avec labels
      if (i.getLabel() != null) {
        instructionsWithLabel.put(i.getLabel(), i);
      }

      //
      if (i instanceof JumpCall && i.getName().equals("CALL")) {
        String callLabel = ((JumpCall) i).getAddress();

        // On cherche l'instruction RET suivante après le label pointé par le CALL
        Instruction nextRetAfterLabel = null;
        boolean isAfterLabel = false;
        for (Instruction j : instructions) {
          if (j.getLabel().equals(callLabel)) {
            isAfterLabel = true;
          }

          if (isAfterLabel && j instanceof Ret) {
            nextRetAfterLabel = j;
            break;
          }
        }

        if (nextRetAfterLabel == null) {
          throw new RuntimeException("No RET for label " + callLabel + "but was called here : " + i.toString());
        }

        // On ajoute le CALL (instruction courante i) dans la liste des CALLs pour le RET trouvé
        // dans la HashMap retMap
        if (!retMap.containsKey((Ret) nextRetAfterLabel)) {
          retMap.put((Ret) nextRetAfterLabel, new ArrayList<>());
        }
        retMap.get((Ret) nextRetAfterLabel).add(i);
      }

      graph.addVertex(i);

      if (prevInstruction != null) {
        if (!(prevInstruction instanceof JumpCall) && !(prevInstruction instanceof Ret)) {
          // On relie l'instruction précédente à l'instruction courante
          // sauf si la précédente est un JMP/CALL/RET
          graph.addEdge(prevInstruction, i);

          // si la précédente est un CondJump, on relie aussi le CondJump
          // à l'instruction courante au cas ou la condition soit fausse
        }
      }
      prevInstruction = i;
    }


    // Second passage : on ajoute les arêtes pour les JMP/CALL et les CondJumps
    // d'après les instructions avec labels stockés dans la HashMap
    for (Instruction i : instructions) {
      if (i instanceof JumpCall) {
        // On relie le JumpCall à l'instruction pointée par son label
        Instruction targetInstruction = instructionsWithLabel.get(((JumpCall) i).getAddress());
        if (targetInstruction != null) {
          graph.addEdge(i, targetInstruction);
        }
      }

      if (i instanceof CondJump) {
        // On relie le CondJump à l'instruction pointée par son label
        Instruction targetInstruction = instructionsWithLabel.get(((CondJump) i).getAddress());
        if (targetInstruction != null) {
          graph.addEdge(i, targetInstruction);
        }
      }

      if (i instanceof Ret) {
        // On relie le RET à l'instruction suivant chaque CALL qui y fait référence
        for (Instruction caller : retMap.getOrDefault(((Ret) i), new ArrayList<>())) {
          int callerIndex = instructions.indexOf(caller);
          if (callerIndex + 1 < instructions.size()) {
            Instruction nextInstruction = instructions.get(callerIndex + 1);
            graph.addEdge(i, nextInstruction);
          }
        }
      }
    }

    return graph;
  }

  /**
   * Conversion du graphe de contrôle en format DOT
   */
  public static String toDot(Program p, OrientedGraph<Instruction> g) {
    ArrayList<Instruction> instructions = p.getInstructions();
    String s = "digraph G {\n";
    for (Instruction i : instructions) {
      for (Instruction neighbor : g.getOutNeighbors(i)) {
        Integer index = instructions.indexOf(i);
        Integer neighborIndex = instructions.indexOf(neighbor);
        s += "\t\""
                + index.toString() + " : "
                + instructions.get(index).toString().replace("\n", "")
                + "\" -> \""
                + neighborIndex.toString() + " : "
                + instructions.get(neighborIndex).toString().replace("\n", "")
                + "\";\n";
      }
    }
    s += "}\n";

    System.out.println("https://dreampuf.github.io/GraphvizOnline/?engine=dot");
    return s;
  }

}
