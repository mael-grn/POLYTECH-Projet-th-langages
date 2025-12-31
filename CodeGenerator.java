import java.util.Map;

import Asm.*;
import org.antlr.v4.runtime.tree.AbstractParseTreeVisitor;
import Type.Type;
import Type.UnknownType;

public class CodeGenerator extends AbstractParseTreeVisitor<Program> implements grammarTCLVisitor<Program> {

    private Map<UnknownType, Type> types;

    private int registerCounter = 0;

    /**
     * Génère un nouveau numéro de registre unique.
     * @return le numéro du nouveau registre
     */
    private int newRegister() {
        return registerCounter++;
    }

    /**
     * Pour récuperer le dernier registre utilisé dans la dernière instruction.
     * Throw si la dernière instruction n'as pas utilisé de registre
     * @param p le programme à verifier
     * @return le numéro du dernier registre
     */
    private int getResultRegister(Program p) {
        if (p.getInstructions().isEmpty()) {
            throw new RuntimeException("Le programme est vide, impossible de récupérer le registre de résultat.");
        }

        Instruction lastInstr = p.getInstructions().getLast();

        if (lastInstr instanceof UAL) {
            return ((UAL) lastInstr).getDest();
        } else if (lastInstr instanceof UALi) {
            return ((UALi) lastInstr).getDest();
        } else if (lastInstr instanceof Mem) {
            return ((Mem) lastInstr).getDest();
        }
        throw new RuntimeException("Type d'instruction non supporté");
    }

    /**
     * Constructeur
     *
     * @param types types de chaque variable du code source
     */
    public CodeGenerator(Map<UnknownType, Type> types) {
        this.types = types;
    }

    /**
     * On récupère le registre resultat, on stock sa valeur inversée (xor) dans un nouveau registre
     * @param ctx the parse tree
     * @return
     */
    @Override
    public Program visitNegation(grammarTCLParser.NegationContext ctx) {
        Program program = visitChildren(ctx);
        int srcRegister = getResultRegister(program);
        int destRegister = newRegister();
        Instruction instruction = new UALi(UALi.Op.XOR, destRegister, srcRegister, 1);
        program.addInstruction(instruction);
        return program;
    }

    @Override
    public Program visitComparison(grammarTCLParser.ComparisonContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitComparison'");
    }

    @Override
    public Program visitOr(grammarTCLParser.OrContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitOr'");
    }

    @Override
    public Program visitOpposite(grammarTCLParser.OppositeContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitOpposite'");
    }

    @Override
    public Program visitInteger(grammarTCLParser.IntegerContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitInteger'");
    }

    /**
     * On récupère le registre resultat (on suppose qu'il contient l'indice de la valeur à récuperer en mémoire)
     * On récupère la valeur correspondante en memoire et on la stock dans un nouveau registre
     * @param ctx the parse tree
     * @return
     */
    @Override
    public Program visitTab_access(grammarTCLParser.Tab_accessContext ctx) {
        // Quelles sont les expressions de ctx à ce moment de l'execution ?
        // COmment savoir a quel indice visiter et quelle tab ?

        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitTab_access'");
    }

    @Override
    public Program visitBrackets(grammarTCLParser.BracketsContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitBrackets'");
    }

    @Override
    public Program visitCall(grammarTCLParser.CallContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitCall'");
    }

    @Override
    public Program visitBoolean(grammarTCLParser.BooleanContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitBoolean'");
    }

    @Override
    public Program visitAnd(grammarTCLParser.AndContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitAnd'");
    }

    @Override
    public Program visitVariable(grammarTCLParser.VariableContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitVariable'");
    }

    @Override
    public Program visitMultiplication(grammarTCLParser.MultiplicationContext ctx) {
        //Maël
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitMultiplication'");
    }

    @Override
    public Program visitEquality(grammarTCLParser.EqualityContext ctx) {
        //Maël
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitEquality'");
    }

    @Override
    public Program visitTab_initialization(grammarTCLParser.Tab_initializationContext ctx) {
        //Maël
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitTab_initialization'");
    }

    @Override
    public Program visitAddition(grammarTCLParser.AdditionContext ctx) {
        //Maël
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitAddition'");
    }

    @Override
    public Program visitBase_type(grammarTCLParser.Base_typeContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitBase_type'");
    }

    @Override
    public Program visitTab_type(grammarTCLParser.Tab_typeContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitTab_type'");
    }

    @Override
    public Program visitDeclaration(grammarTCLParser.DeclarationContext ctx) {
        // On initialise un nouveau registre à zéro
        Program program = visitChildren(ctx);
        int destRegister = newRegister();
        Instruction instruction = new UAL(UAL.Op.XOR, destRegister, destRegister, destRegister);
        program.addInstruction(instruction);
        return program;
    }

    @Override
    public Program visitPrint(grammarTCLParser.PrintContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitPrint'");
    }

    @Override
    public Program visitAssignment(grammarTCLParser.AssignmentContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitAssignment'");
    }

    @Override
    public Program visitBlock(grammarTCLParser.BlockContext ctx) {
        //Maël
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitBlock'");
    }

    @Override
    public Program visitIf(grammarTCLParser.IfContext ctx) {
        //Maël
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitIf'");
    }

    @Override
    public Program visitWhile(grammarTCLParser.WhileContext ctx) {
        //Maël
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitWhile'");
    }

    @Override
    public Program visitFor(grammarTCLParser.ForContext ctx) {
        //Maël
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitFor'");
    }

    @Override
    public Program visitReturn(grammarTCLParser.ReturnContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitReturn'");
    }

    @Override
    public Program visitCore_fct(grammarTCLParser.Core_fctContext ctx) {
        // Il n'y a rien à faire ?
        Program program = visitChildren(ctx);
        return program;
    }

    @Override
    public Program visitDecl_fct(grammarTCLParser.Decl_fctContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitDecl_fct'");
    }

    @Override
    public Program visitMain(grammarTCLParser.MainContext ctx) {
        // Le main consiste à mettre le label main devant la prochaine instruction
        Program program = visitChildren(ctx);
        program.getInstructions().getFirst().setLabel("main");
        return program;
    }


}
