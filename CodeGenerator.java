import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import Asm.*;
import org.antlr.v4.runtime.tree.AbstractParseTreeVisitor;
import Type.Type;
import Type.UnknownType;

public class CodeGenerator extends AbstractParseTreeVisitor<Program> implements grammarTCLVisitor<Program> {



    private Map<UnknownType, Type> types;
    private int heapPointer = 1000;
    private int registerCounter = 0; // Compteur de registre pour suivre leur utilisation
    private Map<String, Integer> variableRegisters = new HashMap<>(); // Table de symboles pour associer les variables à leurs registres

    public CodeGenerator(Map<UnknownType, Type> types) {
        this.types = types;
    }


    /**
     * Génère un nouveau numéro de registre unique.
     * @return le numéro du nouveau registre
     */
    private int newRegister() {
        return registerCounter++;
    }

    /**
     * Pour récuperer le dernier registre utilisé
     * @return le numéro du dernier registre utilisé
     */
    private int getLastUsedRegister() {
        return registerCounter - 1;
    }

    /**
     * Génère un nouveau registre pour une variable donnée et l'associe dans la table des variables.
     * @param varName le nom de la variable
     * @return le numéro du nouveau registre
     */
    private int newRegister(String varName) {
        int reg = newRegister();
        variableRegisters.put(varName, reg);
        return reg;
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

    @Override
    public Program visitNegation(grammarTCLParser.NegationContext ctx) {
        int destRegister = getLastUsedRegister();

        //Visite de l'expression à néguer
        Program program = visit(ctx.expr());

        //Récupération du registre source
        int srcRegister = getResultRegister(program);

        //Instruction de negation de la valeur (source XOR 1)
        Instruction instruction = new UALi(UALi.Op.XOR, destRegister, srcRegister, 1);
        program.addInstruction(instruction);
        return program;
    }

    @Override
    public Program visitComparison(grammarTCLParser.ComparisonContext ctx) {
        int destReg = getLastUsedRegister();

        // Visite des deux expressions à comparer
        Program left = visit(ctx.expr(0));
        Program right = visit(ctx.expr(1));

        // On fusionne les deux programmes
        Program program = new Program();
        program.addInstructions(left);
        program.addInstructions(right);

        // Récupération des registres sources
        int leftReg = getResultRegister(left);
        int rightReg = getResultRegister(right);

        // Instruction de comparaison (soustraction)
        Instruction cmpInstr = new UAL(UAL.Op.SUB, destReg, leftReg, rightReg);
        program.addInstruction(cmpInstr);
        return program;
    }

    @Override
    public Program visitOr(grammarTCLParser.OrContext ctx) {
        int destReg = getLastUsedRegister();

        // Visite des deux expressions à comparer
        Program left = visit(ctx.expr(0));
        Program right = visit(ctx.expr(1));

        // On fusionne les deux programmes
        Program program = new Program();
        program.addInstructions(left);
        program.addInstructions(right);

        // Récupération des registres sources
        int leftReg = getResultRegister(left);
        int rightReg = getResultRegister(right);

        // Instruction OR
        Instruction orInstr = new UAL(UAL.Op.OR, destReg, leftReg, rightReg);
        program.addInstruction(orInstr);
        return program;
    }

    @Override
    public Program visitOpposite(grammarTCLParser.OppositeContext ctx) {

        int destRegister = getLastUsedRegister();

        // Visite de l'expression à opposer
        Program program = visit(ctx.expr());

        // Récupération du registre source
        int srcRegister = getResultRegister(program);

        // Remise à zéro du registre de destination puis soustraction
        Instruction zeroInstr = new UAL(UAL.Op.SUB, destRegister, srcRegister, srcRegister); //dest = src - src, dest = 0
        Instruction negInstr = new UAL(UAL.Op.SUB, destRegister, destRegister, srcRegister); //dest = dest - src, dest = -src

        program.addInstruction(zeroInstr);
        program.addInstruction(negInstr);
        return program;
    }

    @Override
    public Program visitInteger(grammarTCLParser.IntegerContext ctx) {
        Program program = new Program();

        // Récupération de la valeur entière
        int value = Integer.parseInt(ctx.INT().getText());

        // Récupération du registre de destination
        int destRegister = newRegister();

        // instruction de chargement de la valeur immédiate dans le registre
        Instruction loadInstr = new UALi(UALi.Op.ADD, destRegister, destRegister, value);
        program.addInstruction(loadInstr);
        return program;
    }

    @Override
    public Program visitTab_access(grammarTCLParser.Tab_accessContext ctx) {
        Program program = new Program();

        // Récupération du registre de début du tableau
        int tabStartRegister = variableRegisters.get(ctx.expr(0).getText());

        // Récupération de l'index d'accès
        Program indexProgram = visit(ctx.expr(1));
        program.addInstructions(indexProgram);

        // Récupération du registre contenant l'index calculé precedemment
        int indexRegister = getResultRegister(indexProgram);

        // Stockage de l'index de l'element en memoire (adresse du tableau + index)
        int addrRegister = newRegister();
        program.addInstruction(new UAL(UAL.Op.ADD, addrRegister, indexRegister, tabStartRegister));

        // Registre de destination pour la valeur lue
        int destRegister = newRegister();

        // Instruction de chargement de la valeur depuis la mémoire
        Instruction loadInstr = new Mem(Mem.Op.LD, destRegister, addrRegister);
        program.addInstruction(loadInstr);

        return program;
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
        Program program = new Program();

        // Récupération de la valeur booléenne
        String boolText = ctx.BOOL().getText();

        // Conversion en entier (1 pour true, 0 pour false)
        int value = boolText.equals("true") ? 1 : 0;

        // Récupération du registre de destination
        int destRegister = getLastUsedRegister();

        // Load la valeur du booléen (0 ou 1)
        Instruction loadInstr = new UALi(UALi.Op.ADD, destRegister, destRegister, value);
        program.addInstruction(loadInstr);
        return program;
    }

    @Override
    public Program visitAnd(grammarTCLParser.AndContext ctx) {

        int destReg = getLastUsedRegister();

        // Visite des deux expressions à comparer
        Program left = visit(ctx.expr(0));
        Program right = visit(ctx.expr(1));

        // On fusionne les deux programmes
        Program program = new Program();
        program.addInstructions(left);
        program.addInstructions(right);

        // Récupération des registres sources
        int leftReg = getResultRegister(left);
        int rightReg = getResultRegister(right);

        // Instruction AND
        Instruction andInstr = new UAL(UAL.Op.AND, destReg, leftReg, rightReg);
        program.addInstruction(andInstr);
        return program;
    }

    @Override
    public Program visitVariable(grammarTCLParser.VariableContext ctx) {
        Program program = new Program();

        // Récupération du nom de la variable
        String varName = ctx.getText();

        // Récupération du registre associé à la variable
        Integer srcRegister = variableRegisters.get(varName);
        if (srcRegister == null) {
            throw new RuntimeException("Variable non déclarée : " + varName);
        }

        // Création du registre de destination pour la copie de la valeur
        int destRegister = newRegister();

        // Instruction de chargement de la valeur de la variable dans le registre de destination
        Instruction loadInstr = new UALi(UALi.Op.ADD, destRegister, srcRegister, 0);
        program.addInstruction(loadInstr);
        return program;
    }

    @Override
    public Program visitMultiplication(grammarTCLParser.MultiplicationContext ctx) {

        // Récupération du registre de destination
        int destReg = getLastUsedRegister();

        // Visite des deux expressions à multiplier
        Program left = visit(ctx.expr(0));
        Program right = visit(ctx.expr(1));

        // On fusionne les deux programmes
        Program program = new Program();
        program.addInstructions(left);
        program.addInstructions(right);

        // Récupération des registres sources
        int leftReg = getResultRegister(left);
        int rightReg = getResultRegister(right);

        // Instruction mult
        Instruction orInstr = new UAL(UAL.Op.MUL, destReg, leftReg, rightReg);
        program.addInstruction(orInstr);
        return program;
    }

    @Override
    public Program visitEquality(grammarTCLParser.EqualityContext ctx) {
        // registre destiation qui contiendra le résultat (boolean) de la comparaison
        int destReg = getLastUsedRegister();

        // Visite des deux expressions à comparer
        Program left = visit(ctx.expr(0));
        Program right = visit(ctx.expr(1));

        // On fusionne les deux programmes
        Program program = new Program();
        program.addInstructions(left);
        program.addInstructions(right);

        // Récupération des registres sources
        int leftReg = getResultRegister(left);
        int rightReg = getResultRegister(right);

        // Instruction condition
        Instruction condInstr = new CondJump(CondJump.Op.JNEQ, rightReg, leftReg, "end_equal_" + leftReg + "_" + rightReg);

        // Instruction pour mettre 0 (false) dans le registre de destination
        Instruction setTrue = new UALi(UALi.Op.ADD, destReg, destReg, 1); // destReg = 1 (true)

        // On a un problème à cet endroit : on doit mettre un label sur la prochaine instruction, sauf que nous n'y avons pas accès.
        // Nous allons donc ajouter une instruction qui ne fait rien, qui ne sert à rien, mais qui nous permettra de mettre un label dessus.

        int tmpReg = newRegister(); // registre temporaire pour l'instruction inutile
        Instruction nopInstr = new UAL(UAL.Op.XOR, tmpReg, tmpReg, tmpReg);
        nopInstr.setLabel("end_equal_" + leftReg + "_" + rightReg);

        program.addInstruction(condInstr);
        program.addInstruction(setTrue);
        program.addInstruction(nopInstr);
        return program;
    }

    @Override
    public Program visitTab_initialization(grammarTCLParser.Tab_initializationContext ctx) {
        Program program = new Program();

        // Stockage de la valeur actuelle du pointeur de heap, correspondant au début du tableau
        int tabStartReg = getLastUsedRegister();
        program.addInstruction(new UALi(UALi.Op.ADD, tabStartReg, tabStartReg, heapPointer)); // Chargement de l'adresse du début du tableau

        // Récupération des valeurs à insérer dans le tableau
        // Chaque entrée du tableau est le registre contenant la valeur de l'expression correspondante
        ArrayList<Integer> values = new ArrayList<>();
        for (grammarTCLParser.ExprContext exprCtx : ctx.expr()) {
            Program exprProgram = visit(exprCtx);
            program.addInstructions(exprProgram);
            int valueReg = getResultRegister(exprProgram);
            values.add(valueReg);
        }

        // Registre contenant l'adresse courante pour le stockage
        int addrReg = newRegister();
        program.addInstruction(new UAL(UAL.Op.XOR, addrReg, addrReg, addrReg)); // Mise à zéro du registre
        program.addInstruction(new UALi(UALi.Op.ADD, addrReg, addrReg, tabStartReg)); // Chargement de l'adresse du début du tableau

        for (int i = 0; i < values.size(); i++) {
            int valueReg = values.get(i);

            // Stockage de la valeur dans le tableau
            program.addInstruction(new Mem(Mem.Op.ST, valueReg, addrReg));

            // Calcul de l'adresse où stocker la prochaine valeur
            heapPointer++;
            program.addInstruction(new UALi(UALi.Op.ADD, addrReg, addrReg, 1));
        }

        return program;
    }

    @Override
    public Program visitAddition(grammarTCLParser.AdditionContext ctx) {
        // Récupération du registre de destination
        int destReg = getLastUsedRegister();

        // Visite des deux expressions à multiplier
        Program left = visit(ctx.expr(0));
        Program right = visit(ctx.expr(1));

        // On fusionne les deux programmes
        Program program = new Program();
        program.addInstructions(left);
        program.addInstructions(right);

        // Récupération des registres sources
        int leftReg = getResultRegister(left);
        int rightReg = getResultRegister(right);

        // Instruction add
        Instruction orInstr = new UAL(UAL.Op.ADD, destReg, leftReg, rightReg);
        program.addInstruction(orInstr);
        return program;
    }

    @Override
    public Program visitBase_type(grammarTCLParser.Base_typeContext ctx) {
        // Les types de bases ne génèrent pas de code. Ils sont gérés dans les déclarations, donc pas de code
        return new Program();
    }

    @Override
    public Program visitTab_type(grammarTCLParser.Tab_typeContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitTab_type'");
    }

    @Override
    public Program visitDeclaration(grammarTCLParser.DeclarationContext ctx) {
        Program program = new Program();

        // Récupération du nom de la variable
        String varName = ctx.children.get(1).getText();

        // Allocation d'un nouveau registre pour la variable
        int destRegister = newRegister(varName);
        // Initialisation de la variable à 0
        Instruction zeroInstr = new UAL(UAL.Op.XOR, destRegister, destRegister, destRegister);
        program.addInstruction(zeroInstr);

        // Si une expression d'initialisation est présente
        if (ctx.expr() != null) {
            Program exprProgram = visit(ctx.expr());
            program.addInstructions(exprProgram);
        }

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
        // Rien de spécial à faire pour un bloc, on visite simplement ses enfants
        return visitChildren(ctx);
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
        // Rien de spécial à faire pour une fonction core, on visite simplement ses enfants
        return visitChildren(ctx);
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
        // Cas ou il n'y a aucune instruction dans le main
        if (program == null) {
            return new Program();
        }
        program.getInstructions().getFirst().setLabel("main");
        return program;
    }

    @Override
    protected Program defaultResult() {
        // Par défaut, on retourne un programme vide au lieu de null
        return new Program();
    }

    @Override
    protected Program aggregateResult(Program aggregate, Program nextResult) {
        // Cette méthode fusionne les résultats des enfants
        if (nextResult != null) {
            aggregate.addInstructions(nextResult);
        }
        return aggregate;
    }
}
