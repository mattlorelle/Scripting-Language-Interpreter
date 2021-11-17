package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 *
 * The parser has a similar architecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link #peek(Object...)} and {@link
 * #match(Object...)} are helpers to make the implementation easier.
 *
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have it's own function, and reference to other rules correspond
 * to calling that functions.
 */
public final class Parser {

    private final TokenStream tokens;
    int current = 0;
    public Parser(List<Token> tokens) {

        this.tokens = new TokenStream(tokens);
    }

    /**
     * Parses the {@code source} rule.
     */
    public Ast.Source parseSource() throws ParseException {

        List<Ast.Field> fields = new ArrayList<>();
        List<Ast.Method> methods = new ArrayList<>();

        while(match("LET")) {
            fields.add(parseField());
        }
        while(match("DEF")) {
            methods.add(parseMethod());
        }
        return new Ast.Source(fields, methods);
    }

    /**
     * Parses the {@code field} rule. This method should only be called if the
     * next tokens start a field, aka {@code LET}.
     */
    public Ast.Field parseField() throws ParseException {

        Optional<Ast.Expr> receiver = Optional.empty();
        String name = "";
        String typename = "";

        if(match(Token.Type.IDENTIFIER)) {
            name = tokens.get(-1).getLiteral();

            match(":");
            match(Token.Type.IDENTIFIER);

            typename = tokens.get(-1).getLiteral();

            if (match("=")) {
                Ast.Expr expr = parseExpression();
                receiver = Optional.of(expr);
            }
            if(!peek(";")) {
                throw new ParseException("Expected ;", tokens.index);
            }
        }
        return new Ast.Field(name, typename, receiver);
    }

