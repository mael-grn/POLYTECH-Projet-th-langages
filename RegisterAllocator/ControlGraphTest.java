package RegisterAllocator;

import Asm.*;
import Graph.UnorientedGraph;

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
                "EWH: JMP EWH",
                "INC: ADDi R2 R2 1",
                "     RET"
        });

        ControlGraph cfg = new ControlGraph(p);
        System.out.println(cfg.toDot());
        for (Instruction i : p.getInstructions()) {
            System.out.println("\nGEN(" + i.toString().replace("\n", "") + ") = " + CFGAnalysis.gen(i));
            System.out.println("KILL(" + i.toString().replace("\n", "") + ") = " + CFGAnalysis.kill(i));
        }

        CFGAnalysis analysis = new CFGAnalysis(cfg);
        List<CFGAnalysis.LiveVars> liveVars = analysis.computeLiveVariables();
        List<Instruction> instructions = cfg.getInstructions();
        for (int i = 0; i < instructions.size(); i++) {
            System.out.println("\nInstruction " + i + ":");
            System.out.println("LVentry = " + liveVars.get(i).entry);
            System.out.println("LVexit = " + liveVars.get(i).exit);
        }

        UnorientedGraph<Integer> interferenceGraph = analysis.buildInterferenceGraph();
        interferenceGraph.color();

        for (int i = 0; i <= 4; i++) {
            System.out.println("Reg " + i + " -> Color " + interferenceGraph.getColor(i));
        }
    }
}
