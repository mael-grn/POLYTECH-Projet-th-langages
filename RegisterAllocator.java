import Asm.Instruction;
import Asm.Program;
import Graph.Graph;
import Graph.OrientedGraph;

import java.util.ArrayList;


public class RegisterAllocator {

  private Program program;

  public RegisterAllocator(Program p) {
    this.program = p;
  }

  public Graph<Instruction> generateControlGraph() {
    ArrayList<Instruction> instructions = program.getInstructions();
    Graph<Instruction> graph = new OrientedGraph<Instruction>();

    for (Instruction i : instructions) {
      switch (i.getName()) {
        case "JMP":
          break;
        default:
          graph.addVertex(i);
          break;
      }
    }

    return graph;
  }

}
