import java.util.ArrayList;
import java.util.HashMap;
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
        Type t = visit(ctx.expr());
        Type boolType = new PrimitiveType(Type.Base.BOOL);
        this.updateSubstitutions(t.unify(boolType));
        return boolType;
    }

    @Override
    public Type visitComparison(grammarTCLParser.ComparisonContext ctx) {
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
        Type leftType = visit(ctx.expr(0));
        Type rightType = visit(ctx.expr(1));
        Type boolType = new PrimitiveType(Type.Base.BOOL);
        this.updateSubstitutions(leftType.unify(boolType));
        this.updateSubstitutions(rightType.unify(boolType));
        return boolType;
    }

    @Override
    public Type visitOpposite(grammarTCLParser.OppositeContext ctx) {
        Type t = visit(ctx.expr());
        Type intType = new PrimitiveType(Type.Base.INT);
        this.updateSubstitutions(t.unify(intType));
        return intType;
    }

    @Override
    public Type visitInteger(grammarTCLParser.IntegerContext ctx) {
        return new PrimitiveType(Type.Base.INT);
    }

    @Override
    public Type visitTab_access(grammarTCLParser.Tab_accessContext ctx) {
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
        return ctx.expr().accept(this);
    }

    @Override
    public Type visitBoolean(grammarTCLParser.BooleanContext ctx) {
        return new PrimitiveType(Type.Base.BOOL);
    }

    @Override
    public Type visitAnd(grammarTCLParser.AndContext ctx) {
        Type leftType = ctx.expr(0).accept(this);
        Type rightType = ctx.expr(1).accept(this);
        Type boolType = new PrimitiveType(Type.Base.BOOL);
        this.updateSubstitutions(leftType.unify(boolType));
        this.updateSubstitutions(rightType.unify(boolType));
        return boolType;
    }

    @Override
    public Type visitVariable(grammarTCLParser.VariableContext ctx) {
        String varName = ctx.VAR().getText();
        if (!symbolTable.containsKey(varName)) {
            symbolTable.put(varName, new UnknownType());
        }
        Type t = symbolTable.get(varName);
        return applyAll(t);
    }

    @Override
    public Type visitMultiplication(grammarTCLParser.MultiplicationContext ctx) {
        Type leftType = ctx.expr(0).accept(this);
        Type rightType = ctx.expr(1).accept(this);
        Type intType = new PrimitiveType(Type.Base.INT);
        this.updateSubstitutions(leftType.unify(intType));
        this.updateSubstitutions(rightType.unify(intType));
        return intType;
    }

    @Override
    public Type visitEquality(grammarTCLParser.EqualityContext ctx) {
        Type leftType = ctx.expr(0).accept(this);
        Type rightType = ctx.expr(1).accept(this);
        this.updateSubstitutions(leftType.unify(rightType));
        return new PrimitiveType(Type.Base.BOOL);
    }

    @Override
    public Type visitTab_initialization(grammarTCLParser.Tab_initializationContext ctx) {
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
        Type leftType = ctx.expr(0).accept(this);
        Type rightType = ctx.expr(1).accept(this);
        Type intType = new PrimitiveType(Type.Base.INT);
        this.updateSubstitutions(leftType.unify(intType));
        this.updateSubstitutions(applyAll(rightType).unify(intType));
        return intType;
    }

    @Override
    public Type visitBase_type(grammarTCLParser.Base_typeContext ctx) {
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
        Type t = ctx.type().accept(this);
        return new ArrayType(t);
    }

    @Override
    public Type visitDeclaration(grammarTCLParser.DeclarationContext ctx) {
        String varName = ctx.VAR().getText();

        // VERIFICATION CRITIQUE :
        if (symbolTable.containsKey(varName)) {
            throw new RuntimeException("Erreur sémantique : La variable '" + varName + "' est déjà déclarée.");
        }

        Type decType = ctx.type().accept(this);
        symbolTable.put(varName, decType); // On ne l'ajoute que si elle n'existait pas

        if (ctx.ASSIGN() != null) {
            Type initType = ctx.expr().accept(this);
            this.updateSubstitutions(decType.unify(initType));
        }
        return applyAll(decType);
    }

    @Override
    public Type visitPrint(grammarTCLParser.PrintContext ctx) {
        String varName = ctx.VAR().getText();
        if (!symbolTable.containsKey(varName)) {
            symbolTable.put(varName, new UnknownType());
        }
        Type varType = symbolTable.get(varName);
        return applyAll(varType);
    }

    @Override
    public Type visitAssignment(grammarTCLParser.AssignmentContext ctx) {
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

        Type rightSideType = ctx.expr(ctx.expr().size() - 1).accept(this);
        this.updateSubstitutions(curType.unify(rightSideType));
        return applyAll(curType);
    }

    @Override
    public Type visitBlock(grammarTCLParser.BlockContext ctx) {
        Type lastType = null;
        for (grammarTCLParser.InstrContext instrCtx : ctx.instr()){
            lastType = instrCtx.accept(this);
        }
        return lastType;
    }

    @Override
    public Type visitIf(grammarTCLParser.IfContext ctx) {
        Type condType = ctx.expr().accept(this);
        Type thenType = ctx.instr(0).accept(this);
        this.updateSubstitutions(condType.unify(new PrimitiveType(Type.Base.BOOL)));
        if (ctx.instr().size() > 1){
            Type elseType = ctx.instr(1).accept(this);
        }
        return null;
    }

    @Override
    public Type visitWhile(grammarTCLParser.WhileContext ctx) {
        Type condType = ctx.expr().accept(this);
        this.updateSubstitutions(condType.unify(new PrimitiveType(Type.Base.BOOL)));
        if (ctx.instr() != null){
            ctx.instr().accept(this);
        }
        return null;
    }

    @Override
    public Type visitFor(grammarTCLParser.ForContext ctx) {
        ctx.instr(0).accept(this);
        if (ctx.expr() != null) {
            Type condType = ctx.expr().accept(this);
            this.updateSubstitutions(condType.unify(new PrimitiveType(Type.Base.BOOL)));
        }
        ctx.instr(1).accept(this);
        if (ctx.instr().size() > 2) {
            ctx.instr(2).accept(this);
        }
        return null;
    }

    @Override
    public Type visitReturn(grammarTCLParser.ReturnContext ctx) {
        if (currentFunctionReturnType == null) return null;
        Type actRetType = ctx.expr().accept(this);
        this.updateSubstitutions(actRetType.unify(currentFunctionReturnType));
        return applyAll(actRetType);
    }

    @Override
    public Type visitCall(grammarTCLParser.CallContext ctx) {
        String fctName = ctx.VAR().getText();
        if (!symbolTable.containsKey(fctName)) {
            symbolTable.put(fctName, new UnknownType());
        }

        Type fctTypeInTable = symbolTable.get(fctName);
        ArrayList<Type> providedArgs = new ArrayList<>();

        for (grammarTCLParser.ExprContext exprCtx : ctx.expr()) {
            providedArgs.add(exprCtx.accept(this));
        }

        ArrayList<Type> expectedArgsVars = new ArrayList<>();
        for (int i = 0; i < providedArgs.size(); i++) {
            expectedArgsVars.add(new UnknownType());
        }

        Type returnVar = new UnknownType();
        FunctionType callSignature = new FunctionType(returnVar, expectedArgsVars);

        this.updateSubstitutions(fctTypeInTable.unify(callSignature));

        for (int i = 0; i < providedArgs.size(); i++) {
            Type updatedExpectedType = applyAll(expectedArgsVars.get(i));
            this.updateSubstitutions(providedArgs.get(i).unify(updatedExpectedType));
        }

        return applyAll(returnVar);
    }

    @Override
    public Type visitCore_fct(grammarTCLParser.Core_fctContext ctx) {
        for (grammarTCLParser.InstrContext instrCtx : ctx.instr()) {
            instrCtx.accept(this);
        }
        Type actRetType = ctx.expr().accept(this);
        if (this.currentFunctionReturnType != null) {
            this.updateSubstitutions(actRetType.unify(this.currentFunctionReturnType));
        }
        return applyAll(actRetType);
    }

    @Override
    public Type visitDecl_fct(grammarTCLParser.Decl_fctContext ctx) {
        Type returnType = ctx.type(0).accept(this);
        String fctName = ctx.VAR(0).getText();
        ArrayList<Type> argsTypes = new ArrayList<>();

        // Sauvegarder la table des symboles actuelle
        Map<String, Type> previousSymbolTable = new HashMap<>(symbolTable);

        // Ajouter les paramètres à la table locale
        for (int i = 1; i < ctx.type().size(); i++) {
            Type argType = ctx.type(i).accept(this);
            argsTypes.add(argType);
            String paramName = ctx.VAR(i).getText();
            symbolTable.put(paramName, argType);
        }

        FunctionType fctSignature = new FunctionType(returnType, argsTypes);
        symbolTable.put(fctName, fctSignature);
        this.currentFunctionReturnType = returnType;
        ctx.core_fct().accept(this);

        // Restaurer la table des symboles
        symbolTable = previousSymbolTable;
        symbolTable.put(fctName, fctSignature);
        this.currentFunctionReturnType = null;
        return fctSignature;
    }

    @Override
    public Type visitMain(grammarTCLParser.MainContext ctx) {
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

    public Map<String, Type> getSymbolTable() {
        return this.symbolTable;
    }
}

