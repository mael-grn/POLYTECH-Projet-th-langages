import java.lang.ref.PhantomReference;
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
    private void updateSubstitutions(Map<UnknownType, Type> newSubsts) {
        for (Map.Entry<UnknownType, Type> entry : newSubsts.entrySet()) {
            UnknownType v = entry.getKey();
            Type t = entry.getValue();
            this.types.replaceAll((var, existingType) ->existingType.substitute(v,t));
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
    public Type visitCall(grammarTCLParser.CallContext ctx) {
        // Done
        Type fctType = ctx.expr(0).accept(this);
        ArrayList<Type> provideArgs = new ArrayList<>();

        for (int i=1; i<ctx.expr().size(); i++) {
            provideArgs.add(ctx.expr(i).accept(this));
        }
        ArrayList<Type> expArgsVars = new ArrayList<>();
        for (int i=0; i< provideArgs.size(); i++){
            expArgsVars.add(new UnknownType());
        }
        UnknownType returnVar = new UnknownType();
        FunctionType expFctStruct = new FunctionType(returnVar, expArgsVars);
        this.updateSubstitutions(fctType.unify(expFctStruct));

        for (int i=0; i<provideArgs.size();i++){
            Type currentArgExp = applyAll(expArgsVars.get(i));
            this.updateSubstitutions(provideArgs.get(i).unify(currentArgExp));
        }

        return applyAll(returnVar);
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
        String varName = ctx.ID().getText();

        if (!symbolTable.containsKey((varName))) {
            symbolTable.put(varName, new UnknownType());
        }
        Type t = symbolTable.get(varName);

        return applyAll(t);
    }

    @Override
    public Type visitMultiplication(grammarTCLParser.MultiplicationContext ctx) {
        // TODO Auto-generated method stub
        Type leftType = ctx.expr(0).accept(this);
        Type rightType = ctx.expr(1).accept(this);
        Type intType = new PrimitiveType(Type.Base.INT);

        this.updateSubstitutions(leftType.unify(intType));
        this.updateSubstitutions(rightType.unify(intType));

        return intType;
    }

    @Override
    public Type visitEquality(grammarTCLParser.EqualityContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitEquality'");
    }

    @Override
    public Type visitTab_initialization(grammarTCLParser.Tab_initializationContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitTab_initialization'");
    }

    @Override
    public Type visitAddition(grammarTCLParser.AdditionContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitAddition'");
    }

    @Override
    public Type visitBase_type(grammarTCLParser.Base_typeContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitBase_type'");
    }

    @Override
    public Type visitTab_type(grammarTCLParser.Tab_typeContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitTab_type'");
    }

    @Override
    public Type visitDeclaration(grammarTCLParser.DeclarationContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitDeclaration'");
    }

    @Override
    public Type visitPrint(grammarTCLParser.PrintContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitPrint'");
    }

    @Override
    public Type visitAssignment(grammarTCLParser.AssignmentContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitAssignment'");
    }

    @Override
    public Type visitBlock(grammarTCLParser.BlockContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitBlock'");
    }

    @Override
    public Type visitIf(grammarTCLParser.IfContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitIf'");
    }

    @Override
    public Type visitWhile(grammarTCLParser.WhileContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitWhile'");
    }

    @Override
    public Type visitFor(grammarTCLParser.ForContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitFor'");
    }

    @Override
    public Type visitReturn(grammarTCLParser.ReturnContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitReturn'");
    }

    @Override
    public Type visitCore_fct(grammarTCLParser.Core_fctContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitCore_fct'");
    }

    @Override
    public Type visitDecl_fct(grammarTCLParser.Decl_fctContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitDecl_fct'");
    }

    @Override
    public Type visitMain(grammarTCLParser.MainContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitMain'");
    }

    
}
