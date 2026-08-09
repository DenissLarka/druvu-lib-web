package com.druvu.web.php.internal.lex;

/**
 * What kind of thing a {@link Token} is. Which operator or punctuator a {@code SYMBOL} token stands for is a separate
 * question, answered by {@link Symbol} — so this list stays short enough to read at a glance.
 *
 * @author Deniss Larka
 */
public enum TokenType {

    /** Literal text outside any PHP tag. Written to the response exactly as it appears, never escaped. */
    INLINE_HTML,

    /** {@code <?php} */
    OPEN_TAG,

    /** {@code <?=}, which opens a tag and echoes the expression that follows. */
    OPEN_TAG_ECHO,

    /** {@code ?>} */
    CLOSE_TAG,

    /** {@code $name}. The token's text is the name without the dollar. */
    VARIABLE,

    /** A bare word: a function name, a constant, or one of the language's keywords. */
    IDENTIFIER,

    /** A decimal integer literal; the token carries its value. */
    INT_LITERAL,

    /** A decimal float literal; the token carries its value. */
    FLOAT_LITERAL,

    /** A quoted string; the token carries its value with every escape already resolved. */
    STRING_LITERAL,

    /**
     * Opens the pieces of a string that had something interpolated into it. What follows, until the matching end, is
     * the alternating literal and expression tokens the parser concatenates back together.
     */
    INTERPOLATION_START,

    /** Closes the pieces opened by {@link #INTERPOLATION_START}. */
    INTERPOLATION_END,

    /** An operator or a punctuator; see {@link Token#symbol()}. */
    SYMBOL,

    /** The end of the template. */
    EOF
}
