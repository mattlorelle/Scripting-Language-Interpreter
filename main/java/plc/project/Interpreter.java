package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;


public class Interpreter implements Ast.Visitor<Environment.PlcObject> {

    private Scope scope = new Scope(null);

    public Interpreter(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Environment.PlcObject visit(Ast.Source ast) {

        List<Environment.PlcObject> list = new ArrayList<>();

        for (int i = 0; i < ast.getFields().size(); i++) {
            list.add(visit(ast.getFields().get(i)));
        }
        for (int i = 0; i < ast.getMethods().size(); i++) {
            list.add(visit(ast.getMethods().get(i)));
        }

        try {
            return scope.lookupFunction("main", 0).invoke(list);
        } catch (RuntimeException e) {
            throw e;
        }
    }

    @Override
    public Environment.PlcObject visit(Ast.Field ast) {

        if (ast.getValue().isPresent()) {
            scope.defineVariable(ast.getName(), visit(ast.getValue().get()));
        } else {
            scope.defineVariable(ast.getName(), Environment.NIL);
        }
        return Environment.NIL;
    } //FINISHED

    @Override
    public Environment.PlcObject visit(Ast.Method ast) {

        Scope parent = scope.getParent();

        scope.defineFunction(ast.getName(), ast.getParameters().size(), args -> {

            try {
                for (int i = 0; i < ast.getParameters().size(); i++) {
                    scope.defineVariable(ast.getParameters().get(i), args.get(i));
                }
                for (int j = 0; j < ast.getStatements().size(); j++) {
                    visit(ast.getStatements().get(j));
                }
            }
            catch (Return r) {

                return r.value;

            } finally {
                scope = scope.getParent();
            }
            return Environment.NIL;
        });

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Expression ast) {

        visit(ast.getExpression());
        return Environment.NIL;
    } //FINISHED

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Declaration ast) {

        if (ast.getValue().isPresent()) { //if the optional value is present
            scope.defineVariable(ast.getName(), visit(ast.getValue().get()));
        } else {
            scope.defineVariable(ast.getName(), Environment.NIL);
        }

        return Environment.NIL;
    } //FINISHED

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Assignment ast) {

        if (ast.getReceiver() instanceof Ast.Expr.Access) {

            Ast.Expr.Access access = (Ast.Expr.Access)ast.getReceiver();

            if (access.getReceiver().isPresent())
                visit(access.getReceiver().get()).setField(access.getName(), visit(ast.getValue()));
            else
                getScope().lookupVariable(access.getName()).setValue(visit(ast.getValue()));
        }
        else
            throw new RuntimeException();

        return Environment.NIL;
    } //FINISHED


    @Override
    public Environment.PlcObject visit(Ast.Stmt.If ast) {

        if (requireType(Boolean.class, visit (ast.getCondition()))) {
            try {
                scope = new Scope(scope);
                for (Ast.Stmt stmt : ast.getThenStatements()) {
                    visit(stmt);
                }
            } finally {
                scope = scope.getParent();
            }
        } else {
            try {
                scope = new Scope(scope);
                for(Ast.Stmt stmt : ast.getElseStatements()) {
                    visit(stmt);
                }
            } finally {
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
    } //FINISHED

    @Override
    public Environment.PlcObject visit(Ast.Stmt.For ast) {

        Iterable<Environment.PlcObject> values = requireType(Iterable.class, visit(ast.getValue()));

        for (Environment.PlcObject p : values) {
            try {
                scope = new Scope(scope);
                getScope().defineVariable(ast.getName(), p);
                for (Ast.Stmt stmt : ast.getStatements()) {
                    visit(stmt);
                }
            } finally {
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
    } //FINISHED

    @Override
    public Environment.PlcObject visit(Ast.Stmt.While ast) {

        while (requireType(Boolean.class, visit (ast.getCondition()))) {
            try {
                scope = new Scope(scope);
                for (Ast.Stmt stmt : ast.getStatements()) {
                    visit(stmt);
                }
            } finally {
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
    } //FINISHED

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Return ast) {

        throw new Return(visit(ast.getValue()));
    } //FINISHED

    @Override
    public Environment.PlcObject visit(Ast.Expr.Literal ast) {

        if (ast.getLiteral() == null)
            return Environment.NIL;
        else
            return Environment.create(ast.getLiteral());
    } //FINISHED

    @Override
    public Environment.PlcObject visit(Ast.Expr.Group ast) {

        return Environment.create(visit(ast.getExpression()).getValue());
    } //FINISHED

    @Override
    public Environment.PlcObject visit(Ast.Expr.Binary ast) {

        String operator = ast.getOperator();

        switch (operator) {
            case "AND": //check if left hand operand is a bool, else throw an exception

                requireType(Boolean.class, visit(ast.getLeft()));
                requireType(Boolean.class, visit(ast.getRight()));

                if (visit (ast.getLeft()).getValue().equals(true)) {
                    if (visit(ast.getRight()).getValue().equals(true))
                        return Environment.create(true);
                    else
                        return Environment.create(false);
                } else
                    return Environment.create(false);

            case "OR":

                requireType(Boolean.class, visit(ast.getLeft()));

                if (visit(ast.getLeft()).getValue().equals(true))
                    return Environment.create(true);

                requireType(Boolean.class, visit(ast.getRight()));

                if (visit(ast.getRight()).getValue().equals(true))
                    return Environment.create(true);
                else
                    return Environment.create(false);

            case "<": //check if left hand operand is a Comparable
                requireType(Comparable.class, visit(ast.getLeft()));
                requireType(Comparable.class, visit(ast.getRight()));

                if(((Comparable)visit(ast.getLeft()).getValue()).compareTo((Comparable)visit(ast.getRight()).getValue()) < 0)
                    return Environment.create(true);
                else
                    return Environment.create(false);

            case "<=":
                requireType(Comparable.class, visit(ast.getLeft()));
                requireType(Comparable.class, visit(ast.getRight()));

                if(((Comparable)visit(ast.getLeft()).getValue()).compareTo((Comparable)visit(ast.getRight()).getValue()) <= 0)
                    return Environment.create(true);
                else
                    return Environment.create(false);

            case ">":
                requireType(Comparable.class, visit(ast.getLeft()));
                requireType(Comparable.class, visit(ast.getRight()));

                if(((Comparable)visit(ast.getLeft()).getValue()).compareTo((Comparable)visit(ast.getRight()).getValue()) > 0)
                    return Environment.create(true);
                else
                    return Environment.create(false);

            case ">=":
                requireType(Comparable.class, visit(ast.getLeft()));
                requireType(Comparable.class, visit(ast.getRight()));

                if(((Comparable)visit(ast.getLeft()).getValue()).compareTo((Comparable)visit(ast.getRight()).getValue()) >= 0)
                    return Environment.create(true);
                else
                    return Environment.create(false);

            case "==":
                if (visit(ast.getLeft()).getValue().equals(visit(ast.getRight()).getValue()))
                    return Environment.create(true);
                else
                    return Environment.create(false);

            case "!=":
                if (!visit(ast.getLeft()).getValue().equals(visit(ast.getRight()).getValue()))
                    return Environment.create(true);
                else
                    return Environment.create(false);

            case "+":
                if (visit(ast.getLeft()).getValue() instanceof String)
                    return Environment.create((String)visit(ast.getLeft()).getValue() + visit(ast.getRight()).getValue());
                if (visit(ast.getRight()).getValue() instanceof String)
                    return Environment.create((String)visit(ast.getRight()).getValue() + visit(ast.getLeft()).getValue());

                if (visit(ast.getLeft()).getValue() instanceof BigInteger) {
                    requireType(BigInteger.class, visit(ast.getRight()));
                    return Environment.create(((BigInteger) visit(ast.getLeft()).getValue()).add((BigInteger) visit(ast.getRight()).getValue()));
                }
                else if (visit(ast.getLeft()).getValue() instanceof BigDecimal) {
                    requireType(BigDecimal.class, visit(ast.getRight()));
                    return Environment.create(((BigDecimal) visit(ast.getLeft()).getValue()).add((BigDecimal) visit(ast.getRight()).getValue()));
                }

            case "-":
                if (visit(ast.getLeft()).getValue() instanceof BigInteger) {
                    requireType(BigInteger.class, visit(ast.getRight()));
                    return Environment.create(((BigInteger) visit(ast.getLeft()).getValue()).subtract((BigInteger) visit(ast.getRight()).getValue()));
                }
                else if (visit(ast.getLeft()).getValue() instanceof BigDecimal) {
                    requireType(BigDecimal.class, visit(ast.getRight()));
                    return Environment.create(((BigDecimal) visit(ast.getLeft()).getValue()).subtract((BigDecimal) visit(ast.getRight()).getValue()));
                }

            case "*":
                if (visit(ast.getLeft()).getValue() instanceof BigInteger) {
                    requireType(BigInteger.class, visit(ast.getRight()));
                    return Environment.create(((BigInteger) visit(ast.getLeft()).getValue()).multiply((BigInteger) visit(ast.getRight()).getValue()));
                }
                else if (visit(ast.getLeft()).getValue() instanceof BigDecimal) {
                    requireType(BigDecimal.class, visit(ast.getRight()));
                    return Environment.create(((BigDecimal) visit(ast.getLeft()).getValue()).multiply((BigDecimal) visit(ast.getRight()).getValue()));
                }

            case "/":
                if (visit(ast.getLeft()).getValue() instanceof BigInteger) {
                    requireType(BigInteger.class, visit(ast.getRight()));
                    return Environment.create(((BigInteger) visit(ast.getLeft()).getValue()).divide((BigInteger) visit(ast.getRight()).getValue()));
                }
                else if (visit(ast.getLeft()).getValue() instanceof BigDecimal) {
                    requireType(BigDecimal.class, visit(ast.getRight()));
                    return Environment.create(((BigDecimal) visit(ast.getLeft()).getValue()).divide((BigDecimal) visit(ast.getRight()).getValue(), BigDecimal.ROUND_HALF_EVEN));
                }
        }
        return Environment.NIL;
    } //FINISHED

    @Override
    public Environment.PlcObject visit(Ast.Expr.Access ast) {

        if (ast.getReceiver().isPresent()) {
            return visit(ast.getReceiver().get()).getField(ast.getName()).getValue();
        }
        else {
            return getScope().lookupVariable(ast.getName()).getValue();
        }
    } //FINISHED

    @Override
    public Environment.PlcObject visit(Ast.Expr.Function ast) {

        List<Environment.PlcObject> plc = new ArrayList<>();
        for (int i = 0; i < ast.getArguments().size(); i++) {
            plc.add(visit(ast.getArguments().get(i)));
        }

        if (ast.getReceiver().isPresent()) {
            return visit(ast.getReceiver().get()).callMethod(ast.getName(), plc);
        }
        else {
            return getScope().lookupFunction(ast.getName(), ast.getArguments().size()).invoke(plc);
        }
    } //FINISHED

    /**
     * Helper function to ensure an object is of the appropriate type.
     */
    private static <T> T requireType(Class<T> type, Environment.PlcObject object) {
        if (type.isInstance(object.getValue())) {
            return type.cast(object.getValue());
        } else {
            throw new RuntimeException("Expected type " + type.getName() + ", received " + object.getValue().getClass().getName() + ".");
        }
    }

    /**
     * Exception class for returning values.
     */
    private static class Return extends RuntimeException {

        private final Environment.PlcObject value;

        private Return(Environment.PlcObject value) {
            this.value = value;
        }

    }

}
