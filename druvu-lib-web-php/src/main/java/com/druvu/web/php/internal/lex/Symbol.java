package com.druvu.web.php.internal.lex;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * Every operator and punctuator the dialect knows, each with the one way it is spelled.
 *
 * <p>Naming them lets the parser say {@code Symbol.SPACESHIP} instead of matching a string, and keeping the spellings
 * here means the lexer needs no table of its own: {@link #longestMatching} tries the longest spelling first, so
 * {@code <=>} is never mistaken for {@code <=} followed by {@code >}.
 *
 * <p>Deliberately absent: the bitwise family ({@code & | ^ << >>}), error suppression ({@code @}), reference assignment
 * ({@code =&}) and the spread operator. None of them lays out a page, and leaving them out means a template that uses
 * one gets an error instead of a surprise.
 *
 * @author Deniss Larka
 */
public enum Symbol {
    SPACESHIP("<=>"),
    IDENTICAL("==="),
    NOT_IDENTICAL("!=="),
    COALESCE_ASSIGN("??="),
    NULLSAFE_ARROW("?->"),

    EQUAL("=="),
    NOT_EQUAL("!="),
    NOT_EQUAL_ALT("<>"),
    LESS_OR_EQUAL("<="),
    GREATER_OR_EQUAL(">="),
    AND("&&"),
    OR("||"),
    COALESCE("??"),
    ELVIS("?:"),
    POWER("**"),
    INCREMENT("++"),
    DECREMENT("--"),
    PLUS_ASSIGN("+="),
    MINUS_ASSIGN("-="),
    TIMES_ASSIGN("*="),
    DIVIDE_ASSIGN("/="),
    MODULO_ASSIGN("%="),
    CONCAT_ASSIGN(".="),
    DOUBLE_ARROW("=>"),
    ARROW("->"),

    PLUS("+"),
    MINUS("-"),
    TIMES("*"),
    DIVIDE("/"),
    MODULO("%"),
    CONCAT("."),
    LESS("<"),
    GREATER(">"),
    NOT("!"),
    QUESTION("?"),
    COLON(":"),
    ASSIGN("="),
    LEFT_PAREN("("),
    RIGHT_PAREN(")"),
    LEFT_BRACKET("["),
    RIGHT_BRACKET("]"),
    LEFT_BRACE("{"),
    RIGHT_BRACE("}"),
    COMMA(","),
    SEMICOLON(";");

    /** Longest spelling first, which is all "maximal munch" amounts to. */
    private static final List<Symbol> LONGEST_FIRST = Arrays.stream(values())
            .sorted(Comparator.comparingInt((Symbol symbol) -> symbol.spelling.length())
                    .reversed())
            .toList();

    private final String spelling;

    Symbol(String spelling) {
        this.spelling = spelling;
    }

    public String spelling() {
        return spelling;
    }

    /**
     * The longest symbol whose spelling the caller says is written at its position, or null if none is.
     *
     * <p>The caller supplies the match because only it knows where "there" is; the order is supplied here, because only
     * this class knows that {@code <=>} has to be tried before {@code <=}.
     */
    public static Symbol longestMatching(Predicate<String> spellingIsWrittenHere) {
        for (Symbol symbol : LONGEST_FIRST) {
            if (spellingIsWrittenHere.test(symbol.spelling)) {
                return symbol;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return spelling;
    }
}
