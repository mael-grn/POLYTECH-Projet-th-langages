
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.antlr.v4.runtime.tree.AbstractParseTreeVisitor;

import Type.Type;
import Type.PrimitiveType;
import Type.UnknownType;
import Type.ArrayType;
import Type.FunctionType;
public class TyperVisitor extends AbstractParseTreeVisitor<Type> implements grammarTCLVisitor<Type> {

    private Map<UnknownType,Type> types = new HashMap<UnknownType,Type>();
    private Map<String, Type> symbolTable = new HashMap<>();
    private Type currentFunctionReturnType = null;
    public Map<UnknownType, Type> getTypes() {
        return types;
    }

    @Override
    public Type visitNegation(grammarTCLParser.NegationContext ctx) {
        // Done
        Type t = visit(ctx.expr());
        Type boolType = new PrimitiveType(Type.Base.BOOL);

        Map<UnknownType, Type> res = t.unify(boolType);
        this.updateSubstitutions(t.unify(boolType));
        return boolType;
    }

    @Override
    public Type visitComparison(grammarTCLParser.ComparisonContext ctx) {
        // Done
        Type leftType = visit(ctx.expr(0));
        Type rightType = visit(ctx.expr(1));
        Type intType = new PrimitiveType(Type.Base.INT);
        this.updateSubstitutions(leftType.unify(intType));
        this.updateSubstitutions(rightType.unify(intType));

        return new PrimitiveType(Type.Base.BOOL);
    }
    private void updateSubstitutions(Map<UnknownType, Type> newSb) {
        if (newSb == null) return;
        for (Map.Entry<UnknownType, Type> entry : newSb.entrySet()) {
            UnknownType v = entry.getKey();
            Type t = entry.getValue();
            this.types.replaceAll((var, existingType) -> existingType.substitute(v,t));
            this.types.put(v,t);
        }
    }
    @Override
    public Type visitOr(grammarTCLParser.OrContext ctx) {
        // Done
        Type leftType = visit(ctx.expr(0));
        Type rightType = visit(ctx.expr(1));
        Type boolType = new PrimitiveType(Type.Base.BOOL);

        this.updateSubstitutions(leftType.unify(boolType));
        this.updateSubstitutions(rightType.unify(boolType));
        return boolType;
    }

    @Override
    public Type visitOpposite(grammarTCLParser.OppositeContext ctx) {
        // Dnoe
        Type t = visit(ctx.expr());
        Type intType = new PrimitiveType(Type.Base.INT);
        this.updateSubstitutions(t.unify(intType));
        return intType;
    }

    @Override
    public Type visitInteger(grammarTCLParser.IntegerContext ctx) {
        //Done
        return new PrimitiveType(Type.Base.INT);
    }

    @Override
    public Type visitTab_access(grammarTCLParser.Tab_accessContext ctx) {
        // Done
        Type tabType = ctx.expr(0).accept(this);
        Type t = ctx.expr(1).accept(this);

        this.updateSubstitutions(t.unify(new PrimitiveType(Type.Base.INT)));
        Type contentVar = new UnknownType();
        Type expectedTabType = new ArrayType(contentVar);
        this.updateSubstitutions(tabType.unify(expectedTabType));

        return applyAll(contentVar);
    }
    private Type applyAll(Type t){
        Type result = t;
        for (Map.Entry<UnknownType, Type> entry : types.entrySet()) {
            result = result.substitute(entry.getKey(), entry.getValue());
        }
        return result;
    }

    @Override
    public Type visitBrackets(grammarTCLParser.BracketsContext ctx) {
        // Done
        return ctx.expr().accept(this);
    }

    @Override
    public Type visitBoolean(grammarTCLParser.BooleanContext ctx) {
        // Done
        return new PrimitiveType(Type.Base.BOOL);
    }

