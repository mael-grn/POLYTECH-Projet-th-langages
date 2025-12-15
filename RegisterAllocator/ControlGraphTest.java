import Asm.*;
import RegisterAllocator.CFGAnalysis;
import RegisterAllocator.ControlGraph;

void main() {
  Program p;

  /* p = ProgramGenerator.compile(new String[] {
          "       XOR R0 R0 R0",
          "       ADDi R0 R0 5",
          "       JMP test",
          "       XOR R0 R0 R0",
          "test:  ADDi R1 R0 5"
  }); */

  /* p = RegisterAllocator.ProgramGenerator.compile(new String[] {
          "       XOR R0 R0 R0",
          "       ADDi R0 R0 5",
          "       JEQU R1 R0 test",
          "       XOR R0 R0 R0",
          "test:  ADDi R1 R0 5"
  }); */

  p = RegisterAllocator.ProgramGenerator.compile(new String[] {
          "     XOR R0 R0 R0",
          "     CALL inc",
          "     XOR R0 R0 R0",
          "inc: ADDi R0 R0 1",
          "     RET",
          "     ADDi R0 R0 45"
  });

  ControlGraph cfg = new ControlGraph(p);
  System.out.println(cfg.toDot());
  for (Instruction i : p.getInstructions()) {
    System.out.println("\nGEN(" + i.toString().replace("\n", "") + ") = " + CFGAnalysis.gen(i));
    System.out.println("KILL(" + i.toString().replace("\n", "") + ") = " + CFGAnalysis.kill(i));
  }
}
