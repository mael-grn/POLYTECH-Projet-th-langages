package RegisterAllocator;

/*
 * Pour les tests :
 * Générateur de classe Program à partir d'un code assembleur en texte
 */

import Asm.Program;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ProgramGenerator {
  private static final List<String> UAL_INSTRUCTIONS = Arrays.stream(new String[]{
          "ADD", "SUB", "MUL", "DIV", "MOD", "XOR", "AND", "OR", "SL", "SR"
  }).toList();
  private static final List<String> UALi_INSTRUCTIONS = Arrays.stream(new String[]{
          "ADDi", "SUBi", "MULi", "DIVi", "MODi", "XORi", "ANDi", "ORi", "SLi", "SRi"
  }).toList();
  private static final List<String> JUMPCALL_INSTRUCTIONS = Arrays.stream(new String[]{
          "JMP", "CALL"
  }).toList();
  private static final List<String> CONDJUMP_INSTRUCTIONS = Arrays.stream(new String[]{
          "JINF", "JEQU", "JSUP", "JNEQ", "JIEQ", "JSEQ"
  }).toList();
  private static final List<String> IO_INSTRUCTIONS = Arrays.stream(new String[]{
          "IN", "OUT", "PRINT", "READ"
  }).toList();
  private static final List<String> MEM_INSTRUCTIONS = Arrays.stream(new String[]{
          "LD", "ST"
  }).toList();


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

      String op = tokens.get(0);

      if (UAL_INSTRUCTIONS.contains(op)) {
        int rd = Integer.parseInt(tokens.get(1).substring(1));
        int rs1 = Integer.parseInt(tokens.get(2).substring(1));
        int rs2 = Integer.parseInt(tokens.get(3).substring(1));
        Asm.UAL.Op operation = Asm.UAL.Op.valueOf(op);
        if (label != null) {
          program.addInstruction(new Asm.UAL(label, operation, rd, rs1, rs2));
        } else {
          program.addInstruction(new Asm.UAL(operation, rd, rs1, rs2));
        }
      } else if (UALi_INSTRUCTIONS.contains(op)) {
        int rd = Integer.parseInt(tokens.get(1).substring(1));
        int rs1 = Integer.parseInt(tokens.get(2).substring(1));
        int imm = Integer.parseInt(tokens.get(3));
        Asm.UALi.Op operation = Asm.UALi.Op.valueOf(op.substring(0, op.length() - 1));
        if (label != null) {
          program.addInstruction(new Asm.UALi(label, operation, rd, rs1, imm));
        } else {
          program.addInstruction(new Asm.UALi(operation, rd, rs1, imm));
        }
      } else if (JUMPCALL_INSTRUCTIONS.contains(op)) {
        String destLabel = tokens.get(1);
        Asm.JumpCall.Op operation = Asm.JumpCall.Op.valueOf(op);
        if (label != null) {
          program.addInstruction(new Asm.JumpCall(label, operation, destLabel));
        } else {
          program.addInstruction(new Asm.JumpCall(operation, destLabel));
        }
      } else if (CONDJUMP_INSTRUCTIONS.contains(op)) {
        int rs1 = Integer.parseInt(tokens.get(1).substring(1));
        int rs2 = Integer.parseInt(tokens.get(2).substring(1));
        String destLabel = tokens.get(3);
        Asm.CondJump.Op operation = Asm.CondJump.Op.valueOf(op);
        if (label != null) {
          program.addInstruction(new Asm.CondJump(label, operation, rs1, rs2, destLabel));
        } else {
          program.addInstruction(new Asm.CondJump(operation, rs1, rs2, destLabel));
        }
      } else if (IO_INSTRUCTIONS.contains(op)) {
        int rs = Integer.parseInt(tokens.get(1).substring(1));
        Asm.IO.Op operation = Asm.IO.Op.valueOf(op);
        if (label != null) {
          program.addInstruction(new Asm.IO(label, operation, rs));
        } else {
          program.addInstruction(new Asm.IO(operation, rs));
        }
      } else if (MEM_INSTRUCTIONS.contains(op)) {
        int rd = Integer.parseInt(tokens.get(1).substring(1));
        int addr = Integer.parseInt(tokens.get(2).substring(1));
        Asm.Mem.Op operation = Asm.Mem.Op.valueOf(op);
        if (label != null) {
          program.addInstruction(new Asm.Mem(label, operation, rd, addr));
        } else {
          program.addInstruction(new Asm.Mem(operation, rd, addr));
        }
      } else if (op.equals("RET")) {
        if (label != null) {
          program.addInstruction(new Asm.Ret(label));
        } else {
          program.addInstruction(new Asm.Ret());
        }
      } else if (op.equals("STOP")) {
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
