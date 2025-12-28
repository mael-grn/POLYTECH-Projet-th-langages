import Asm.*;
import Graph.OrientedGraph;
import Graph.UnorientedGraph;
import RegisterAllocator.CFGAnalysis;
import RegisterAllocator.ControlGraph;

void main() {
  RegisterAllocator ra;
  Program p;
  OrientedGraph<Instruction> g;

  /*        XOR R0 R0 R0
   *        ADDi R0 R0 5
   *        JMP test
   *        XOR R0 R0 R0
   *  test: ADDi R1 R0 5
   */
  p = new Program();
  p.addInstruction(new UAL(UAL.Op.XOR, 0, 0, 0));
  p.addInstruction(new UALi(UALi.Op.ADD, 0, 0, 5));
  p.addInstruction(new JumpCall(JumpCall.Op.JMP, "test"));
  p.addInstruction(new UAL(UAL.Op.XOR, 0, 0, 0));
  p.addInstruction(new UALi("test", UALi.Op.ADD, 1, 0, 5));

  ra = new RegisterAllocator(p);
  g = ra.generateControlGraph();
  // g.printGraph();
  System.out.println(RegisterAllocator.toDot(p, g));


  /*        XOR R0 R0 R0
   *        ADDi R0 R0 5
   *        JEQU R1 R0 test
   *        XOR R0 R0 R0
   *  test: ADDi R1 R0 5
   */
  p = new Program();
  p.addInstruction(new UAL(UAL.Op.XOR, 0, 0, 0));
  p.addInstruction(new UALi(UALi.Op.ADD, 0, 0, 5));
  p.addInstruction(new CondJump(CondJump.Op.JEQU, 1, 0, "test"));
  p.addInstruction(new UAL(UAL.Op.XOR, 0, 0, 0));
  p.addInstruction(new UALi("test", UALi.Op.ADD, 1, 0, 5));

  ra = new RegisterAllocator(p);
  g = ra.generateControlGraph();
  // g.printGraph();
  System.out.println(RegisterAllocator.toDot(p, g));


  /*        XOR R0 R0 R0
   *        CALL inc
   *
   *        XOR R0 R0 R0
   *
   * inc:   ADDi R0 R0 1
   *        RET
   *
   *        ADDi R0 R0 45
   */
  p = new Program();

  p.addInstruction(new UAL(UAL.Op.XOR, 0, 0, 0));
  p.addInstruction(new JumpCall(JumpCall.Op.CALL, "inc"));
  p.addInstruction(new UAL(UAL.Op.XOR, 0, 0, 0));
  p.addInstruction(new UALi("inc", UALi.Op.ADD, 0, 0, 1));
  p.addInstruction(new Ret());
  p.addInstruction(new UALi(UALi.Op.ADD, 0, 0, 45));

  ra = new RegisterAllocator(p);
  g = ra.generateControlGraph();
  // g.printGraph();
  System.out.println(RegisterAllocator.toDot(p, g));
  ControlGraph cfg;
  CFGAnalysis analysis = new CFGAnalysis(cfg);

  UnorientedGraph<Integer> graph =
          analysis.buildInterferenceGraph();

  System.out.println("Nombre de sommets : " + graph.getNodes().size());
  System.out.println("Nombre d'arêtes : " + graph.getEdges().size());

}
