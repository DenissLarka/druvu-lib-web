package com.druvu.web.php.internal.lex;

import com.druvu.web.php.internal.Location;
import com.druvu.web.php.internal.PhpEngineConfig;
import com.druvu.web.php.internal.PhpSyntaxException;
import com.druvu.web.php.internal.value.PhpFloat;
import com.druvu.web.php.internal.value.PhpInt;
import com.druvu.web.php.internal.value.PhpString;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Turns a template into tokens.
 *
 * <p>A PHP template is two languages interleaved, so the lexer has two modes. It starts in text mode, where everything
 * is literal HTML until an open tag; an open tag switches it to PHP mode, and {@code ?>} switches it back. Getting that
 * re-entry right is most of what a template lexer does — and it is why {@code ?>} written inside a string or a comment
 * has to be understood rather than merely searched for, which is exactly where the engine's previous tokenizer broke.
 *
 * <p>A string with something interpolated into it becomes a <em>run</em> of tokens rather than one, bracketed by the
 * two interpolation markers. The pieces are lexed here, in place, by the same code that lexes everything else — so the
 * parser can build a concatenation out of them without any part of the source ever being read twice.
 *
 * <p>Kept apart from the parser on purpose. The lexer is the layer where template engines rot, and separating it means
 * every boundary case here can be tested by looking at the tokens rather than at rendered output.
 *
 * @author Deniss Larka
 */
public final class PhpLexer {

    /** What {@code \v} stands for; Java has no escape of its own for it. */
    private static final char VERTICAL_TAB = 0x0B;

    /** What {@code \e} stands for. */
    private static final char ESCAPE = 0x1B;

    private final SourceCursor cursor;
    private final PhpEngineConfig config;

    private boolean inPhp;

    public PhpLexer(String source, String file, PhpEngineConfig config) {
        this.cursor = new SourceCursor(source, file);
        this.config = config;
    }

