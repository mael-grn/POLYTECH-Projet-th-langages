import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.ArrayList;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.AbstractParseTreeVisitor;

import Type.Type;
import Type.PrimitiveType;
import Type.UnknownType;
import Type.ArrayType;
import Type.FunctionType;

public class TyperVisitor extends AbstractParseTreeVisitor<Type> implements grammarTCLVisitor<Type> {

    private Map<UnknownType,Type> types = new HashMap<UnknownType,Type>();
    private Stack<Map<String, Type>> symbolTableStack = new Stack<>();
    private Type currentFunctionReturnType = null;

    public TyperVisitor() {
        // Initialiser avec un scope global
        enterScope();
    }

    public Map<UnknownType, Type> getTypes() {
        return types;
    }

    // ==================== Méthodes utilitaires ====================

    private void enterScope() {
        symbolTableStack.push(new HashMap<>());
    }

    private void exitScope() {
        symbolTableStack.pop();
    }

    private Type lookupVariable(String name) {
        // Chercher du scope le plus récent au plus ancien
        for (int i = symbolTableStack.size() - 1; i >= 0; i--) {
            Map<String, Type> scope = symbolTableStack.get(i);
            if (scope.containsKey(name)) {
                return scope.get(name);
            }
        }
        return null;
    }

    private void addVariable(String name, Type type) {
        symbolTableStack.peek().put(name, type);
    }

    private boolean variableExistsInCurrentScope(String name) {
        return symbolTableStack.peek().containsKey(name);
    }

    private void updateSubstitutions(Map<UnknownType, Type> newSubstitutions) {
        if (newSubstitutions == null) return;

        // Appliquer les nouvelles substitutions aux types existants
        for (Map.Entry<UnknownType, Type> entry : newSubstitutions.entrySet()) {
            UnknownType v = entry.getKey();
            Type t = entry.getValue();

            // Mettre à jour toutes les entrées existantes
            for (Map.Entry<UnknownType, Type> existing : types.entrySet()) {
                existing.setValue(existing.getValue().substitute(v, t));
            }

            // Ajouter la nouvelle substitution
            types.put(v, t);
        }
    }

    private Type applyAllSubstitutions(Type t) {
        Type result = t;
        for (Map.Entry<UnknownType, Type> entry : types.entrySet()) {
            result = result.substitute(entry.getKey(), entry.getValue());
        }
        return result;
    }

    // ==================== Visiteurs pour les expressions ====================

    @Override
    public Type visitNegation(grammarTCLParser.NegationContext ctx) {
        Type exprType = visit(ctx.expr());
        Type boolType = new PrimitiveType(Type.Base.BOOL);

        updateSubstitutions(exprType.unify(boolType));
        return boolType;
    }

    @Override
    public Type visitComparison(grammarTCLParser.ComparisonContext ctx) {
        Type leftType = visit(ctx.expr(0));
        Type rightType = visit(ctx.expr(1));
        Type intType = new PrimitiveType(Type.Base.INT);

        updateSubstitutions(leftType.unify(intType));
        updateSubstitutions(rightType.unify(intType));

        return new PrimitiveType(Type.Base.BOOL);
    }

    @Override
    public Type visitOr(grammarTCLParser.OrContext ctx) {
        Type leftType = visit(ctx.expr(0));
        Type rightType = visit(ctx.expr(1));
        Type boolType = new PrimitiveType(Type.Base.BOOL);

        updateSubstitutions(leftType.unify(boolType));
        updateSubstitutions(rightType.unify(boolType));

        return boolType;
    }

    @Override
    public Type visitOpposite(grammarTCLParser.OppositeContext ctx) {
        Type exprType = visit(ctx.expr());
        Type intType = new PrimitiveType(Type.Base.INT);

        updateSubstitutions(exprType.unify(intType));
        return intType;
    }

    @Override
    public Type visitInteger(grammarTCLParser.IntegerContext ctx) {
        return new PrimitiveType(Type.Base.INT);
    }

