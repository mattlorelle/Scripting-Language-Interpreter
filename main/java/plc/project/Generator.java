package plc.project;

import java.io.PrintWriter;

public final class Generator implements Ast.Visitor<Void> {

    private final PrintWriter writer;
    private int indent = 0;

    public Generator(PrintWriter writer) {
        this.writer = writer;
    }

    private void print(Object... objects) {
        for (Object object : objects) {
            if (object instanceof Ast) {
                visit((Ast) object);
            } else {
                writer.write(object.toString());
            }
        }
    }

    private void newline(int indent) {
        writer.println();
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
        }
    }

    /*@Override
    public Void visit(Ast.Source ast) {

        create a "class main {"

        declare fields

        declare "public static void main(String[] args) {
                       System.exit(main());
                   }"

        declare each of our methods
        one of our methods is called main!

        return null;
    }*/

    @Override
    public Void visit(Ast.Source ast) {
        print("public class Main {");
        newline(0);
        newline(1);

        if (!(ast.getFields().isEmpty())) {
            for (int i = 0; i < ast.getFields().size(); i++) {
                print(ast.getFields().get(i));
                newline(indent);
            }
            newline(indent);
        }

        print("public static void main(String[] args) {");
        newline(2);
        print("System.exit(new Main().main());");
        newline(1);
        print("}");
        newline(0);

        newline(1);
        print("int main() {");
        newline(2);
        print("System.out.println(\"Hello, World!\");");
        newline(2);
        print("return 0;");
        newline(1);
        print("}");

        newline(0);
        newline(0);
        print("}");

        return null;
    }

    @Override
    public Void visit(Ast.Field ast) {
        print(ast.getTypeName(), " ", ast.getVariable().getName());
        if (ast.getValue().isPresent()) {
            print(" = ", ast.getVariable().getValue());
        }
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Method ast) {

        print(ast.getFunction().getJvmName(), " ", ast.getFunction().getName(), "(");
        int size = ast.getParameters().size();
        for (int i = 0; i < size - 1; i++) {
            print(ast.getParameters().get(i), ", ");
        }
        print(ast.getParameters().get(size - 1));

        if (ast.getStatements().size() == 0) {
            print(" {}");
        }
        else {
            newline(++indent);
            for (int i = 0; i < ast.getStatements().size(); i++) {
                if (i != 0) {
                    newline(indent);
                }
                print(ast.getStatements().get(i));
            }
            newline(--indent);
            print("}");
        }

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Expression ast) {
        print(ast.getExpression(), ";");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Declaration ast) {
        print(ast.getVariable().getType().getJvmName(),
                " ",
                ast.getVariable().getJvmName());
        if (ast.getValue().isPresent()) {
            print(" = ", ast.getValue().get());
        }
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Assignment ast) {
        print(ast.getReceiver(), " = \"", ast.getValue(), "\";");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.If ast) {
        print("if (", ast.getCondition(), ") {");

        if (!(ast.getThenStatements().isEmpty()) && (ast.getElseStatements().isEmpty())) {
            newline(++indent);
            for (int i = 0; i < ast.getThenStatements().size(); i++) {
                if (i != 0) {
                    newline(indent);
                }
                print(ast.getThenStatements().get(i));
            }
        }

        if (!(ast.getThenStatements().isEmpty()) && !(ast.getElseStatements().isEmpty())) {
            newline(++indent);
            for (int i = 0; i < ast.getThenStatements().size(); i++) {
                if (i != 0) {
                    newline(indent);
                }
                print(ast.getThenStatements().get(i));
            }
            newline(--indent);
            print("} else {");
            newline(++indent);
            for (int i = 0; i < ast.getElseStatements().size(); i++) {
                if (i != 0) {
                    newline(indent);
                }
                print(ast.getElseStatements().get(i));
            }
        }

        newline(0);
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.For ast) {
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.While ast) {
        print("while (", ast.getCondition(), ") {");
        if (!ast.getStatements().isEmpty()) {
            newline(++indent);
            for (int i = 0; i < ast.getStatements().size(); i++) {
                if (i != 0) {
                    newline(indent);
                }
                print(ast.getStatements().get(i));
            }
            newline(--indent);
        }
        print("}");

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Return ast) {
        print ("return ", ast.getValue(),";");
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Literal ast) {

        if (ast.getType() == Environment.Type.STRING)
            print ("\"", ast.getLiteral(), "\"");
        else if (ast.getType() == Environment.Type.CHARACTER)
            print ("\'", ast.getLiteral(), "\'");
        else
            print(ast.getLiteral());

        return null;
    }

    @Override
    public Void visit(Ast.Expr.Group ast) {
        print("(", ast.getExpression(), ")");
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Binary ast) {
        if (ast.getOperator() == "AND") {
            print (ast.getLeft(), " && ", ast.getRight());
        }
        else if (ast.getOperator() == "OR") {
            print (ast.getLeft(), "||", ast.getRight());
        }
        else
            print (ast.getLeft(), " ", ast.getOperator(), " ", ast.getRight());
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Access ast) {
        if (!ast.getReceiver().isPresent()) {
            print(ast.getVariable().getJvmName());
        }
        else {
            print(ast.getReceiver().get(), ".", ast.getVariable().getJvmName());
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Function ast) {

        if (ast.getReceiver().isPresent()) {
            print(ast.getReceiver().get(), ".");
        }
        print(ast.getFunction().getJvmName(), "(");
        int size = ast.getArguments().size();
        for (int i = 0; i < size - 1; i++) {
            print(ast.getArguments().get(i), ", ");
        }
        print(ast.getArguments().get(size - 1), ")");

        return null;
    }

}
