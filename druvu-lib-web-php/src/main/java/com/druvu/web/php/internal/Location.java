package com.druvu.web.php.internal;

/**
 * A position in a template source file. Every token and every AST node carries one, so an error can always name the
 * file and line it came from rather than an offset into an expression fragment.
 *
 * @param file the template path the position belongs to
 * @param line 1-based line number
 * @param column 1-based column number
 * @param offset 0-based character offset from the start of the file
 * @author Deniss Larka
 */
public record Location(String file, int line, int column, int offset) {

    /** Used where a node is built outside a parse, mainly in tests. */
    public static final Location UNKNOWN = new Location("<unknown>", 0, 0, -1);

    @Override
    public String toString() {
        return file + ":" + line + ":" + column;
    }
}