    @Override
    public Type visitTab_access(grammarTCLParser.Tab_accessContext ctx) {
        Type arrayType = visit(ctx.expr(0));
        Type indexType = visit(ctx.expr(1));
        Type intType = new PrimitiveType(Type.Base.INT);

        // L'index doit être un entier
        updateSubstitutions(indexType.unify(intType));

        // Le tableau a un type d'éléments inconnu
        UnknownType elementType = new UnknownType();
        Type expectedArrayType = new ArrayType(elementType);

        updateSubstitutions(arrayType.unify(expectedArrayType));

        return applyAllSubstitutions(elementType);
    }

    @Override
    public Type visitBrackets(grammarTCLParser.BracketsContext ctx) {
        return visit(ctx.expr());
    }

    @Override
    public Type visitCall(grammarTCLParser.CallContext ctx) {
        String functionName = ctx.VAR().getText();
        Type functionType = lookupVariable(functionName);

        if (functionType == null) {
            throw new TypeException("Fonction non déclarée: " + functionName, ctx);
        }

        // Collecter les types des arguments
        ArrayList<Type> argumentTypes = new ArrayList<>();
        for (grammarTCLParser.ExprContext exprCtx : ctx.expr()) {
            argumentTypes.add(visit(exprCtx));
        }

        // Créer un type de fonction attendu avec des variables inconnues
        ArrayList<Type> paramVars = new ArrayList<>();
        for (int i = 0; i < argumentTypes.size(); i++) {
            paramVars.add(new UnknownType());
        }
        UnknownType returnVar = new UnknownType();
        FunctionType expectedType = new FunctionType(returnVar, paramVars);

        // Unifier avec le type de la fonction
        updateSubstitutions(functionType.unify(expectedType));

        // Unifier chaque argument avec son paramètre correspondant
        for (int i = 0; i < argumentTypes.size(); i++) {
            Type paramType = applyAllSubstitutions(paramVars.get(i));
            updateSubstitutions(argumentTypes.get(i).unify(paramType));
        }

        return applyAllSubstitutions(returnVar);
    }

    @Override
    public Type visitBoolean(grammarTCLParser.BooleanContext ctx) {
        return new PrimitiveType(Type.Base.BOOL);
    }

    @Override
    public Type visitAnd(grammarTCLParser.AndContext ctx) {
        Type leftType = visit(ctx.expr(0));
        Type rightType = visit(ctx.expr(1));
        Type boolType = new PrimitiveType(Type.Base.BOOL);

        updateSubstitutions(leftType.unify(boolType));
        updateSubstitutions(rightType.unify(boolType));

        return boolType;
    }

    @Override
    public Type visitVariable(grammarTCLParser.VariableContext ctx) {
        String varName = ctx.VAR().getText();
        Type varType = lookupVariable(varName);

        if (varType == null) {
            throw new TypeException("Variable non déclarée: " + varName, ctx);
        }

        return applyAllSubstitutions(varType);
    }

    @Override
    public Type visitMultiplication(grammarTCLParser.MultiplicationContext ctx) {
        Type leftType = visit(ctx.expr(0));
        Type rightType = visit(ctx.expr(1));
        Type intType = new PrimitiveType(Type.Base.INT);

        updateSubstitutions(leftType.unify(intType));
        updateSubstitutions(rightType.unify(intType));

        return intType;
    }

    @Override
    public Type visitEquality(grammarTCLParser.EqualityContext ctx) {
        Type leftType = visit(ctx.expr(0));
        Type rightType = visit(ctx.expr(1));

        // Les deux côtés doivent avoir le même type
        updateSubstitutions(leftType.unify(rightType));

        return new PrimitiveType(Type.Base.BOOL);
    }

    @Override
    public Type visitTab_initialization(grammarTCLParser.Tab_initializationContext ctx) {
        if (ctx.expr().isEmpty()) {
            // Tableau vide : type inconnu
            return new ArrayType(new UnknownType());
        }

        // Type du premier élément
        Type firstType = visit(ctx.expr(0));
        Type elementType = applyAllSubstitutions(firstType);

        // Vérifier que tous les éléments ont le même type
        for (int i = 1; i < ctx.expr().size(); i++) {
            Type currentType = visit(ctx.expr(i));
            updateSubstitutions(currentType.unify(elementType));
            elementType = applyAllSubstitutions(elementType);
        }

        return new ArrayType(elementType);
    }

