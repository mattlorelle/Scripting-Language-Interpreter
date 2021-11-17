package plc.project;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.*;

/**
 * The lexer works through three main functions:
 *
 *  - {@link #lex()}, which repeatedly calls lexToken() and skips whitespace
 *  - {@link #lexToken()}, which lexes the next token
 *  - {@link CharStream}, which manages the state of the lexer and literals
 *
 * If the lexer fails to parse something (such as an unterminated string) you
 * should throw a {@link ParseException} with an index at the character which is
 * invalid or missing.
 *
 * The {@link #peek(String...)} and {@link #match(String...)} functions are
 * helpers you need to use, they will make the implementation a lot easier.
 */
public final class Lexer {

    private final CharStream chars;

    public Lexer(String input) {
        chars = new CharStream(input);
    }

    /**
     * Repeatedly lex the input using {@link #lexToken()}, also skipping over
     * whitespace where appropriate.
     */
    public List<Token> lex() {

        List<Token> myList = new ArrayList<Token>();

        while(chars.has(0)) {

            while (peek("[␊␍␉\\n\\r\\t]")) {
                chars.advance();
                chars.skip();
            }
            myList.add(lexToken());
        }
        return myList;
    }

    /**
     * This method determines the type of the next token, delegating to the
     * appropriate lex method. As such, it is best for this method to not change
     * the state of the char stream (thus, use peek not match).
     *
     * The next character should start a valid token since whitespace is handled
     * by {@link #lex()}
     */
    public Token lexToken() {

        if (!peek("[+-]", "[0-9]") && peek("[A-Za-z_][A-Za-z0-9_-]*"))
            return lexIdentifier();
        else if (peek("[0-9]") || peek("[+-]","[0-9]"))
            return lexNumber();
        else if (peek("\'"))
            return lexCharacter();
        else if (peek("\""))
            return lexString();
        else if (peek("\\\\"))
            lexEscape();
        else
            return lexOperator();

        return null;
    }

    public Token lexIdentifier() {

        if (!match("[A-Za-z_][A-Za-z0-9_-]*"))
            throw new ParseException("Something went wrong.", chars.index);

        while (match("[A-Za-z_]")){}
        while (match("[A-Za-z0-9_-]*")){}
        return chars.emit(Token.Type.IDENTIFIER);
    }

    //need to look at - project spec has integer and decimal not number
    public Token lexNumber() {

        if (!match("[+-]?","[0-9]") && !match("[0-9]"))
            throw new ParseException("Something went wrong.", chars.index);
        else if (chars.has(1)) {
            while (match("[0-9]+")) {}
            if (match("\\.")) {
                while (match("[0-9]")){}
                return chars.emit(Token.Type.DECIMAL);
            }
            else {
                return chars.emit(Token.Type.INTEGER);
            }
        }
        else
            return chars.emit(Token.Type.INTEGER);
    }

    public Token lexCharacter() {

        if (!match("'"))
            throw new ParseException("Something went wrong.", chars.index);
        else {
            if (!match("\'")) {
                if (!chars.has(0))
                    throw new ParseException("Unterminated.", chars.index);
                else if (peek("\\\\","[bnrt'\"\\\\]")) {
                    chars.advance();
                }
                chars.advance();
            }
            if (!match("\'"))
                throw new ParseException("Improper format.", chars.index);
            else
                return chars.emit(Token.Type.CHARACTER);
        }
    }

    public Token lexString() {

        if (!match("[\"]"))
            throw new ParseException("Something went wrong.", chars.index);
        else {
            while (!match("\"")) {
                if (!chars.has(0))
                    throw new ParseException("Unterminated.", chars.index);
                else if (match("\\\\","[^bnrt'\"\\\\]"))
                    throw new ParseException("Escape.", chars.index);
                chars.advance();
            }
            return chars.emit(Token.Type.STRING);
        }
    }

    public void lexEscape() {

        match("'\\' [bnrt'\"\\\\]");
    }

    //need to look at - possibly incorrect regex
    public Token lexOperator() {

        if (match("[<>!=]'='?|[^ ]")) {
            if (peek("=")) {
                chars.advance();
                return new Token(Token.Type.OPERATOR, String.valueOf(chars.get(-2)) + String.valueOf(chars.get(-1)), chars.index - 2);
            } else {
                return new Token(Token.Type.OPERATOR, String.valueOf(chars.get(-1)), chars.index - 1);
            }
        } else {
            throw new ParseException("Something went wrong.", chars.index);
        }
    }

    /**
     * Returns true if the next sequence of characters match the given patterns,
     * which should be a regex. For example, {@code peek("a", "b", "c")} would
     * return true if the next characters are {@code 'a', 'b', 'c'}.
     */
    public boolean peek(String... patterns) {

        for (int i = 0; i < patterns.length; i++) {
            if (!chars.has(i) || !String.valueOf(chars.get(i)).matches(patterns[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true in the same way as {@link #peek(String...)}, but also
     * advances the character stream past all matched characters if peek returns
     * true. Hint - it's easiest to have this method simply call peek.
     */
    public boolean match(String... patterns) {

        boolean peek = peek (patterns);
        if (peek) {
            for (int i = 0; i < patterns.length; i++) {
                chars.advance();
            }
        }
        return peek;
    }

    /**
     * A helper class maintaining the input string, current index of the char
     * stream, and the current length of the token being matched.
     *
     * You should rely on peek/match for state management in nearly all cases.
     * The only field you need to access is {@link #index} for any {@link
     * ParseException} which is thrown.
     */
    public static final class CharStream {

        private final String input;
        private int index = 0;
        private int length = 0;

        public CharStream(String input) {
            this.input = input;
        }

        public boolean has(int offset) {
            return index + offset < input.length();
        }

        public char get(int offset) {
            return input.charAt(index + offset);
        }

        public void advance() {
            index++;
            length++;
        }

        public void skip() {
            length = 0;
        }

        public Token emit(Token.Type type) {
            int start = index - length;
            skip();
            return new Token(type, input.substring(start, index), start);
        }

    }

}
