package RegisterAllocator;

import Asm.*;
import java.util.List;

public class ControlGraphTest {
    public static void main(String[] args) {
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
                "     XOR R1 R1 R1",
                "     XOR R2 R2 R2",
                "     ADDi R0 R0 9",
                "     JMP B",
                "A:   ADDi R1 R1 1",
                "     ADDi R0 R0 4",
                "     RET",
                "B:   CALL A",
                "     ADDi R2 R2 2"
        });

        ControlGraph cfg = new ControlGraph(p);
        System.out.println(cfg.toDot());
        for (Instruction i : p.getInstructions()) {
            System.out.println("\nGEN(" + i.toString().replace("\n", "") + ") = " + CFGAnalysis.gen(i));
            System.out.println("KILL(" + i.toString().replace("\n", "") + ") = " + CFGAnalysis.kill(i));
        }

        CFGAnalysis analysis = new CFGAnalysis(cfg);
        List<CFGAnalysis.LiveVars> liveVars = analysis.computeLiveVariables();
        List<List<Instruction>> blocks = cfg.getBlocks();
        for (int i = 0; i < blocks.size(); i++) {
            System.out.println("\nBloc " + i + ":");
            System.out.println("LVentry = " + liveVars.get(i).entry);
            System.out.println("LVexit = " + liveVars.get(i).exit);
        }
    }
}