    @Override
    public Type visitAddition(grammarTCLParser.AdditionContext ctx) {
        Type leftType = visit(ctx.expr(0));
        Type rightType = visit(ctx.expr(1));
        Type intType = new PrimitiveType(Type.Base.INT);

        updateSubstitutions(leftType.unify(intType));
        updateSubstitutions(rightType.unify(intType));

        return intType;
    }

    // ==================== Visiteurs pour les types ====================

    @Override
    public Type visitBase_type(grammarTCLParser.Base_typeContext ctx) {
        String typeName = ctx.getText();

        switch (typeName) {
            case "int":
                return new PrimitiveType(Type.Base.INT);
            case "bool":
                return new PrimitiveType(Type.Base.BOOL);
            case "auto":
                return new UnknownType();
            default:
                throw new TypeException("Type inconnu: " + typeName, ctx);
        }
    }

    @Override
    public Type visitTab_type(grammarTCLParser.Tab_typeContext ctx) {
        Type elementType = visit(ctx.type());
        return new ArrayType(elementType);
    }

    // ==================== Visiteurs pour les instructions ====================

    @Override
    public Type visitDeclaration(grammarTCLParser.DeclarationContext ctx) {
        Type declaredType = visit(ctx.type());
        String varName = ctx.VAR().getText();

        // Vérifier la redéclaration
        if (variableExistsInCurrentScope(varName)) {
            throw new TypeException("Variable déjà déclarée: " + varName, ctx);
        }

        addVariable(varName, declaredType);

        // Si il y a une initialisation
        if (ctx.expr() != null) {
            Type initType = visit(ctx.expr());
            updateSubstitutions(declaredType.unify(initType));
        }

        return applyAllSubstitutions(declaredType);
    }

    @Override
    public Type visitPrint(grammarTCLParser.PrintContext ctx) {
        String varName = ctx.VAR().getText();
        Type varType = lookupVariable(varName);

        if (varType == null) {
            throw new TypeException("Variable non déclarée: " + varName, ctx);
        }

        // print peut afficher n'importe quel type
        return applyAllSubstitutions(varType);
    }

    @Override
    public Type visitAssignment(grammarTCLParser.AssignmentContext ctx) {
        String varName = ctx.VAR().getText();
        Type varType = lookupVariable(varName);

        if (varType == null) {
            throw new TypeException("Variable non assignée: " + varName, ctx);
        }

        Type currentType = varType;

        // Gérer les indices pour les tableaux
        int numIndices = ctx.expr().size() - 1;
        for (int i = 0; i < numIndices; i++) {
            Type indexType = visit(ctx.expr(i));
            Type intType = new PrimitiveType(Type.Base.INT);
            updateSubstitutions(indexType.unify(intType));

            UnknownType elementType = new UnknownType();
            Type expectedArrayType = new ArrayType(elementType);
            updateSubstitutions(currentType.unify(expectedArrayType));

            currentType = applyAllSubstitutions(elementType);
        }

        // Type de la valeur à assigner
        Type valueType = visit(ctx.expr(ctx.expr().size() - 1));
        updateSubstitutions(currentType.unify(valueType));

        return applyAllSubstitutions(currentType);
    }

    @Override
    public Type visitBlock(grammarTCLParser.BlockContext ctx) {
        enterScope();

        Type lastType = null;
        for (grammarTCLParser.InstrContext instrCtx : ctx.instr()) {
            lastType = visit(instrCtx);
        }

        exitScope();
        return lastType;
    }

    @Override
    public Type visitIf(grammarTCLParser.IfContext ctx) {
        Type conditionType = visit(ctx.expr());
        Type boolType = new PrimitiveType(Type.Base.BOOL);

        updateSubstitutions(conditionType.unify(boolType));

        // Visiter le then
        visit(ctx.instr(0));

        // Visiter le else si présent
        if (ctx.instr().size() > 1) {
            visit(ctx.instr(1));
        }

        return null; // if n'a pas de type de retour
    }

    @Override
    public Type visitWhile(grammarTCLParser.WhileContext ctx) {
        Type conditionType = visit(ctx.expr());
        Type boolType = new PrimitiveType(Type.Base.BOOL);

        updateSubstitutions(conditionType.unify(boolType));

        // Visiter le corps
        if (ctx.instr() != null) {
            visit(ctx.instr());
        }

        return null; // while n'a pas de type de retour
    }

