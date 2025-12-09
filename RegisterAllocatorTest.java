import Asm.*;
import Graph.Graph;

import java.lang.IO;

void main() {

  Program p1 = new Program();

  /*        XOR R0 R0 R0
   *        ADDi R0 R0 5
   *        JMP test
   *        XOR R0 R0 R0
   *  test: ADDi R1 R0 5
   */
  p1.addInstruction(new UAL(UAL.Op.XOR, 0, 0, 0));
  p1.addInstruction(new UALi(UALi.Op.ADD, 0, 0, 5));
  p1.addInstruction(new JumpCall(JumpCall.Op.JMP, "test"));
  p1.addInstruction(new UAL(UAL.Op.XOR, 0, 0, 0));
  p1.addInstruction(new UALi("test", UALi.Op.ADD, 1, 0, 5));

  RegisterAllocator ra = new RegisterAllocator(p1);
  Graph<Instruction> g = ra.generateControlGraph();
  // IO.println(g.toString());


  Program p2 = new Program();

  /*        XOR R0 R0 R0
   *        ADDi R0 R0 5
   *        JEQU R1 R0 test
   *        XOR R0 R0 R0
   *  test: ADDi R1 R0 5
   */
  p2.addInstruction(new UAL(UAL.Op.XOR, 0, 0, 0));
  p2.addInstruction(new UALi(UALi.Op.ADD, 0, 0, 5));
  p2.addInstruction(new CondJump(CondJump.Op.JEQU, 1, 0, "test"));
  p2.addInstruction(new UAL(UAL.Op.XOR, 0, 0, 0));
  p2.addInstruction(new UALi("test", UALi.Op.ADD, 1, 0, 5));

  ra = new RegisterAllocator(p2);
  g = ra.generateControlGraph();
  IO.println(g.toString());
}