    /**
     * Parses the {@code method} rule. This method should only be called if the
     * next tokens start a method, aka {@code DEF}.
     */
    public Ast.Method parseMethod() throws ParseException {

        String name = "";
        List<String> parameters = new ArrayList<>();
        List<String> parameterTypeNames = new ArrayList<>();
        Optional<String> returnTypeName = Optional.empty();
        List<Ast.Stmt> statements = new ArrayList<>();

        if(match(Token.Type.IDENTIFIER)) {
            name = tokens.get(-1).getLiteral();
            if(match("(")) {
                if (match(Token.Type.IDENTIFIER)) {
                    parameters.add(tokens.get(-1).getLiteral());
                    match(":");
                    match(Token.Type.IDENTIFIER);
                    parameterTypeNames.add(tokens.get(-1).getLiteral());
                    while (match(",")) {
                        parameters.add(tokens.get(0).getLiteral());
                        match(":");
                        match(Token.Type.IDENTIFIER);
                        parameterTypeNames.add(tokens.get(-1).getLiteral());
                        tokens.advance();
                    }
                }
                if (match(")")) {

                    if (match(":")) {
                        match(Token.Type.IDENTIFIER);
                        returnTypeName = Optional.of(tokens.get(-1).getLiteral());
                    }

                    if(match("DO")) {
                        while(tokens.has(1))  {
                            statements.add(parseStatement());
                            tokens.advance();
                        }
                        if(!peek("END")) {
                            if (tokens.has(0))
                                throw new ParseException("Expected \"END\"", tokens.get(0).getIndex());
                            else
                                throw new ParseException("Expected \"END\"", tokens.get(-1).getIndex());
                        }
                    } else {
                        if (tokens.has(0))
                            throw new ParseException("Expected \"DO\"", tokens.get(0).getIndex());
                        else
                            throw new ParseException("Expected \"DO\"", tokens.get(-1).getIndex());

                    }
                } else
                if (tokens.has(0))
                    throw new ParseException("Expected )", tokens.get(0).getIndex());
                else
                    throw new ParseException("Expected )", tokens.get(-1).getIndex());
            } else
            if (tokens.has(0))
                throw new ParseException("Expected (", tokens.get(0).getIndex());
            else
                throw new ParseException("Expected (", tokens.get(-1).getIndex());
        }
        else {
            if (tokens.has(0))
                throw new ParseException("Expected an Identifier", tokens.get(0).getIndex());
            else
                throw new ParseException("Expected an Identifier", tokens.get(-1).getIndex());
        }
        return new Ast.Method(name,parameters, parameterTypeNames, returnTypeName, statements);
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Stmt parseStatement() throws ParseException {

        if(match("LET")) {
            return parseDeclarationStatement();
        }
        else if(match("IF")) {
            return parseIfStatement();
        }
        else if(match("FOR")) {
            return parseForStatement();
        }
        else if(match("WHILE")) {
            return parseWhileStatement();
        }
        else if(match("RETURN")) {
            return parseReturnStatement();
        }
        else {

            Ast.Expr expr = parseExpression();

            if(match("=")) {

                Ast.Expr value = parseExpression();

                if (value != null) {
                    if(peek(";"))
                        return new Ast.Stmt.Assignment(expr, value);
                    else
                        throw new ParseException("Expected ;", tokens.index);
                } else
                    throw new ParseException("Value missing", tokens.index);

            }

            return new Ast.Stmt.Expression(expr);
        }
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Stmt.Declaration parseDeclarationStatement() throws ParseException {

        String name = "";
        Optional<String> typeName = Optional.empty();
        Optional<Ast.Expr> value = Optional.empty();

        if (match(Token.Type.IDENTIFIER)) {
            name = tokens.get(-1).getLiteral();
            if (match(":")) {
                match(Token.Type.IDENTIFIER);
                typeName = Optional.of(tokens.get(-1).getLiteral());
            }
            if (match("=")) {
                value = Optional.of(parseExpression());
            }
        }
        else {
            throw new ParseException("Expected an Identifier", tokens.index);
        }
        if (!match(";")) {
            throw new ParseException("Expected ;", tokens.index);
        }
        return new Ast.Stmt.Declaration(name, typeName, value);
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Stmt.If parseIfStatement() throws ParseException {

        List<Ast.Stmt> thenStatements = new ArrayList<>();
        List<Ast.Stmt> elseStatements = new ArrayList<>();

        Ast.Expr condition = parseExpression();

        if (match("DO")) {
            while(!peek("END")) {
                if(peek("ELSE"))
                    break;
                thenStatements.add(parseStatement());
                tokens.advance();
            }
            if(match("ELSE")) {
                while (!peek("END")) {
                    elseStatements.add(parseStatement());
                    tokens.advance();
                }
            }
        } else {
            throw new ParseException("Expected \"DO\"", tokens.index);
        }
        return new Ast.Stmt.If(condition, thenStatements, elseStatements);
    }

    /**
     * Parses a for statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a for statement, aka
     * {@code FOR}.
     */
    public Ast.Stmt.For parseForStatement() throws ParseException {

        String name = "";
        Ast.Expr value;
        List<Ast.Stmt> statements = new ArrayList<>();

        if (match(Token.Type.IDENTIFIER)) {
            name = tokens.get(-1).getLiteral();
            if (match("IN")) {
                value = parseExpression();
                if (match("DO")) {
                    while(tokens.has(1))  {
                        statements.add(parseStatement());
                        tokens.advance();
                    }
                    if(!peek("END")) {
                        throw new ParseException("Expected \"END\"", tokens.index);
                    }
                }
                else {
                    throw new ParseException("Expected \"DO\"", tokens.index);
                }
            }
            else {
                throw new ParseException("Expected \"IN\"", tokens.index);
            }
        }
        else {
            throw new ParseException("Expected an Identifier", tokens.index);
        }
        return new Ast.Stmt.For(name, value, statements);
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Stmt.While parseWhileStatement() throws ParseException {

        Ast.Expr condition;
        List<Ast.Stmt> statements = new ArrayList<>();

        condition = parseExpression();

        if (match("DO")) {
            while(tokens.has(1)) {
                statements.add(parseStatement());
                tokens.advance();
            }
            if (!peek("END"))
                throw new ParseException("Expected \"END\"", tokens.index);
        } else
            throw new ParseException("Expected \"DO\"", tokens.index);

        return new Ast.Stmt.While(condition,statements);
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Stmt.Return parseReturnStatement() throws ParseException {

        Ast.Expr value = parseExpression();
        if(!peek(";"))
            throw new ParseException("Expected ;", tokens.index);
        else {
            if (value != null)
                return new Ast.Stmt.Return(value);
            else
                throw new ParseException("Value missing", tokens.index);
        }
    }

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expr parseExpression() throws ParseException {

        return parseLogicalExpression();
    }

    /**
     * Parses the {@code logical-expression} rule.
     */
    public Ast.Expr parseLogicalExpression() throws ParseException {

        Ast.Expr expr = parseEqualityExpression();

        while(match("AND") | match("OR")) {
            Token operator = tokens.get(-1);
            Ast.Expr right = parseEqualityExpression();
            expr = new Ast.Expr.Binary(operator.getLiteral(), expr, right);
        }

        return expr;
    }

    /**
     * Parses the {@code equality-expression} rule.
     */
    public Ast.Expr parseEqualityExpression() throws ParseException {

        Ast.Expr expr = parseAdditiveExpression();

        while(match("<") | match("<=") | match(">") | match(">=") | match("==") | match("!=")) {
            Token operator = tokens.get(-1);
            Ast.Expr right = parseAdditiveExpression();
            expr = new Ast.Expr.Binary(operator.getLiteral(), expr, right);
        }

        return expr;
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expr parseAdditiveExpression() throws ParseException {

        Ast.Expr expr = parseMultiplicativeExpression();

        while(match("+") | match("-")) {
            Token operator = tokens.get(-1);
            Ast.Expr right = parseMultiplicativeExpression();
            expr = new Ast.Expr.Binary(operator.getLiteral(), expr, right);
        }

        return expr;
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expr parseMultiplicativeExpression() throws ParseException {

        Ast.Expr expr = parseSecondaryExpression();

        while(match("*") | match("/")) {
            Token operator = tokens.get(-1);
            Ast.Expr right = parseSecondaryExpression();
            expr = new Ast.Expr.Binary(operator.getLiteral(), expr, right);
        }

        return expr;
    }

    /**
     * Parses the {@code secondary-expression} rule.
     */
    public Ast.Expr parseSecondaryExpression() throws ParseException {

        Ast.Expr expr = parsePrimaryExpression();

        Optional<Ast.Expr> receiver;
        receiver = Optional.of(expr);

        List<Ast.Expr> arguments = new ArrayList<>();

        String name = "";

        while(match(".")) {

            if (match(Token.Type.IDENTIFIER)) {

                name = tokens.get(-1).getLiteral();

                if (match("(")) {
                    if(peek(")")) { //if it is a method call (obj.name())
                        return new Ast.Expr.Function(receiver, name, arguments);
                    }
                    //otherwise, it is a function call (name(expr1,expr2,expr3)
                    parsePrimaryExpression();
                    while (match(",")) {
                        parsePrimaryExpression();
                    }
                    if (peek(")")) {
                        tokens.advance();
                    } else
                        throw new ParseException("Expected )", tokens.index);
                }
            } else {
                throw new ParseException("Expected identifier", tokens.index);
            }
            return new Ast.Expr.Access(receiver, name);
        }
        return expr;
    }

    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */
    public Ast.Expr parsePrimaryExpression() throws ParseException {

        if (match("NIL")) return new Ast.Expr.Literal(null); //for nil
        if (match("TRUE")) return new Ast.Expr.Literal(true); //for true
        if (match("FALSE")) return new Ast.Expr.Literal(false); //for false


        if (match(Token.Type.INTEGER)) //for an integer (big int)
            return new Ast.Expr.Literal(new BigInteger(tokens.get(-1).getLiteral()));

        if (match(Token.Type.DECIMAL)) //for a decimal (big decimal)
            return new Ast.Expr.Literal(new BigDecimal(tokens.get(-1).getLiteral()));

        if (match(Token.Type.CHARACTER)) //for a character (had to trim ')
            return new Ast.Expr.Literal(tokens.get(-1).getLiteral().charAt(1));

        if (match(Token.Type.STRING)) {//for a string (had to trim ")
            if (tokens.get(-1).getLiteral().contains("\\b")) // \b escape sequence
                return new Ast.Expr.Literal(tokens.get(-1).getLiteral().replace("\\b","\b").substring(1,tokens.get(-1).getLiteral().replace("\\b","\b").length() - 1));
            else if (tokens.get(-1).getLiteral().contains("\\n")) // \n escape sequence
                return new Ast.Expr.Literal(tokens.get(-1).getLiteral().replace("\\n","\n").substring(1,tokens.get(-1).getLiteral().replace("\\n","\n").length() - 1));
            else if (tokens.get(-1).getLiteral().contains("\\r")) // \r escape sequence
                return new Ast.Expr.Literal(tokens.get(-1).getLiteral().replace("\\r","\r").substring(1,tokens.get(-1).getLiteral().replace("\\r","\r").length() - 1));
            else if (tokens.get(-1).getLiteral().contains("\\t")) // \t escape sequence
                return new Ast.Expr.Literal(tokens.get(-1).getLiteral().replace("\\t","\t").substring(1,tokens.get(-1).getLiteral().replace("\\t","\t").length() - 1));
            else //if the string contains no escape sequences
                return new Ast.Expr.Literal(tokens.get(-1).getLiteral().substring(1, tokens.get(-1).getLiteral().length() - 1));
        }


        if (match("(")) {
            Ast.Expr expr = parseExpression();
            if (peek(")")) {
                tokens.advance();
                return new Ast.Expr.Group(expr);
            }
            else
                throw new ParseException("Expected )", tokens.index); //throw exception
        }

        if (match(Token.Type.IDENTIFIER)) {

            String name = tokens.get(-1).getLiteral();
            Optional<Ast.Expr> receiver = Optional.empty();

            List<Ast.Expr> arguments = new ArrayList<>();

            Optional<Ast.Expr> receiver2 = Optional.empty();
            Ast.Expr expr = new Ast.Expr.Access(receiver2, tokens.get(-1).getLiteral());

            if (match("(")) {
                if (!match(")")) {
                    expr = new Ast.Expr.Access(receiver2, tokens.get(-1).getLiteral());
                    arguments.add(parseExpression());
                    while (match(",")) {
                        arguments.add(parseExpression());
                    }
                    if (peek(")")) {
                        tokens.advance();
                        return new Ast.Expr.Function(receiver, name, arguments);
                    } else {
                        throw new ParseException("Expected )", tokens.index);
                    }
                } else {
                    return new Ast.Expr.Function(receiver, name, arguments);
                }
            }
            return expr;
        }
        return null;
    }

    /**
     * As in the lexer, returns {@code true} if the current sequence of tokens
     * matches the given patterns. Unlike the lexer, the pattern is not a regex;
     * instead it is either a {@link Token.Type}, which matches if the token's
     * type is the same, or a {@link String}, which matches if the token's
     * literal is the same.
     *
     * In other words, {@code Token(IDENTIFIER, "literal")} is matched by both
     * {@code peek(Token.Type.IDENTIFIER)} and {@code peek("literal")}.
     */
    private boolean peek(Object... patterns) {

        for (int i = 0; i < patterns.length; i++) {
            if (!tokens.has(i)) {
                return false;
            }
            else if (patterns[i] instanceof Token.Type) {
                if (patterns[i] != tokens.get(i).getType()) {
                    return false;
                }
            }
            else if (patterns[i] instanceof String) {
                if (!patterns[i].equals(tokens.get(i).getLiteral())) {
                    return false;
                }
            }
            else {
                throw new AssertionError("Invalid pattern object: " + patterns[i].getClass());
            }
        }
        return true;
    }

    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
     * and advances the token stream.
     */
    private boolean match(Object... patterns) {

        boolean peek = peek(patterns);
        if (peek) {
            for (int i = 0; i < patterns.length; i++) {
                tokens.advance();
            }
        }
        return peek;
    }

    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at index + offset.
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Gets the token at index + offset.
         */
        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        /**
         * Advances to the next token, incrementing the index.
         */
        public void advance() {
            index++;
        }

    }

}