    @Override
    public Type visitAnd(grammarTCLParser.AndContext ctx) {
        // Done
        Type leftType = ctx.expr(0).accept(this);
        Type rightType = ctx.expr(1).accept(this);
        Type boolType = new PrimitiveType(Type.Base.BOOL);

        this.updateSubstitutions(leftType.unify(boolType));
        this.updateSubstitutions(rightType.unify(boolType));

        return boolType;
    }

    @Override
    public Type visitVariable(grammarTCLParser.VariableContext ctx) {
        // Done
        String varName = ctx.VAR().getText();

        if (!symbolTable.containsKey((varName))) {
            symbolTable.put(varName, new UnknownType());
        }
        Type t = symbolTable.get(varName);

        return applyAll(t);
    }

    @Override
    public Type visitMultiplication(grammarTCLParser.MultiplicationContext ctx) {
        // Done
        Type leftType = ctx.expr(0).accept(this);
        Type rightType = ctx.expr(1).accept(this);
        Type intType = new PrimitiveType(Type.Base.INT);

        this.updateSubstitutions(leftType.unify(intType));
        this.updateSubstitutions(rightType.unify(intType));

        return intType;
    }

    @Override
    public Type visitEquality(grammarTCLParser.EqualityContext ctx) {
        // Done
        Type leftType = ctx.expr(0).accept(this);
        Type rightType = ctx.expr(1).accept(this);

        this.updateSubstitutions(leftType.unify(rightType));

        return new PrimitiveType(Type.Base.BOOL);
    }

    @Override
    public Type visitTab_initialization(grammarTCLParser.Tab_initializationContext ctx) {
        // Done
        Type t = new UnknownType();

        for (grammarTCLParser.ExprContext exprCtx : ctx.expr()) {
            Type curType = exprCtx.accept(this);
            this.updateSubstitutions(t.unify(curType));
            t = applyAll(t);
        }
        return new ArrayType(t);
    }

    @Override
    public Type visitAddition(grammarTCLParser.AdditionContext ctx) {
        // Done
        Type leftType = ctx.expr(0).accept(this);
        Type rightType = ctx.expr(1).accept(this);
        Type intType = new PrimitiveType(Type.Base.INT);

        this.updateSubstitutions(leftType.unify(intType));
        this.updateSubstitutions(rightType.unify(intType));

        return intType;
    }

    @Override
    public Type visitBase_type(grammarTCLParser.Base_typeContext ctx) {
        // Done
        String typeName = ctx.getText();

        if (typeName.equals("int")){
            return new PrimitiveType(Type.Base.INT);
        }
        if (typeName.equals("bool")){
            return new PrimitiveType(Type.Base.BOOL);
        }
        if (typeName.equals("auto")){
            return new UnknownType();
        }
        return null;
    }

    @Override
    public Type visitTab_type(grammarTCLParser.Tab_typeContext ctx) {
        // Done
        Type t = ctx.type().accept(this);

        return new ArrayType(t);
    }

    @Override
    public Type visitDeclaration(grammarTCLParser.DeclarationContext ctx) {
        // Done
        Type decType = ctx.type().accept(this);
        String varName = ctx.VAR().getText();


        if (symbolTable.containsKey(varName)) {
            throw new RuntimeException("ERREUR : La variable "+ varName +" déjà definis");
        }
        symbolTable.put(varName, decType);
        if (ctx.ASSIGN() !=null){
            Type initType = ctx.expr().accept(this);
            this.updateSubstitutions(decType.unify(initType));
        }

        return decType;
    }

    @Override
    public Type visitPrint(grammarTCLParser.PrintContext ctx) {
        // Done
        String varName = ctx.VAR().getText();

        if (!symbolTable.containsKey(varName)) {
            symbolTable.put(varName, new UnknownType());
        }
        Type varType = symbolTable.get(varName);

        return applyAll(varType);
    }

