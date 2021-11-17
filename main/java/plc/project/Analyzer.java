package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * See the specification for information about what the different visit
 * methods should do.
 */
public final class Analyzer implements Ast.Visitor<Void> {

    public Scope scope;
    private Ast.Method method ;

    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Void visit(Ast.Source ast) {

        try {
            scope = new Scope(scope);
            for (Ast.Field field : ast.getFields()) {
                visit(field);
            }
        }
        finally {
            scope = scope.getParent();
        }
        try {
            scope = new Scope(scope);
            for (Ast.Method method : ast.getMethods()) {
                visit(method);
            }
        }
        finally {
            scope = scope.getParent();
        }

        scope.lookupFunction("main", 0);
        requireAssignable(Environment.Type.INTEGER, scope.lookupFunction("main", 0).getReturnType());

        return null;
    } //FINISHED

    @Override
    public Void visit(Ast.Field ast) {

        if (ast.getValue().isPresent()) {
            visit(ast.getValue().get());
            requireAssignable(ast.getVariable().getValue().getField(ast.getName()).getType(), ast.getValue().get().getType());
        }

        ast.setVariable(scope.defineVariable(ast.getName(), ast.getName(), ast.getVariable().getType(), Environment.NIL));

        return null;
    } //FINISHED

    @Override
    public Void visit(Ast.Method ast) {

        List<Environment.Type> parameterTypes = new ArrayList<>();
        for (int i = 0; i < ast.getParameterTypeNames().size(); i++) {
            parameterTypes.add(Environment.getType(ast.getParameterTypeNames().get(i)));
        }
        Environment.Type returnType = Environment.Type.NIL;
        if (ast.getReturnTypeName().isPresent())
            returnType = Environment.getType(ast.getReturnTypeName().get());

        scope.defineFunction(ast.getName(), ast.getName(), parameterTypes, returnType, args -> Environment.NIL);

        for (int i = 0; i < ast.getStatements().size(); i++) {
            visit(ast.getStatements().get(i));
        }

        ast.setFunction(scope.lookupFunction(ast.getName(), ast.getParameters().size()));

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Expression ast) {
        visit(ast.getExpression());
        if (!(ast.getExpression() instanceof Ast.Expr.Function)) {
            throw new RuntimeException("The expression is not an Ast.Expr.Function");
        }
        return null;
    } //FINISHED

    @Override
    public Void visit(Ast.Stmt.Declaration ast) {

        Environment.Type type;
        if (ast.getTypeName().isPresent()) {
            type = Environment.getType(ast.getTypeName().get());
        } else if (ast.getValue().isPresent()) {
            visit(ast.getValue().get());
            type = ast.getValue().get().getType();
        } else {
            throw new RuntimeException("Both value and typename not present");
        }

        scope.defineVariable(ast.getName(),ast.getName(), type, Environment.NIL);
        ast.setVariable(scope.lookupVariable(ast.getName()));

        return null;
    } //FINISHED

    @Override
    public Void visit(Ast.Stmt.Assignment ast) {

        visit(ast.getReceiver());
        visit(ast.getValue());
        if (!(ast.getReceiver() instanceof Ast.Expr.Access)) {
            throw new RuntimeException("The expression is not an Ast.Expr.Function");
        }
        requireAssignable(ast.getReceiver().getType(), ast.getValue().getType());

        return null;
    } //FINISHED

    @Override
    public Void visit(Ast.Stmt.If ast) {

        visit(ast.getCondition());
        requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());
        if (ast.getThenStatements().isEmpty()) {
            throw new RuntimeException("The thenStatements list is empty");
        }


        try {
            scope = new Scope(scope);
            for (Ast.Stmt stmt : ast.getThenStatements()) {
                visit(stmt);
            }
        }
        finally {
            scope = scope.getParent();
        }

        try {
            scope = new Scope(scope);
            for (Ast.Stmt stmt : ast.getElseStatements()) {
                visit(stmt);
            }
        }
        finally {
            scope = scope.getParent();
        }

