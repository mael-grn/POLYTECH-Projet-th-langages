package RegisterAllocator;

import Graph.OrientedGraph;
import Asm.*;

import java.util.ArrayList;
import java.util.HashMap;

public class ControlGraph {
  private final Program program;
  private final OrientedGraph<Instruction> graph;

  public ControlGraph(Program program) {
    this.program = program;
    this.graph = new OrientedGraph<Instruction>();
    generate();
  }

  /**
   * Retourne le graphe de contrôle
   */
  public OrientedGraph<Instruction> getGraph() {
    return graph;
  }

  /**
   * Génération du graphe de contrôle à partir du programme
   * fonction appelée automatiquement dans le constructeur
   */
  private void generate() {
    ArrayList<Instruction> instructions = program.getInstructions();

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

      // On remplit la HashMap des RET avec les CALLs qui y font référence
      if (i instanceof JumpCall && i.getName().equals("CALL")) {
        String callLabel = ((JumpCall) i).getAddress();

        // On cherche l'instruction RET suivante après le label pointé par le CALL
        Instruction nextRetAfterLabel = getNextRetAfterLabel(callLabel, instructions);

        // On a appelé une fonction, mais aucun RET n'existe après le label appelé
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
  }


  /**
   * Recherche de la prochaine instruction RET après un label donné
   * @param label le label
   * @param instructions la liste des instructions du programme
   * @return la prochaine instruction RET après le label, ou null si aucune n'est trouvée
   */
  private Instruction getNextRetAfterLabel(String label, ArrayList<Instruction> instructions) {
    boolean isAfterLabel = false;
    for (Instruction j : instructions) {
      if (j.getLabel() != null && j.getLabel().equals(label)) {
        isAfterLabel = true;
      }

      if (isAfterLabel && j instanceof Ret) {
        return j;
      }
    }
    return null;
  }


  /**
   * Conversion du graphe de contrôle en format DOT
   */
  public String toDot() {
    ArrayList<Instruction> instructions = program.getInstructions();

    System.out.println(instructions);

    String s = "digraph G {\n";
    for (Instruction i : instructions) {
      ArrayList<Instruction> outNeighbors = graph.getOutNeighbors(i);
      if (outNeighbors == null) continue;

      for (Instruction neighbor : outNeighbors) {
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