    @Override
    public Type visitAssignment(grammarTCLParser.AssignmentContext ctx) {
        // Done
        String varName = ctx.VAR().getText();

        if (!symbolTable.containsKey(varName)) {
            symbolTable.put(varName, new UnknownType());
        }

        Type curType = symbolTable.get(varName);

        for (grammarTCLParser.ExprContext indexCtx : ctx.expr()) {
            Type indexType = indexCtx.accept(this);
            this.updateSubstitutions(indexType.unify(new PrimitiveType(Type.Base.INT)));
            Type contentVar = new UnknownType();
            this.updateSubstitutions(curType.unify(new ArrayType(contentVar)));
            curType = applyAll(contentVar);
        }
        Type rightSideType = ctx.expr(ctx.expr().size() -1).accept(this);
        this.updateSubstitutions(curType.unify(rightSideType));
        return applyAll(curType);
    }

    @Override
    public Type visitBlock(grammarTCLParser.BlockContext ctx) {
        // Done
        Type lastType = null;
        for (grammarTCLParser.InstrContext instrCtx : ctx.instr()){
            lastType = instrCtx.accept(this);
        }
        return lastType;
    }

    @Override
    public Type visitIf(grammarTCLParser.IfContext ctx) {
        Type condType = ctx.expr().accept(this);
        this.updateSubstitutions(condType.unify(new PrimitiveType(Type.Base.BOOL)));

        // Visiter le bloc 'then'
        ctx.instr(0).accept(this);

        // Visiter le bloc 'else' s'il existe
        if (ctx.instr().size() > 1) {
            ctx.instr(1).accept(this);
        }
        return null;
    }

    @Override
    public Type visitWhile(grammarTCLParser.WhileContext ctx) {
        // Done
        Type condType = ctx.expr().accept(this);

        this.updateSubstitutions(condType.unify(new PrimitiveType(Type.Base.BOOL)));

        if (ctx.instr() !=null){
            ctx.instr().accept(this);
        }
        return null;
    }

    @Override
    public Type visitFor(grammarTCLParser.ForContext ctx) {
        // Done
        ctx.instr(0).accept(this);
        if (ctx.expr() != null) {
            Type condType = ctx.expr().accept(this);
            this.updateSubstitutions(condType.unify(new PrimitiveType(Type.Base.BOOL)));
        }
        ctx.instr(1).accept(this);

        ctx.instr(2).accept(this);

        return null;
    }

    @Override
    public Type visitReturn(grammarTCLParser.ReturnContext ctx) {
        if (currentFunctionReturnType == null) return null;

        // 1. Récupérer le type de l'expression (ex: INT ou BOOL)
        Type actRetType = ctx.expr().accept(this);

        // 2. Unifier avec le type de retour attendu de la fonction [cite: 65-71]
        // On utilise applyAll pour être sûr de comparer avec la contrainte déjà établie
        this.updateSubstitutions(applyAll(currentFunctionReturnType).unify(actRetType));

        return applyAll(actRetType);
    }
    @Override
    public Type visitCall(grammarTCLParser.CallContext ctx) {
        String fctName = ctx.VAR().getText();
        if (!symbolTable.containsKey(fctName)) {
            symbolTable.put(fctName, new UnknownType());
        }

        // On récupère le type actuel de la fonction
        Type fctType = applyAll(symbolTable.get(fctName));

        if (fctType instanceof FunctionType originalFct) {
            // --- ÉTAPE INDISPENSABLE : INSTANCIATION ---
            // On crée de nouvelles variables (?#) pour cet appel précis [cite: 74-80]
            Map<UnknownType, Type> freshMap = new HashMap<>();
            FunctionType callSignature = (FunctionType) instantiate(originalFct, freshMap);

            // On unifie les arguments passés avec les nouveaux arguments frais
            for (int i = 0; i < ctx.expr().size(); i++) {
                Type providedArg = ctx.expr(i).accept(this);
                Type expectedArg = applyAll(callSignature.getArgsType(i));
                this.updateSubstitutions(expectedArg.unify(providedArg));
            }

            // On retourne le type de retour "frais"
            return applyAll(callSignature.getReturnType());
        }
        return applyAll(fctType);
    }