        return null;
    } //FINISHED

    @Override
    public Void visit(Ast.Stmt.For ast) {

        visit(ast.getValue());
        requireAssignable(Environment.Type.INTEGER_ITERABLE, ast.getValue().getType());

        if (ast.getStatements().isEmpty()) {
            throw new RuntimeException("The statements list is empty");
        }

        try {
            scope = new Scope(scope);
            for (Ast.Stmt stmt : ast.getStatements()) {
                visit(stmt);
            }
        }
        finally {
            scope.defineVariable(ast.getName(), ast.getName(), Environment.Type.INTEGER, Environment.NIL);
            scope = scope.getParent();
        }

        return null;
    } //FINISHED

    @Override
    public Void visit(Ast.Stmt.While ast) {

        visit(ast.getCondition());
        requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());
        try {
            scope = new Scope(scope);
            for (Ast.Stmt stmt : ast.getStatements()) {
                visit(stmt);
            }
        }
        finally {
            scope = scope.getParent();
        }
        return null;
    } //FINISHED

    @Override
    public Void visit(Ast.Stmt.Return ast) {

        visit(ast.getValue());
        requireAssignable(method.getFunction().getReturnType(), ast.getValue().getType());
        return null;
    } //FINISHED

    @Override
    public Void visit(Ast.Expr.Literal ast) {


        if (Objects.isNull(ast.getLiteral()))
            ast.setType(Environment.Type.NIL);
        if (ast.getLiteral() instanceof Boolean)
            ast.setType(Environment.Type.BOOLEAN);
        if (ast.getLiteral() instanceof Character)
            ast.setType(Environment.Type.CHARACTER);
        if (ast.getLiteral() instanceof String)
            ast.setType(Environment.Type.STRING);

        if (ast.getLiteral() instanceof BigInteger) {

            BigInteger bigInt = (BigInteger) (ast.getLiteral());
            if ((bigInt.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) == 1) || bigInt.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) == -1)
                throw new RuntimeException("Outside min/max boundaries");
            else
                ast.setType(Environment.Type.INTEGER);
        }

        if (ast.getLiteral() instanceof BigDecimal) {

            BigDecimal bigDec = (BigDecimal) (ast.getLiteral());
            if ((bigDec.compareTo(BigDecimal.valueOf(Double.MAX_VALUE)) == 1) && bigDec.compareTo(BigDecimal.valueOf(Double.MIN_VALUE)) == -1)
                throw new RuntimeException("Outside min/max boundaries");
            else
                ast.setType(Environment.Type.DECIMAL);
        }

        return null;
    } //FINISHED

    @Override
    public Void visit(Ast.Expr.Group ast) {

        visit(ast.getExpression());

        if (!(ast.getExpression() instanceof Ast.Expr.Binary))
            throw new RuntimeException("Expression not of type binary");

        return null;
    } //FINISHED

    @Override
    public Void visit(Ast.Expr.Binary ast) {

        visit(ast.getLeft());
        visit(ast.getRight());

        switch (ast.getOperator()) {

            case "AND": //check if both left and right operands are boolean and if so, set ast type to boolean

                requireAssignable(Environment.Type.BOOLEAN, ast.getLeft().getType());
                requireAssignable(Environment.Type.BOOLEAN, ast.getRight().getType());

                ast.setType(Environment.Type.BOOLEAN);

            case "OR":

                requireAssignable(Environment.Type.BOOLEAN, ast.getLeft().getType());
                requireAssignable(Environment.Type.BOOLEAN, ast.getRight().getType());

                ast.setType(Environment.Type.BOOLEAN);

            case "<": //check if left hand operand is a Comparable

                requireAssignable(Environment.Type.COMPARABLE, ast.getLeft().getType());
                requireAssignable(Environment.Type.COMPARABLE, ast.getRight().getType());
                requireAssignable(ast.getLeft().getType(), ast.getRight().getType());

                ast.setType(Environment.Type.BOOLEAN);

            case "<=":

                requireAssignable(Environment.Type.COMPARABLE, ast.getLeft().getType());
                requireAssignable(Environment.Type.COMPARABLE, ast.getRight().getType());
                requireAssignable(ast.getLeft().getType(), ast.getRight().getType());

                ast.setType(Environment.Type.BOOLEAN);

            case ">":

                requireAssignable(Environment.Type.COMPARABLE, ast.getLeft().getType());
                requireAssignable(Environment.Type.COMPARABLE, ast.getRight().getType());
                requireAssignable(ast.getLeft().getType(), ast.getRight().getType());

                ast.setType(Environment.Type.BOOLEAN);

            case ">=":

                requireAssignable(Environment.Type.COMPARABLE, ast.getLeft().getType());
                requireAssignable(Environment.Type.COMPARABLE, ast.getRight().getType());
                requireAssignable(ast.getLeft().getType(), ast.getRight().getType());

                ast.setType(Environment.Type.BOOLEAN);

            case "==":

                requireAssignable(Environment.Type.COMPARABLE, ast.getLeft().getType());
                requireAssignable(Environment.Type.COMPARABLE, ast.getRight().getType());
                requireAssignable(ast.getLeft().getType(), ast.getRight().getType());

                ast.setType(Environment.Type.BOOLEAN);

            case "!=":

                requireAssignable(Environment.Type.COMPARABLE, ast.getLeft().getType());
                requireAssignable(Environment.Type.COMPARABLE, ast.getRight().getType());
                requireAssignable(ast.getLeft().getType(), ast.getRight().getType());

                ast.setType(Environment.Type.BOOLEAN);

            case "+":

                if (ast.getLeft().getType().getName().equals(Environment.Type.STRING.getName()) || ast.getRight().getType().getName().equals(Environment.Type.STRING.getName()))
                    ast.setType(Environment.Type.STRING);

                try {
                    requireAssignable(Environment.Type.INTEGER, ast.getLeft().getType());
                    requireAssignable(Environment.Type.INTEGER, ast.getRight().getType());
                    ast.setType(Environment.Type.INTEGER);
                } catch (RuntimeException e) { }

                try {
                    requireAssignable(Environment.Type.DECIMAL, ast.getLeft().getType());
                    requireAssignable(Environment.Type.DECIMAL, ast.getRight().getType());
                    ast.setType(Environment.Type.DECIMAL);
                } catch (RuntimeException e) { }

            case "-":

                if (ast.getLeft().getType().getName().equals(Environment.Type.INTEGER.getName())) {
                    requireAssignable(Environment.Type.INTEGER, ast.getRight().getType());
                    ast.setType(Environment.Type.INTEGER);
                }
                else if (ast.getLeft().getType().getName().equals(Environment.Type.DECIMAL.getName())) {
                    requireAssignable(Environment.Type.DECIMAL, ast.getRight().getType());
                    ast.setType(Environment.Type.DECIMAL);
                }

            case "*":

                if (ast.getLeft().getType().getName().equals(Environment.Type.INTEGER.getName())) {
                    requireAssignable(Environment.Type.INTEGER, ast.getRight().getType());
                    ast.setType(Environment.Type.INTEGER);
                }
                else if (ast.getLeft().getType().getName().equals(Environment.Type.DECIMAL.getName())) {
                    requireAssignable(Environment.Type.DECIMAL, ast.getRight().getType());
                    ast.setType(Environment.Type.DECIMAL);
                }

            case "/":

                if (ast.getLeft().getType().getName().equals(Environment.Type.INTEGER.getName())) {
                    requireAssignable(Environment.Type.INTEGER, ast.getRight().getType());
                    ast.setType(Environment.Type.INTEGER);
                }
                else if (ast.getLeft().getType().getName().equals(Environment.Type.DECIMAL.getName())) {
                    requireAssignable(Environment.Type.DECIMAL, ast.getRight().getType());
                    ast.setType(Environment.Type.DECIMAL);
                }
        }
        return null;
    } //FINISHED

    @Override
    public Void visit(Ast.Expr.Access ast) {

        if (ast.getReceiver().isPresent()) {
            visit(ast.getReceiver().get());
            ast.setVariable(ast.getReceiver().get().getType().getField(ast.getName()));
        }
        else {
            ast.setVariable(getScope().lookupVariable(ast.getName()));
        }

        return null;
    } //FINISHED

    @Override
    public Void visit(Ast.Expr.Function ast) {

        for (int i = 0; i < ast.getArguments().size(); i++) {
            requireAssignable(ast.getFunction().getParameterTypes().get(i), ast.getArguments().get(i).getType());
        }

        if (ast.getReceiver().isPresent()) {
            visit(ast.getReceiver().get());
            ast.setFunction(ast.getReceiver().get().getType().getMethod(ast.getName(), ast.getArguments().size()));
        }
        else {
            ast.setFunction(getScope().lookupFunction(ast.getName(), ast.getArguments().size()));
        }

        for (int i = 0; i < ast.getArguments().size(); i++) {
            visit(ast.getArguments().get(i));
        }

        return null;
    } //FINISHED

    public static void requireAssignable(Environment.Type target, Environment.Type type) {

        //if statement with all the situations that would cause an error to be thrown
        if (target.getName().equals(Environment.Type.COMPARABLE.getName())) {
            if (type.getName().equals(Environment.Type.ANY.getName()) ||
                    type.getName().equals(Environment.Type.NIL.getName()) ||
                    type.getName().equals(Environment.Type.INTEGER_ITERABLE.getName()))
            {
                throw new RuntimeException("Comparable assignment not of correct type");
            }
        }

        //if the target is not any (meaning it could fail) check if they're not the same
        else if (!target.getName().equals(Environment.Type.ANY.getName())) {
            if (!target.getName().equals(type.getName()))
                throw new RuntimeException("Assignment not of same type");
        }
    } //FINISHED
}
