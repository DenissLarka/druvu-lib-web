package com.druvu.web.php.internal.lex;

import com.druvu.web.php.internal.Location;
import com.druvu.web.php.internal.value.PhpValue;

/**
 * One token, and where in the template it was written.
 *
 * <p>Which of the three payloads is filled in follows from the type, and only one ever is: {@code text} for inline
 * HTML, variables and identifiers; {@code symbol} for operators and punctuators; {@code value} for the three kinds of
 * literal, already converted so that nothing downstream has to parse a number or unescape a string twice.
 *
 * @param type what kind of token this is
 * @param location where it starts, in the template it came from
 * @param text the token's spelling, for the types that carry one; null otherwise
 * @param symbol which operator or punctuator, for {@code SYMBOL} tokens; null otherwise
 * @param value the literal's value, for the three literal types; null otherwise
 * @author Deniss Larka
 */
public record Token(TokenType type, Location location, String text, Symbol symbol, PhpValue value) {

    /** A token that carries nothing beyond its type: the tags and the end of the file. */
    public static Token simple(TokenType type, Location location) {
        return new Token(type, location, null, null, null);
    }

    /** Inline HTML, a variable name or an identifier. */
    public static Token of(TokenType type, Location location, String text) {
        return new Token(type, location, text, null, null);
    }

    public static Token of(Symbol symbol, Location location) {
        return new Token(TokenType.SYMBOL, location, symbol.spelling(), symbol, null);
    }

    /** An integer, float or string literal, with its value already worked out. */
    public static Token of(TokenType type, Location location, PhpValue value) {
        return new Token(type, location, null, null, value);
    }

    public boolean is(TokenType candidate) {
        return type == candidate;
    }

    public boolean is(Symbol candidate) {
        return symbol == candidate;
    }

    /** Whether this is the given bare word. PHP's keywords are not case-sensitive, so neither is this. */
    public boolean isIdentifier(String spelling) {
        return type == TokenType.IDENTIFIER && text.equalsIgnoreCase(spelling);
    }

    @Override
    public String toString() {
        if (symbol != null) {
            return type + "(" + symbol.spelling() + ") at " + location;
        }
        if (value != null) {
            return type + "(" + value + ") at " + location;
        }
        if (text != null) {
            return type + "(" + text + ") at " + location;
        }
        return type + " at " + location;
    }
}