    // Ajoute cette méthode utilitaire à la fin de ton TyperVisitor.java
// Elle permet de copier un type en remplaçant les inconnues par des nouvelles
    private Type instantiate(Type t, Map<UnknownType, Type> freshMap) {
        if (t instanceof UnknownType ut) {
            return freshMap.computeIfAbsent(ut, k -> new UnknownType());
        }
        if (t instanceof ArrayType at) {
            return new ArrayType(instantiate(at.getTabType(), freshMap));
        }
        if (t instanceof FunctionType ft) {
            Type resRet = instantiate(ft.getReturnType(), freshMap);
            ArrayList<Type> resArgs = new ArrayList<>();
            for (int i = 0; i < ft.getNbArgs(); i++) {
                resArgs.add(instantiate(ft.getArgsType(i), freshMap));
            }
            return new FunctionType(resRet, resArgs);
        }
        return t; // PrimitiveType ne change pas
    }

    @Override
    public Type visitCore_fct(grammarTCLParser.Core_fctContext ctx) {
        // 1. Visiter toutes les instructions du corps
        for (grammarTCLParser.InstrContext instrCtx : ctx.instr()) {
            instrCtx.accept(this);
        }

        // 2. Vérifier si l'expression de return final existe pour éviter le crash
        if (ctx.expr() != null) {
            Type actRetType = ctx.expr().accept(this);
            if (this.currentFunctionReturnType != null) {
                this.updateSubstitutions(actRetType.unify(this.currentFunctionReturnType));
            }
            return applyAll(actRetType);
        }

        return null;
    }

    @Override
    public Type visitDecl_fct(grammarTCLParser.Decl_fctContext ctx) {
        String fctName = ctx.VAR(0).getText();

        // 1. Sauvegarde de la table globale (on cache les variables du main) [cite: 43-44]
        Map<String, Type> globalScope = new HashMap<>(symbolTable);

        Type returnType = ctx.type(0).accept(this);
        ArrayList<Type> argsTypes = new ArrayList<>();
        for (int i = 1; i < ctx.type().size(); i++) {
            Type argType = ctx.type(i).accept(this);
            argsTypes.add(argType);
            // On ajoute l'argument localement [cite: 46-47]
            symbolTable.put(ctx.VAR(i).getText(), argType);
        }

        FunctionType fctSignature = new FunctionType(returnType, argsTypes);
        symbolTable.put(fctName, fctSignature);
        this.currentFunctionReturnType = returnType;

        ctx.core_fct().accept(this);
        this.currentFunctionReturnType = null;

        // 2. RESTAURATION : On efface les variables locales (x, etc.)
        // mais on garde la signature de la fonction pour le futur [cite: 44]
        Type finalSig = applyAll(fctSignature);
        symbolTable = globalScope;
        symbolTable.put(fctName, finalSig);

        return finalSig;
    }

    @Override
    public Type visitMain(grammarTCLParser.MainContext ctx) {
        // Done
        for (grammarTCLParser.Decl_fctContext fctCtx : ctx.decl_fct()) {
            fctCtx.accept(this);
        }
        this.currentFunctionReturnType = new PrimitiveType(Type.Base.INT);

        if (ctx.core_fct() != null) {
            ctx.core_fct().accept(this);
        }
        this.currentFunctionReturnType = null;
        return null;
    }

    // Dans TyperVisitor, ajoute :
    public Map<String, Type> getSymbolTable() {
        // Retourne une copie avec les substitutions appliquées
        Map<String, Type> result = new HashMap<>();
        for (Map.Entry<String, Type> entry : symbolTable.entrySet()) {
            result.put(entry.getKey(), applyAll(entry.getValue()));
        }
        return result;
    }


}