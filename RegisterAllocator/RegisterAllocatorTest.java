package RegisterAllocator;

import Asm.*;
import Graph.UnorientedGraph;

import java.util.List;

public class RegisterAllocatorTest {
  public static void main(String[] args) {
    Program p;

        /* p = ProgramGenerator.compile(new String[] {
                "       XOR R0 R0 R0",
                "       ADDi R0 R0 5",
                "       JMP test",
                "       XOR R0 R0 R0",
                "test:  ADDi R1 R0 5"
        }); */

        /* p = ProgramGenerator.compile(new String[] {
                "       XOR R0 R0 R0",
                "       XOR R1 R1 R1",
                "       ADDi R0 R0 5",
                "       SUBi R1 R1 5",
                "       JEQU R1 R0 test",
                "       XOR R0 R0 R0",
                "test:  ADDi R1 R0 5"
        }); */

        /* p = ProgramGenerator.compile(new String[] {
                "     XOR R0 R0 R0",
                "     XOR R1 R1 R1",
                "     XOR R2 R2 R2",
                "     ADDi R0 R0 9",
                "     JMP B",
                "A:   ADDi R1 R1 1",
                "     ADDi R0 R0 4",
                "     RET",
                "B:   CALL A",
                "     ADDi R2 R2 2"
        }); */

    p = ProgramGenerator.compile(new String[] {
            "     XOR R0 R0 R0",
            "     ADDi R1 R0 1",
            "     XOR R2 R2 R2",
            "     XOR R3 R3 R3",
            "     ADDi R4 R3 3",
            "WH:  JSEQ R2 R4 EWH",
            "     ADD R1 R1 R1",
            "     CALL INC",
            "     JMP WH",
            "EWH: OUT R1",
            "     OUT R2",
            "     STOP",
            "INC: ADDi R2 R2 1",
            "     RET"
    });

    RegisterAllocator ra = new RegisterAllocator(p);
    System.out.println(ra.minimizeRegisters());
  }
}
