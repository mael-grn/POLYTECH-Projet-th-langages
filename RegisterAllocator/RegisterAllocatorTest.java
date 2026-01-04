package RegisterAllocator;

import Asm.Program;

public class RegisterAllocatorTest {
  public static void main(String[] args) {
    Program p;

        /* p = ProgramGenerator.compile(new String[] {
                "       XOR R2 R2 R2",
                "       ADDi R2 R2 5",
                "       JMP test",
                "       XOR R2 R2 R2",
                "test:  ADDi R3 R2 5"
        }); */

        /* p = ProgramGenerator.compile(new String[] {
                "       XOR R2 R2 R2",
                "       XOR R3 R3 R3",
                "       ADDi R2 R2 5",
                "       SUBi R3 R3 5",
                "       JEQU R3 R2 test",
                "       XOR R2 R2 R2",
                "test:  ADDi R3 R2 5"
        }); */

        /* p = ProgramGenerator.compile(new String[] {
                "     XOR R2 R2 R2",
                "     XOR R3 R3 R3",
                "     XOR R4 R4 R4",
                "     ADDi R2 R2 9",
                "     JMP B",
                "A:   ADDi R3 R3 1",
                "     ADDi R2 R2 4",
                "     RET",
                "B:   CALL A",
                "     ADDi R4 R4 2"
        }); */

    p = ProgramGenerator.compile(new String[] {
            "     XOR R2 R2 R2",
            "     ADDi R3 R2 2",
            "     XOR R4 R4 R4",
            "     XOR R5 R5 R5",
            "     ADDi R6 R5 3",
            "WH:  JSEQ R4 R6 EWH",
            "     MUL R3 R3 R3",
            "     CALL INC",
            "     JMP WH",
            "EWH: PRINT R3",
            "     PRINT R4",
            "     STOP",
            "INC: ADDi R4 R4 1",
            "     RET"
    });

    RegisterAllocator ra = new RegisterAllocator(p);
    System.out.println(ra.minimizeRegisters());
  }
}
