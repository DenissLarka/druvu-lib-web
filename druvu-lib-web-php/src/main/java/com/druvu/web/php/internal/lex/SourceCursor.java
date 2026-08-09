package com.druvu.web.php.internal.lex;

import com.druvu.web.php.internal.Location;

/**
 * A position in a template, and the bookkeeping that keeps it honest.
 *
 * <p>Every character the lexer consumes goes through {@link #advance()}, which is the only place line and column are
 * updated. That is the whole reason this is its own class: get it right once, and no error message can ever point at
 * the wrong line.
 *
 * @author Deniss Larka
 */
final class SourceCursor {

    private final String source;
    private final String file;

    private int at;
    private int line = 1;
    private int lineStart;

    SourceCursor(String source, String file) {
        this.source = source;
        this.file = file;
    }

    boolean atEnd() {
        return at >= source.length();
    }

    int offset() {
        return at;
    }

    /** The character ahead of the cursor, or {@code '\0'} past the end of the template. */
    char peek(int ahead) {
        int position = at + ahead;
        return position < source.length() ? source.charAt(position) : '\0';
    }

    char peek() {
        return peek(0);
    }

    boolean startsWith(String text) {
        return source.startsWith(text, at);
    }

    boolean matchesIgnoreCase(int ahead, String text) {
        return source.regionMatches(true, at + ahead, text, 0, text.length());
    }

    /** Consumes one character and returns it. */
    char advance() {
        char c = source.charAt(at++);
        if (c == '\n') {
            line++;
            lineStart = at;
        }
        return c;
    }

    void advance(int count) {
        for (int i = 0; i < count; i++) {
            advance();
        }
    }

    String slice(int from, int to) {
        return source.substring(from, to);
    }

    /** Where the cursor is now. */
    Location here() {
        return new Location(file, line, at - lineStart + 1, at);
    }
}