    /** Every token in the template, ending with {@code EOF}. */
    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        while (!cursor.atEnd()) {
            if (inPhp) {
                readPhpTokens(tokens);
            } else {
                readInlineHtml(tokens);
            }
        }
        tokens.add(Token.simple(TokenType.EOF, cursor.here()));
        return tokens;
    }

    // ---------------------------------------------------------------- text mode

    /** Reads literal text up to the next open tag, then the open tag itself. */
    private void readInlineHtml(List<Token> tokens) {
        Location start = cursor.here();
        int from = cursor.offset();
        while (!cursor.atEnd() && openTagAhead() == null) {
            cursor.advance();
        }
        if (cursor.offset() > from) {
            tokens.add(Token.of(TokenType.INLINE_HTML, start, cursor.slice(from, cursor.offset())));
        }
        if (!cursor.atEnd()) {
            tokens.add(readOpenTag());
        }
    }

    /** The open tag written at the cursor, or null if there is not one there. */
    private OpenTag openTagAhead() {
        if (!cursor.startsWith("<?")) {
            return null;
        }
        if (cursor.peek(2) == '=') {
            return new OpenTag(TokenType.OPEN_TAG_ECHO, 3);
        }
        if (cursor.matchesIgnoreCase(2, "php") && opensPhpBlock()) {
            return new OpenTag(TokenType.OPEN_TAG, 5);
        }
        // A bare "<?" is literal text unless the host turned short tags on: PHP's own manual discourages them, and
        // leaving them off is what keeps "<?xml ... ?>" in a template from being read as code.
        return config.shortOpenTag() ? new OpenTag(TokenType.OPEN_TAG, 2) : null;
    }

    /** PHP requires whitespace after {@code <?php}, or the end of the file. */
    private boolean opensPhpBlock() {
        char after = cursor.peek(5);
        return after == '\0' || isWhitespace(after);
    }

    private Token readOpenTag() {
        Location start = cursor.here();
        OpenTag tag = openTagAhead();
        cursor.advance(tag.length());
        inPhp = true;
        return Token.simple(tag.type(), start);
    }

    private record OpenTag(TokenType type, int length) {}

    // ----------------------------------------------------------------- php mode

    /** The next thing in PHP mode. Usually one token; a string with an interpolation in it is a whole run of them. */
    private void readPhpTokens(List<Token> tokens) {
        skipWhitespaceAndComments();
        if (cursor.atEnd()) {
            // An omitted closing tag at the end of a template is legal PHP, and common in include-only files.
            return;
        }
        if (cursor.startsWith("?>")) {
            tokens.add(readCloseTag(cursor.here()));
            return;
        }
        if (cursor.peek() == '"') {
            readDoubleQuoted(tokens);
            return;
        }
        if (cursor.startsWith("<<<")) {
            readHeredoc(tokens);
            return;
        }
        tokens.add(readOneToken());
    }

    /** One plain token: everything except a closing tag and the two kinds of interpolating string. */
    private Token readOneToken() {
        Location start = cursor.here();
        char c = cursor.peek();

        if (c == '$' && isIdentifierStart(cursor.peek(1))) {
            cursor.advance();
            return Token.of(TokenType.VARIABLE, start, readIdentifier());
        }
        if (isIdentifierStart(c)) {
            return Token.of(TokenType.IDENTIFIER, start, readIdentifier());
        }
        if (isDigit(c)) {
            return readNumber(start);
        }
        if (c == '\'') {
            return readSingleQuoted(start);
        }

        Symbol symbol = Symbol.longestMatching(cursor::startsWith);
        if (symbol != null) {
            cursor.advance(symbol.spelling().length());
            return Token.of(symbol, start);
        }
        throw new PhpSyntaxException(start, "Unexpected character '" + c + "'" + whyItIsMissing(c));
    }

    /** The excluded operators are worth naming: a template using one should learn that, not just that it failed. */
    private static String whyItIsMissing(char c) {
        return switch (c) {
            case '&' -> " - this dialect has no references and no bitwise operators";
            case '|', '^', '~' -> " - this dialect has no bitwise operators";
            case '@' -> " - this dialect has no error suppression; errors are meant to be seen";
            default -> "";
        };
    }

    private Token readCloseTag(Location start) {
        cursor.advance(2);
        inPhp = false;
        swallowOneLineEnding();
        return Token.simple(TokenType.CLOSE_TAG, start);
    }

    /**
     * PHP eats exactly one line ending straight after {@code ?>}, so a closing tag alone on its line leaves no blank
     * line behind it. Exactly one: a second blank line in the template is text the author asked for.
     */
    private void swallowOneLineEnding() {
        if (cursor.peek() == '\r') {
            cursor.advance();
        }
        if (cursor.peek() == '\n') {
            cursor.advance();
        }
    }

    // ----------------------------------------------------------------- comments

    private void skipWhitespaceAndComments() {
        while (!cursor.atEnd()) {
            char c = cursor.peek();
            if (isWhitespace(c)) {
                cursor.advance();
            } else if (cursor.startsWith("#[")) {
                throw new PhpSyntaxException(cursor.here(), "PHP attributes (#[...]) are not part of this dialect");
            } else if (cursor.startsWith("//") || c == '#') {
                skipLineComment();
            } else if (cursor.startsWith("/*")) {
                skipBlockComment();
            } else {
                return;
            }
        }
    }

    /** A line comment ends at the next newline or at {@code ?>}, whichever comes first — the closing tag wins. */
    private void skipLineComment() {
        while (!cursor.atEnd() && cursor.peek() != '\n' && !cursor.startsWith("?>")) {
            cursor.advance();
        }
    }

    private void skipBlockComment() {
        Location start = cursor.here();
        cursor.advance(2);
        while (!cursor.startsWith("*/")) {
            if (cursor.atEnd()) {
                throw new PhpSyntaxException(start, "Unterminated block comment");
            }
            cursor.advance();
        }
        cursor.advance(2);
    }

    // -------------------------------------------------------------- identifiers

    private String readIdentifier() {
        int from = cursor.offset();
        while (isIdentifierPart(cursor.peek())) {
            cursor.advance();
        }
        return cursor.slice(from, cursor.offset());
    }

    // ------------------------------------------------------------------ numbers

    private Token readNumber(Location start) {
        rejectNonDecimalBase(start);

        StringBuilder digits = new StringBuilder();
        readDigits(digits);

        boolean floating = false;
        if (cursor.peek() == '.' && isDigit(cursor.peek(1))) {
            floating = true;
            digits.append(cursor.advance());
            readDigits(digits);
        }
        if (looksLikeExponent()) {
            floating = true;
            digits.append(cursor.advance());
            if (cursor.peek() == '+' || cursor.peek() == '-') {
                digits.append(cursor.advance());
            }
            readDigits(digits);
        }

        String number = digits.toString();
        if (floating) {
            return Token.of(TokenType.FLOAT_LITERAL, start, PhpFloat.of(Double.parseDouble(number)));
        }
        try {
            return Token.of(TokenType.INT_LITERAL, start, PhpInt.of(Long.parseLong(number)));
        } catch (NumberFormatException tooLargeForAnInteger) {
            // PHP does the same: an integer literal that does not fit becomes a float.
            return Token.of(TokenType.FLOAT_LITERAL, start, PhpFloat.of(Double.parseDouble(number)));
        }
    }

    /**
     * Hexadecimal, octal and binary literals are out of scope, and a leading zero means octal in PHP. Reading
     * {@code 0755} as seven hundred and fifty-five would be quietly wrong, so it is refused instead.
     */
    private void rejectNonDecimalBase(Location start) {
        if (cursor.peek() != '0') {
            return;
        }
        char next = cursor.peek(1);
        boolean otherBase = next == 'x' || next == 'X' || next == 'b' || next == 'B' || next == 'o' || next == 'O';
        if (otherBase || isDigit(next) || next == '_') {
            throw new PhpSyntaxException(
                    start, "Only decimal numbers are supported; hexadecimal, octal and binary literals are not");
        }
    }

    /** Digits, dropping PHP 7.4's underscore separators as they are read. An underscore must sit between digits. */
    private void readDigits(StringBuilder digits) {
        while (!cursor.atEnd()) {
            char c = cursor.peek();
            if (isDigit(c)) {
                digits.append(cursor.advance());
            } else if (c == '_' && endsWithDigit(digits) && isDigit(cursor.peek(1))) {
                cursor.advance();
            } else {
                return;
            }
        }
    }

    /** {@code 1e3} has an exponent; {@code 1e} is the number 1 followed by the identifier {@code e}. */
    private boolean looksLikeExponent() {
        char c = cursor.peek();
        if (c != 'e' && c != 'E') {
            return false;
        }
        int ahead = 1;
        if (cursor.peek(ahead) == '+' || cursor.peek(ahead) == '-') {
            ahead++;
        }
        return isDigit(cursor.peek(ahead));
    }

    private static boolean endsWithDigit(StringBuilder digits) {
        return !digits.isEmpty() && isDigit(digits.charAt(digits.length() - 1));
    }

    // ------------------------------------------------------------------ strings

    /** A single-quoted string never interpolates and knows only two escapes, so it is always exactly one token. */
    private Token readSingleQuoted(Location start) {
        cursor.advance();
        StringBuilder value = new StringBuilder();
        while (true) {
            if (cursor.atEnd()) {
                throw new PhpSyntaxException(start, "Unterminated string literal");
            }
            char c = cursor.peek();
            if (c == '\'') {
                cursor.advance();
                return Token.of(TokenType.STRING_LITERAL, start, PhpString.of(value.toString()));
            }
            if (c == '\\') {
                cursor.advance();
                readSingleQuotedEscape(value);
            } else {
                value.append(cursor.advance());
            }
        }
    }

    private void readDoubleQuoted(List<Token> tokens) {
        Location start = cursor.here();
        cursor.advance();
        readInterpolatingBody(tokens, start, () -> cursor.peek() == '"', 0);
        cursor.advance();
    }

    /**
     * The shared body of a double-quoted string and a heredoc: escapes decoded, interpolations split off, everything
     * else copied through. The two differ only in where the body stops and whether each line sheds some indentation, so
     * those are the two parameters.
     */
    private void readInterpolatingBody(List<Token> tokens, Location start, BooleanSupplier ends, int indent) {
        List<Token> parts = new ArrayList<>();
        StringBuilder literal = new StringBuilder();
        skipIndent(indent);

        while (!ends.getAsBoolean()) {
            if (cursor.atEnd()) {
                throw new PhpSyntaxException(start, "Unterminated string literal");
            }
            char c = cursor.peek();
            if (c == '\\') {
                cursor.advance();
                readDoubleQuotedEscape(literal);
            } else if (startsInterpolation()) {
                flush(parts, literal, start);
                readInterpolation(parts);
            } else {
                literal.append(cursor.advance());
                if (c == '\n') {
                    skipIndent(indent);
                }
            }
        }
        flush(parts, literal, start);

        if (parts.isEmpty()) {
            tokens.add(Token.of(TokenType.STRING_LITERAL, start, PhpString.of("")));
        } else if (parts.size() == 1 && parts.get(0).is(TokenType.STRING_LITERAL)) {
            tokens.add(parts.get(0));
        } else {
            tokens.add(Token.simple(TokenType.INTERPOLATION_START, start));
            tokens.addAll(parts);
            tokens.add(Token.simple(TokenType.INTERPOLATION_END, cursor.here()));
        }
    }

    private void flush(List<Token> parts, StringBuilder literal, Location where) {
        if (literal.isEmpty()) {
            return;
        }
        parts.add(Token.of(TokenType.STRING_LITERAL, where, PhpString.of(literal.toString())));
        literal.setLength(0);
    }

    /** In an interpolating string an unescaped {@code $name} or a {@code {$} begins an interpolation; nothing else. */
    private boolean startsInterpolation() {
        if (cursor.peek() == '$') {
            return isIdentifierStart(cursor.peek(1));
        }
        return cursor.peek() == '{' && cursor.peek(1) == '$';
    }

    private void readInterpolation(List<Token> parts) {
        if (cursor.peek() == '{') {
            readComplexInterpolation(parts);
        } else {
            readSimpleInterpolation(parts);
        }
    }

    /**
     * {@code "$name"} and {@code "$name[key]"}.
     *
     * <p>The subscript form is its own small grammar rather than a real expression: PHP reads the key of a
     * {@code $a[key]} written inside a string as the string {@code "key"}, and quoting it there is an error.
     */
    private void readSimpleInterpolation(List<Token> parts) {
        Location start = cursor.here();
        cursor.advance();
        parts.add(Token.of(TokenType.VARIABLE, start, readIdentifier()));

        if (cursor.peek() != '[') {
            return;
        }
        parts.add(Token.of(Symbol.LEFT_BRACKET, cursor.here()));
        cursor.advance();
        parts.add(readSimpleSubscript());
        if (cursor.peek() != ']') {
            throw new PhpSyntaxException(cursor.here(), "Expected ']' to close the subscript inside the string");
        }
        parts.add(Token.of(Symbol.RIGHT_BRACKET, cursor.here()));
        cursor.advance();
    }

    private Token readSimpleSubscript() {
        Location start = cursor.here();
        if (cursor.peek() == '$') {
            cursor.advance();
            return Token.of(TokenType.VARIABLE, start, readIdentifier());
        }
        if (isDigit(cursor.peek()) || cursor.peek() == '-' && isDigit(cursor.peek(1))) {
            StringBuilder digits = new StringBuilder();
            if (cursor.peek() == '-') {
                digits.append(cursor.advance());
            }
            while (isDigit(cursor.peek())) {
                digits.append(cursor.advance());
            }
            return Token.of(TokenType.INT_LITERAL, start, PhpInt.of(Long.parseLong(digits.toString())));
        }
        return Token.of(TokenType.STRING_LITERAL, start, PhpString.of(readIdentifier()));
    }

    /** {@code "{$anything}"}, where anything really is any expression — lexed here by the ordinary rules. */
    private void readComplexInterpolation(List<Token> parts) {
        Location start = cursor.here();
        cursor.advance();
        int depth = 1;
        while (true) {
            skipWhitespaceAndComments();
            if (cursor.atEnd()) {
                throw new PhpSyntaxException(start, "Unterminated {$...} inside a string");
            }
            if (cursor.startsWith("?>")) {
                throw new PhpSyntaxException(cursor.here(), "A closing tag cannot appear inside {$...}");
            }
            int before = parts.size();
            readPhpTokens(parts);
            for (int i = before; i < parts.size(); i++) {
                Token token = parts.get(i);
                if (token.is(Symbol.LEFT_BRACE)) {
                    depth++;
                } else if (token.is(Symbol.RIGHT_BRACE) && --depth == 0) {
                    while (parts.size() > i) {
                        parts.remove(parts.size() - 1);
                    }
                    return;
                }
            }
        }
    }

    // ----------------------------------------------------------------- heredocs

    /**
     * {@code <<<LABEL} … {@code LABEL}, and its non-interpolating twin {@code <<<'LABEL'}.
     *
     * <p>Since PHP 7.3 the closing label may be indented, and whatever indentation it carries is removed from every
     * line of the body — which is what lets a heredoc sit at the indentation of the code around it.
     */
    private void readHeredoc(List<Token> tokens) {
        Location start = cursor.here();
        cursor.advance(3);
        while (cursor.peek() == ' ' || cursor.peek() == '\t') {
            cursor.advance();
        }

        char quote = cursor.peek();
        boolean quoted = quote == '\'' || quote == '"';
        if (quoted) {
            cursor.advance();
        }
        String label = readIdentifier();
        if (label.isEmpty()) {
            throw new PhpSyntaxException(start, "Expected a label after <<<");
        }
        if (quoted) {
            if (cursor.peek() != quote) {
                throw new PhpSyntaxException(start, "Expected " + quote + " to close the heredoc label");
            }
            cursor.advance();
        }
        if (cursor.peek() == '\r') {
            cursor.advance();
        }
        if (cursor.peek() != '\n') {
            throw new PhpSyntaxException(start, "The body of a heredoc must begin on the line after its label");
        }
        cursor.advance();

        Closing closing = findClosing(start, label);
        int bodyStart = cursor.offset();
        int bodyEnd = bodyStart + closing.bodyLength();
        int afterLabel = bodyStart + closing.afterLabel();

        if (quote == '\'') {
            String body = stripIndent(cursor.slice(bodyStart, bodyEnd), closing.indent());
            tokens.add(Token.of(TokenType.STRING_LITERAL, start, PhpString.of(body)));
        } else {
            readInterpolatingBody(tokens, start, () -> cursor.offset() >= bodyEnd, closing.indent());
        }
        cursor.advance(afterLabel - cursor.offset());
    }

    /**
     * Where the body ends and how far the closing label is indented, both measured from the start of the body.
     *
     * @param bodyLength characters of body, not counting the line ending in front of the closing label
     * @param indent how far the closing label is indented, and so how much every line of the body sheds
     * @param afterLabel where the label ends, so the caller knows where to carry on
     */
    private record Closing(int bodyLength, int indent, int afterLabel) {}

    private Closing findClosing(Location start, String label) {
        int lineStart = 0;
        while (true) {
            int indent = 0;
            while (cursor.peek(lineStart + indent) == ' ' || cursor.peek(lineStart + indent) == '\t') {
                indent++;
            }
            if (labelAt(lineStart + indent, label)) {
                int bodyLength = lineStart == 0 ? 0 : lineStart - 1;
                if (bodyLength > 0 && cursor.peek(bodyLength - 1) == '\r') {
                    bodyLength--;
                }
                return new Closing(bodyLength, indent, lineStart + indent + label.length());
            }
            int nextLine = lineStart;
            while (cursor.peek(nextLine) != '\n') {
                if (cursor.peek(nextLine) == '\0') {
                    throw new PhpSyntaxException(start, "Unterminated heredoc: no line holding " + label);
                }
                nextLine++;
            }
            lineStart = nextLine + 1;
        }
    }

    /** A label closes the heredoc only when nothing runs on from it, so a line reading {@code LABELS} does not. */
    private boolean labelAt(int ahead, String label) {
        for (int i = 0; i < label.length(); i++) {
            if (cursor.peek(ahead + i) != label.charAt(i)) {
                return false;
            }
        }
        return !isIdentifierPart(cursor.peek(ahead + label.length()));
    }

    private void skipIndent(int indent) {
        for (int i = 0; i < indent && (cursor.peek() == ' ' || cursor.peek() == '\t'); i++) {
            cursor.advance();
        }
    }

    private static String stripIndent(String body, int indent) {
        if (indent == 0) {
            return body;
        }
        StringBuilder stripped = new StringBuilder(body.length());
        for (String line : body.split("\n", -1)) {
            int skip = 0;
            while (skip < indent && skip < line.length() && (line.charAt(skip) == ' ' || line.charAt(skip) == '\t')) {
                skip++;
            }
            if (!stripped.isEmpty()) {
                stripped.append('\n');
            }
            stripped.append(line, skip, line.length());
        }
        return stripped.toString();
    }

    // ------------------------------------------------------------------ escapes

    /** A single-quoted string knows only {@code \\} and {@code \'}; every other backslash stands for itself. */
    private void readSingleQuotedEscape(StringBuilder value) {
        char c = cursor.peek();
        if (c == '\\' || c == '\'') {
            value.append(cursor.advance());
        } else {
            value.append('\\');
        }
    }

    private void readDoubleQuotedEscape(StringBuilder value) {
        if (cursor.atEnd()) {
            value.append('\\');
            return;
        }
        switch (cursor.peek()) {
            case 'n' -> appendEscaped(value, '\n');
            case 't' -> appendEscaped(value, '\t');
            case 'r' -> appendEscaped(value, '\r');
            case 'v' -> appendEscaped(value, VERTICAL_TAB);
            case 'e' -> appendEscaped(value, ESCAPE);
            case 'f' -> appendEscaped(value, '\f');
            case '\\' -> appendEscaped(value, '\\');
            case '$' -> appendEscaped(value, '$');
            case '"' -> appendEscaped(value, '"');
            case 'x' -> readHexEscape(value);
            case 'u' -> readUnicodeEscape(value);
            default -> {
                if (isOctalDigit(cursor.peek())) {
                    readOctalEscape(value);
                } else {
                    // PHP keeps an escape it does not know as a literal backslash, then the character itself.
                    value.append('\\');
                }
            }
        }
    }

    private void appendEscaped(StringBuilder value, char decoded) {
        cursor.advance();
        value.append(decoded);
    }

    /** {@code \xHH}, one or two hexadecimal digits. Without any, PHP leaves the {@code \x} as written. */
    private void readHexEscape(StringBuilder value) {
        int length = 0;
        while (length < 2 && isHexDigit(cursor.peek(1 + length))) {
            length++;
        }
        if (length == 0) {
            value.append('\\');
            return;
        }
        int from = cursor.offset() + 1;
        value.append((char) Integer.parseInt(cursor.slice(from, from + length), 16));
        cursor.advance(1 + length);
    }

    /**
     * <code>&#92;u{HHHH}</code>. The braces are required: PHP leaves a brace-less <code>&#92;u</code> as written.
     * (Written with an entity because javac reads a unicode escape even inside a comment.)
     */
    private void readUnicodeEscape(StringBuilder value) {
        if (cursor.peek(1) != '{') {
            value.append('\\');
            return;
        }
        int length = 0;
        while (isHexDigit(cursor.peek(2 + length))) {
            length++;
        }
        if (length == 0 || cursor.peek(2 + length) != '}') {
            throw new PhpSyntaxException(cursor.here(), "Malformed \\u{...} escape");
        }
        int from = cursor.offset() + 2;
        int codePoint = Integer.parseInt(cursor.slice(from, from + length), 16);
        if (codePoint > Character.MAX_CODE_POINT) {
            throw new PhpSyntaxException(cursor.here(), "Code point out of range in \\u{...} escape");
        }
        value.appendCodePoint(codePoint);
        cursor.advance(3 + length);
    }

    /** {@code \0} to {@code \777}, which PHP narrows to a single byte. */
    private void readOctalEscape(StringBuilder value) {
        int length = 0;
        while (length < 3 && isOctalDigit(cursor.peek(length))) {
            length++;
        }
        int from = cursor.offset();
        value.append((char) (Integer.parseInt(cursor.slice(from, from + length), 8) & 0xFF));
        cursor.advance(length);
    }

    // ----------------------------------------------------------- character sets

    /** PHP allows the bytes above ASCII in a name, and so does this. */
    private static boolean isIdentifierStart(char c) {
        return c == '_' || c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= 128;
    }

    private static boolean isIdentifierPart(char c) {
        return isIdentifierStart(c) || isDigit(c);
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isHexDigit(char c) {
        return isDigit(c) || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F';
    }

    private static boolean isOctalDigit(char c) {
        return c >= '0' && c <= '7';
    }

    private static boolean isWhitespace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == VERTICAL_TAB || c == '\f';
    }
}
