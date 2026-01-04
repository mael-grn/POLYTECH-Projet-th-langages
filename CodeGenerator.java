import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import Asm.*;
import org.antlr.v4.runtime.tree.AbstractParseTreeVisitor;
import Type.Type;
import Type.UnknownType;
import org.antlr.v4.runtime.tree.ParseTree;
import Asm.IO;
public class CodeGenerator extends AbstractParseTreeVisitor<Program> implements grammarTCLVisitor<Program> {



    private Map<UnknownType, Type> types;
    private int resultRegister;
    private int heapPointer = 1000;
    private final int stackPointerRegister = 1; // ou 1


    private int registerCounter = 0; // Compteur de registre pour suivre leur utilisation
    private Map<String, Integer> variableRegisters = new HashMap<>(); // Table de symboles pour associer les variables à leurs registres

    public CodeGenerator(Map<UnknownType, Type> types) {
        this.types = types;
    }
    private final int returnRegister = 0;
    private boolean isReserved(int r) {
        return r == returnRegister || r == stackPointerRegister;
    }
    /**
     * Génère un nouveau numéro de registre unique.
     * @return le numéro du nouveau registre
     */
    private int newRegister() {
        do {
            registerCounter++;
        } while (isReserved(registerCounter));
        return registerCounter;
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
    public int getResultRegister() {
        return resultRegister;
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

        Instruction lastInstr = p.getInstructions().get(p.getInstructions().size() - 1);

        if (lastInstr instanceof UAL) {
            return ((UAL) lastInstr).getDest();
        } else if (lastInstr instanceof UALi) {
            return ((UALi) lastInstr).getDest();
        } else if (lastInstr instanceof Mem) {
            return ((Mem) lastInstr).getDest();
        }
        throw new RuntimeException("Type d'instruction non supporté");
    }
    public void setResultRegister(int r) {
        resultRegister = r;
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

        int value = Integer.parseInt(ctx.INT().getText());

        // Allouer un registre pour cette constante
        int destRegister = newRegister();

        // dest = 0
        program.addInstruction(new UAL(UAL.Op.XOR, destRegister, destRegister, destRegister));

        // dest = value
        program.addInstruction(new UALi(UALi.Op.ADD, destRegister, destRegister, value));

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

        // les parentheses n'affectent pas la génération de code; on visite simplement l'expression interne
        return visit(ctx.expr());

    }

    @Override
    public Program visitCall(grammarTCLParser.CallContext ctx) {
        Program program = new Program();
        int nbArgs = ctx.expr().size();

        // push des arguments en ordre inverse
        for (int i = nbArgs - 1; i >= 0; i--) {
            Program argProg = visit(ctx.expr(i));
            program.addInstructions(argProg);
            int argReg = getResultRegister(argProg);

            // SP = SP - 1
            program.addInstruction(
                    new UALi(UALi.Op.ADD, stackPointerRegister, stackPointerRegister, -1)
            );

            // MEM[SP] = arg
            program.addInstruction(
                    new Mem(Mem.Op.ST, argReg, stackPointerRegister)
            );
        }

        // CALL f
        String fctName = ctx.VAR().getText();
        program.addInstruction(
                new JumpCall(JumpCall.Op.CALL, fctName)
        );

        // nettoyage pile : SP = SP + nbArgs
        if (nbArgs > 0) {
            program.addInstruction(
                    new UALi(UALi.Op.ADD, stackPointerRegister, stackPointerRegister, nbArgs)
            );
        }

        // résultat : R0 -> registre résultat
        int destReg = newRegister();
        program.addInstruction(
                new UALi(UALi.Op.ADD, destReg, 0, 0)
        );

        return program;
    }




    @Override
    public Program visitBoolean(grammarTCLParser.BooleanContext ctx) {
        Program program = new Program();

        // "true" -> 1, "false" -> 0
        int value = ctx.BOOL().getText().equals("true") ? 1 : 0;

        // Allouer un registre pour cette constante
        int destRegister = newRegister();

        // dest = 0
        program.addInstruction(new UAL(UAL.Op.XOR, destRegister, destRegister, destRegister));

        // dest = value (si value == 1, on ajoute 1)
        if (value != 0) {
            program.addInstruction(new UALi(UALi.Op.ADD, destRegister, destRegister, 1));
        }

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
        int tabStartReg = newRegister();
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
        // Les types servent à l'analyseur mais ne génèrent pas de code
        return new Program();
    }

    @Override
    public Program visitDeclaration(grammarTCLParser.DeclarationContext ctx) {
        Program program = new Program();

        String varName = ctx.children.get(1).getText();
        int destRegister = newRegister(varName);

        // Si une expression d'initialisation est présente : on calcule puis on ASSIGNE
        if (ctx.expr() != null) {
            Program exprProgram = visit(ctx.expr());
            program.addInstructions(exprProgram);

            int exprReg = getResultRegister(exprProgram);

            // x = expr (MOV via ADD imm 0)
            program.addInstruction(new UALi(UALi.Op.ADD, destRegister, exprReg, 0));
        } else {
            // Pas d'initialisation -> x = 0
            program.addInstruction(new UAL(UAL.Op.XOR, destRegister, destRegister, destRegister));
        }

        return program;
    }


    @Override
public Program visitPrint(grammarTCLParser.PrintContext ctx) {
    Program program = new Program();

    // 1. On ne visite pas d'expression car ta grammaire dit print(VAR)
    // Mais on sait que juste avant, une déclaration ou une assignation 
    // a mis une valeur dans un registre.
    
    // 2. On récupère le dernier registre utilisé par le compteur
    int lastReg = getLastUsedRegister();

    // 3. On génère l'instruction PRINT sur ce registre
    program.addInstruction(new IO(IO.Op.PRINT, lastReg));

    return program;
}

    

    @Override
    public Program visitAssignment(grammarTCLParser.AssignmentContext ctx) {
        Program program = new Program();

        String varName = ctx.children.get(0).getText();
        Integer destRegister = variableRegisters.get(varName);
        if (destRegister == null) {
            throw new RuntimeException("Variable non déclarée : " + varName);
        }


        // Récupérer l'expression de droite (première expr trouvée)
        grammarTCLParser.ExprContext rhs = ctx.expr(0); // <-- si ça compile, garde ça
        Program exprProgram = visit(rhs);
        

        program.addInstructions(exprProgram);
        int exprReg = getResultRegister(exprProgram);

        program.addInstruction(new UALi(UALi.Op.ADD, destRegister, exprReg, 0));
        return program;
    }



    @Override
    public Program visitBlock(grammarTCLParser.BlockContext ctx) {
        // Rien de spécial à faire pour un bloc, on visite simplement ses enfants
        return visitChildren(ctx);
    }

    @Override
    public Program visitIf(grammarTCLParser.IfContext ctx) {
        // Visite de l'expression conditionnelle
        Program expProgram = visit(ctx.expr());

        // Visite du bloc de l'instruction if
        Program ifBockProgram = visit(ctx.children.get(4));

        // verification de l'existence d'un else
        boolean hasElse = ctx.children.size() > 5;

        // Visite du bloc de l'instruction else
        Program elseBlockProgram = new Program();
        if (hasElse) {
            elseBlockProgram = visit(ctx.children.get(6));
        }

        // Récupération du registre contenant le résultat de la condition
        int conditionRegister = getResultRegister(expProgram);

        // Stockage de la valeur 0 (false) dans un registre temporaire
        int falseRegister = newRegister();
        expProgram.addInstruction(new UAL(UAL.Op.XOR, falseRegister, falseRegister, falseRegister));

        // Instruction de saut conditionnel (si conditionRegister == 0 (false), sauter le bloc)
        if (hasElse) {
            expProgram.addInstruction(new CondJump(CondJump.Op.JEQU, conditionRegister, falseRegister, "else_" + conditionRegister));
        } else {
            expProgram.addInstruction(new CondJump(CondJump.Op.JEQU, conditionRegister, falseRegister, "end_if_" + conditionRegister));
        }

        // ajout du bloc if
        expProgram.addInstructions(ifBockProgram);

        // Si il y a un else, on ajoute une instruction de saut pour sauter le bloc else après le if
        if (hasElse) {
            expProgram.addInstruction(new JumpCall(JumpCall.Op.JMP, "end_if_" + conditionRegister));

            // Ajout du label else
            Instruction elseLabel = new UAL(UAL.Op.XOR, falseRegister, falseRegister, falseRegister);
            elseLabel.setLabel("else_" + conditionRegister);
            expProgram.addInstruction(elseLabel);

            // Ajout du bloc else
            expProgram.addInstructions(elseBlockProgram);
        }

        // Ajout d'un label de fin pour le saut conditionnel
        // Nous utilisons une instruction inutile pour pouvoir y attacher un label
        Instruction endIfLabel = new UAL(UAL.Op.XOR, falseRegister, falseRegister, falseRegister);
        endIfLabel.setLabel("end_if_" + conditionRegister);
        expProgram.addInstruction(endIfLabel);

        return expProgram;
    }

    @Override
    public Program visitWhile(grammarTCLParser.WhileContext ctx) {
        // Visite de l'expression conditionnelle
        Program expProgram = visit(ctx.expr());

        // Visite du bloc de l'instruction while
        Program blockProgram = visitChildren(ctx);

        // Récupération du registre contenant le résultat de la condition
        int conditionRegister = getResultRegister(expProgram);

        // Stockage de la valeur 0 (false) dans un registre temporaire
        int falseRegister = newRegister();
        expProgram.addInstruction(new UAL(UAL.Op.XOR, falseRegister, falseRegister, falseRegister));

        // Instruction de saut conditionnel (si conditionRegister == 0 (false), sauter le bloc)
        Instruction condInstr = new CondJump(CondJump.Op.JEQU, conditionRegister, falseRegister, "end_while_" + conditionRegister);
        condInstr.setLabel("start_while_" + conditionRegister);
        expProgram.addInstruction(condInstr);

        // Fusion des programmes
        expProgram.addInstructions(blockProgram);

        // Ajout de l'instruction de retour au début de la condition
        expProgram.addInstruction(new JumpCall(JumpCall.Op.JMP, "start_while_" + conditionRegister));

        // Ajout d'un label de fin pour le saut conditionnel
        // Nous utilisons une instruction inutile pour pouvoir y attacher un label
        Instruction endIfLabel = new UAL(UAL.Op.XOR, falseRegister, falseRegister, falseRegister);
        endIfLabel.setLabel("end_while_" + conditionRegister);
        expProgram.addInstruction(endIfLabel);

        return expProgram;
    }

    @Override
    public Program visitFor(grammarTCLParser.ForContext ctx) {
        // on ne peut pas visiter l'expression de ctx, car elle ne gère pas l'initialisation de la variable dans la condition de la boucle.
        // on va donc faire les choses manuellement.
        // dans les enfants de ctx, on a :
        // 0 : 'for'
        // 1 : '('
        // 2 : initialisation (declaration ou assignment)
        // 3 : ','
        // 4 : condition (expression)
        // 5 : ','
        // 6 : incrémentation (assignment)
        // 7 : ')'
        // 8 : bloc

        Program program = new Program();

        // Visite de l'expression conditionnelle
        Program initFormProgram = visit(ctx.children.get(2));

        // Visite de la condition
        Program expProgram = visit(ctx.children.get(4));

        // Récupération du registre contenant le résultat de la condition
        int conditionRegister = getResultRegister(expProgram);

        // Visite de l'instruction d'incrémentation
        Program incrProgram = visit(ctx.children.get(6));

        // Visite du bloc de l'instruction for
        Program blockProgram = visit(ctx.children.get(8));

        // Fusion des programmes
        program.addInstructions(initFormProgram);
        program.addInstructions(expProgram);

        // Stockage de la valeur 0 (false) dans un registre temporaire
        int falseRegister = newRegister();
        expProgram.addInstruction(new UAL(UAL.Op.XOR, falseRegister, falseRegister, falseRegister));

        // Instruction de saut conditionnel (si conditionRegister == 0 (false), sauter le bloc)
        Instruction condInstr = new CondJump(CondJump.Op.JEQU, conditionRegister, falseRegister, "end_for_" + conditionRegister);
        condInstr.setLabel("start_for_" + conditionRegister);
        program.addInstruction(condInstr);

        // Fusion des programmes
        program.addInstructions(blockProgram);
        program.addInstructions(incrProgram);

        // Ajout de l'instruction de retour au début de la condition
        program.addInstruction(new JumpCall(JumpCall.Op.JMP, "start_for_" + conditionRegister));

        // Ajout d'un label de fin pour le saut conditionnel
        // Nous utilisons une instruction inutile pour pouvoir y attacher un label
        Instruction endIfLabel = new UAL(UAL.Op.XOR, falseRegister, falseRegister, falseRegister);
        endIfLabel.setLabel("end_for_" + conditionRegister);
        program.addInstruction(endIfLabel);

        return program;
    }

    @Override
    public Program visitReturn(grammarTCLParser.ReturnContext ctx) {
        Program program = visit(ctx.expr());
        int resultReg = getResultRegister(program);
        int resReg = getResultRegister(program);
        program.addInstruction(new UALi(UALi.Op.ADD, returnRegister, resReg, 0));
        program.addInstruction(new Ret());
        System.out.println("visit return");
        return program;
    }

    @Override
    public Program visitCore_fct(grammarTCLParser.Core_fctContext ctx) {
        Program p = new Program();

        // instructions normales
        for (grammarTCLParser.InstrContext ic : ctx.instr()) {
            Program ip = visit(ic);
            if (ip != null) {
                p.addInstructions(ip);
            }
        }

        // s'il y a un return
        if (ctx.RETURN() != null) {

            // === OPTIMISATION : return d'une simple variable ===
            // Si l'expression est juste un identifiant (ex: "x"), on peut copier directement
            // le registre de x dans R0, sans passer par visitVariable (qui ferait une copie inutile).
            String exprText = ctx.expr().getText();
            Integer varReg = variableRegisters.get(exprText);

            if (varReg != null) {
                // R0 <- varReg (seulement si nécessaire)
                if (varReg != returnRegister) {
                    p.addInstruction(new UALi(UALi.Op.ADD, returnRegister, varReg, 0));
                }
                p.addInstruction(new Ret());
                return p; // on termine le core_fct ici
            }

            // === CAS GENERAL : return d'une vraie expression ===
            Program ep = visit(ctx.expr());
            p.addInstructions(ep);

            int res = getResultRegister(ep);

            if (res != returnRegister) {
                p.addInstruction(new UALi(UALi.Op.ADD, returnRegister, res, 0));
            }
            p.addInstruction(new Ret());
        }

        return p;
    }

    @Override
    public Program visitDecl_fct(grammarTCLParser.Decl_fctContext ctx) {
        Program program = new Program();

        // Nom de la fonction
        String fctName = ctx.VAR(0).getText();

        // =========================
        // Prologue : récupérer les arguments depuis la pile
        // =========================

        int nbParams = ctx.VAR().size() - 1;

        for (int i = 0; i < nbParams; i++) {

            // Nom du paramètre
            String paramName = ctx.VAR(i + 1).getText();

            // Nouveau registre pour le paramètre
            int paramReg = newRegister(paramName);

            // LD paramReg <- [SP]
            program.addInstruction(
                    new Mem(Mem.Op.LD, paramReg, stackPointerRegister)
            );

            // SP++
            program.addInstruction(
                    new UALi(UALi.Op.ADD, stackPointerRegister, stackPointerRegister, 1)
            );
        }


        // =========================
        // Corps de la fonction
        // =========================
        Program body = visit(ctx.core_fct());
        program.addInstructions(body);

        // Mettre le label sur la première instruction
        if (!program.getInstructions().isEmpty()) {
            program.getInstructions().getFirst().setLabel(fctName);
        }

        return program;
    }
    @Override
    public Program visitMain(grammarTCLParser.MainContext ctx) {
        Program p = new Program();

        // 1) Générer toutes les fonctions déclarées avant main
        for (grammarTCLParser.Decl_fctContext f : ctx.decl_fct()) {
            Program fp = visit(f);
            if (fp != null) p.addInstructions(fp);
        }

        // 2) Générer le corps de main
        Program mainProg = visit(ctx.core_fct());
        if (mainProg != null) {
            // Mettre le label "main" sur la 1ère instruction du corps de main
            if (!mainProg.getInstructions().isEmpty()) {
                mainProg.getInstructions().get(0).setLabel("main");
            }
            p.addInstructions(mainProg);
        }
        p.addInstruction(new Stop());
        return p;
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
