package RegisterAllocator;

/*
 * Pour les tests :
 * Générateur de classe Program à partir d'un code assembleur en texte
 */

import Asm.Program;

import java.util.ArrayList;
import java.util.Arrays;

public class ProgramGenerator {
  public static Program compile(String code) {
    String[] lines = code.split("\n");
    return compile(lines);
  }

  public static Program compile(String[] lines) {
    Program program = new Program();

    for (String line : lines) {
      line = line.trim();
      if (line.isEmpty()) continue;
      // Here we would parse the line and create the appropriate Instruction object
      // For simplicity, let's assume we have a method parseInstruction that does this

      ArrayList<String> tokens = new ArrayList<>(Arrays.asList(line.split("[\\s\\t]+")));

      String label = null;

      if (tokens.get(0).endsWith(":")) {
        // It's a label
        label = tokens.get(0).substring(0, tokens.get(0).length() - 1);
        // The next token should be the instruction
        tokens.remove(0);
      }

      if (tokens.get(0).equals("XOR")) {
        int rd = Integer.parseInt(tokens.get(1).substring(1));
        int rs1 = Integer.parseInt(tokens.get(2).substring(1));
        int rs2 = Integer.parseInt(tokens.get(3).substring(1));
        if (label != null) {
          program.addInstruction(new Asm.UAL(label, Asm.UAL.Op.XOR, rd, rs1, rs2));
        } else {
          program.addInstruction(new Asm.UAL(Asm.UAL.Op.XOR, rd, rs1, rs2));
        }
      } else if (tokens.get(0).equals("ADDi")) {
        int rd = Integer.parseInt(tokens.get(1).substring(1));
        int rs1 = Integer.parseInt(tokens.get(2).substring(1));
        int imm = Integer.parseInt(tokens.get(3));
        if (label != null) {
          program.addInstruction(new Asm.UALi(label, Asm.UALi.Op.ADD, rd, rs1, imm));
        } else {
          program.addInstruction(new Asm.UALi(Asm.UALi.Op.ADD, rd, rs1, imm));
        }
      } else if (tokens.get(0).equals("ADD")) {
        int rd = Integer.parseInt(tokens.get(1).substring(1));
        int rs1 = Integer.parseInt(tokens.get(2).substring(1));
        int imm = Integer.parseInt(tokens.get(3).substring(1));
        if (label != null) {
          program.addInstruction(new Asm.UAL(label, Asm.UAL.Op.ADD, rd, rs1, imm));
        } else {
          program.addInstruction(new Asm.UAL(Asm.UAL.Op.ADD, rd, rs1, imm));
        }
      } else if (tokens.get(0).equals("JMP")) {
        String destLabel = tokens.get(1);
        if (label != null) {
          program.addInstruction(new Asm.JumpCall(label, Asm.JumpCall.Op.JMP, destLabel));
        } else {
          program.addInstruction(new Asm.JumpCall(Asm.JumpCall.Op.JMP, destLabel));
        }
      } else if (tokens.get(0).equals("JEQU")) {
        int rs1 = Integer.parseInt(tokens.get(1).substring(1));
        int rs2 = Integer.parseInt(tokens.get(2).substring(1));
        String destLabel = tokens.get(3);
        if (label != null) {
          program.addInstruction(new Asm.CondJump(label, Asm.CondJump.Op.JEQU, rs1, rs2, destLabel));
        } else {
          program.addInstruction(new Asm.CondJump(Asm.CondJump.Op.JEQU, rs1, rs2, destLabel));
        }
      } else if (tokens.get(0).equals("JSEQ")) {
        int rs1 = Integer.parseInt(tokens.get(1).substring(1));
        int rs2 = Integer.parseInt(tokens.get(2).substring(1));
        String destLabel = tokens.get(3);
        if (label != null) {
          program.addInstruction(new Asm.CondJump(label, Asm.CondJump.Op.JSEQ, rs1, rs2, destLabel));
        } else {
          program.addInstruction(new Asm.CondJump(Asm.CondJump.Op.JSEQ, rs1, rs2, destLabel));
        }
      } else if (tokens.get(0).equals("CALL")) {
        String destLabel = tokens.get(1);
        if (label != null) {
          program.addInstruction(new Asm.JumpCall(label, Asm.JumpCall.Op.CALL, destLabel));
        } else {
          program.addInstruction(new Asm.JumpCall(Asm.JumpCall.Op.CALL, destLabel));
        }
      } else if (tokens.get(0).equals("RET")) {
        if (label != null) {
          program.addInstruction(new Asm.Ret(label));
        } else {
          program.addInstruction(new Asm.Ret());
        }
      } else if (tokens.get(0).equals("OUT")) {
        int rs = Integer.parseInt(tokens.get(1).substring(1));
        if (label != null) {
          program.addInstruction(new Asm.IO(label, Asm.IO.Op.OUT, rs));
        } else {
          program.addInstruction(new Asm.IO(Asm.IO.Op.OUT, rs));
        }
      } else if (tokens.get(0).equals("STOP")) {
        if (label != null) {
          program.addInstruction(new Asm.Stop(label));
        } else {
          program.addInstruction(new Asm.Stop());
        }
      } else {
        System.err.println("Unknown instruction: " + tokens.get(0));
        throw new RuntimeException("Unknown instruction: " + tokens.get(0));
      }
    }

    return program;
  }
}
