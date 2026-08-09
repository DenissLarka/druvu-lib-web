package com.druvu.web.php.internal.runtime;

/**
 * Turning a value into text that is safe to put in a page.
 *
 * <p>Matches PHP's {@code htmlspecialchars} with the flags it has defaulted to since 8.1: both kinds of quote are
 * escaped, and anything that is not valid text is replaced rather than passed through. Escaping only the angle brackets
 * is the classic half-measure that leaves an attribute value exploitable.
 *
 * @author Deniss Larka
 */
public final class Html {

    /** What an unpaired surrogate becomes, which is what PHP's ENT_SUBSTITUTE does for a malformed byte sequence. */
    private static final char REPLACEMENT = '\uFFFD';

    private Html() {}

    public static String escape(String text) {
        StringBuilder escaped = new StringBuilder(text.length() + 16);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '&' -> escaped.append("&amp;");
                case '<' -> escaped.append("&lt;");
                case '>' -> escaped.append("&gt;");
                case '"' -> escaped.append("&quot;");
                case '\'' -> escaped.append("&#039;");
                default -> escaped.append(isLoneSurrogate(text, i) ? REPLACEMENT : c);
            }
        }
        return escaped.toString();
    }

    /** The five escapes, undone. */
    public static String unescape(String text) {
        return text.replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#039;", "'")
                .replace("&#39;", "'")
                .replace("&amp;", "&");
    }

    /**
     * Every character outside ASCII as a numeric entity, on top of the five escapes.
     *
     * <p>PHP writes named entities here ({@code &eacute;}); these are numeric ({@code &#233;}). Browsers render them
     * identically, and the alternative is carrying a table of two hundred names for a function that UTF-8 made
     * unnecessary. Noted rather than hidden.
     */
    public static String escapeAll(String text) {
        StringBuilder escaped = new StringBuilder(text.length() + 16);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < 128) {
                escaped.append(escape(String.valueOf(c)));
            } else if (isLoneSurrogate(text, i)) {
                escaped.append(REPLACEMENT);
            } else if (Character.isHighSurrogate(c)) {
                escaped.append("&#").append(text.codePointAt(i)).append(';');
                i++;
            } else {
                escaped.append("&#").append((int) c).append(';');
            }
        }
        return escaped.toString();
    }

    private static boolean isLoneSurrogate(String text, int at) {
        char c = text.charAt(at);
        if (Character.isHighSurrogate(c)) {
            return at + 1 >= text.length() || !Character.isLowSurrogate(text.charAt(at + 1));
        }
        return Character.isLowSurrogate(c) && (at == 0 || !Character.isHighSurrogate(text.charAt(at - 1)));
    }
}