    @Override
    public Type visitFor(grammarTCLParser.ForContext ctx) {
        enterScope();

        // Initialisation
        if (ctx.instr(0) != null) {
            visit(ctx.instr(0));
        }

        // Condition
        if (ctx.expr() != null) {
            Type conditionType = visit(ctx.expr());
            Type boolType = new PrimitiveType(Type.Base.BOOL);
            updateSubstitutions(conditionType.unify(boolType));
        }

        // Incrémentation
        if (ctx.instr(1) != null) {
            visit(ctx.instr(1));
        }

        // Corps
        if (ctx.instr().size() > 2) {
            visit(ctx.instr(2));
        }

        exitScope();
        return null; // for n'a pas de type de retour
    }

    @Override
    public Type visitReturn(grammarTCLParser.ReturnContext ctx) {
        if (currentFunctionReturnType == null) {
            throw new TypeException("return en dehors d'une fonction", ctx);
        }

        Type returnExprType = visit(ctx.expr());
        updateSubstitutions(returnExprType.unify(currentFunctionReturnType));

        return applyAllSubstitutions(returnExprType);
    }

    @Override
    public Type visitCore_fct(grammarTCLParser.Core_fctContext ctx) {
        enterScope();

        // Visiter toutes les instructions
        for (grammarTCLParser.InstrContext instrCtx : ctx.instr()) {
            visit(instrCtx);
        }

        // Visiter l'expression de retour
        Type returnExprType = visit(ctx.expr());

        // Unifier avec le type de retour de la fonction
        if (currentFunctionReturnType != null) {
            updateSubstitutions(returnExprType.unify(currentFunctionReturnType));
        }

        exitScope();
        return applyAllSubstitutions(returnExprType);
    }

    @Override
    public Type visitDecl_fct(grammarTCLParser.Decl_fctContext ctx) {
        // Type de retour
        Type returnType = visit(ctx.type(0));

        // Nom de la fonction
        String functionName = ctx.VAR(0).getText();

        // Vérifier la redéclaration
        if (lookupVariable(functionName) != null) {
            throw new TypeException("Fonction déjà déclarée: " + functionName, ctx);
        }

        // Types des paramètres
        ArrayList<Type> paramTypes = new ArrayList<>();
        for (int i = 1; i < ctx.type().size(); i++) {
            paramTypes.add(visit(ctx.type(i)));
        }

        // Créer le type de la fonction
        FunctionType functionType = new FunctionType(returnType, paramTypes);
        addVariable(functionName, functionType);

        // Sauvegarder l'ancien type de retour
        Type oldReturnType = currentFunctionReturnType;
        currentFunctionReturnType = returnType;

        // Visiter le corps de la fonction
        visit(ctx.core_fct());

        // Restaurer l'ancien type de retour
        currentFunctionReturnType = oldReturnType;

        return functionType;
    }

    @Override
    public Type visitMain(grammarTCLParser.MainContext ctx) {
        // Visiter toutes les déclarations de fonctions
        for (grammarTCLParser.Decl_fctContext fctCtx : ctx.decl_fct()) {
            visit(fctCtx);
        }

        // Le main retourne un int
        Type oldReturnType = currentFunctionReturnType;
        currentFunctionReturnType = new PrimitiveType(Type.Base.INT);

        // Visiter le corps du main
        if (ctx.core_fct() != null) {
            visit(ctx.core_fct());
        }

        // Restaurer
        currentFunctionReturnType = oldReturnType;

        return null;
    }

    // ==================== Classe d'exception ====================

    class TypeException extends RuntimeException {
        private final int line;
        private final int column;

        public TypeException(String message, ParserRuleContext ctx) {
            super(message + " (ligne " + ctx.getStart().getLine() +
                    ", colonne " + ctx.getStart().getCharPositionInLine() + ")");
            this.line = ctx.getStart().getLine();
            this.column = ctx.getStart().getCharPositionInLine();
        }

        public int getLine() { return line; }
        public int getColumn() { return column; }
    }
}