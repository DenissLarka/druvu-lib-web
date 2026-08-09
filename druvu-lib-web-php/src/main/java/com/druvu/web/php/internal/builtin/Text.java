package com.druvu.web.php.internal.builtin;

/**
 * String arithmetic done in code points rather than bytes.
 *
 * <p>PHP counts a string in bytes, so {@code strlen("café")} is 5 there. That answer is only ever useful to someone
 * thinking about storage; a template author asking for the length of a word wants the number of characters. The dialect
 * counts characters throughout, and the {@code mb_} functions are aliases rather than a second set — the divergence is
 * deliberate and it is the same one everywhere.
 *
 * @author Deniss Larka
 */
final class Text {

    private Text() {}

    static int length(String text) {
        return text.codePointCount(0, text.length());
    }

    /** The character index {@code position} as a Java offset, clamped into the string. */
    static int offset(String text, int position) {
        int length = length(text);
        int clamped = Math.max(0, Math.min(position, length));
        return text.offsetByCodePoints(0, clamped);
    }

    /**
     * PHP's {@code substr}: a negative start counts back from the end, and a negative length stops that many characters
     * from the end.
     */
    static String substring(String text, long start, Long length) {
        int total = length(text);
        int from = (int) (start < 0 ? Math.max(0, total + start) : Math.min(start, total));
        int to = total;
        if (length != null) {
            to = length < 0 ? (int) Math.max(from, total + length) : (int) Math.min(total, from + length);
        }
        return text.substring(offset(text, from), offset(text, to));
    }

    /** The character index of {@code needle}, or -1. */
    static int indexOf(String text, String needle, int from) {
        int at = text.indexOf(needle, offset(text, from));
        return at < 0 ? -1 : text.codePointCount(0, at);
    }

    static String repeat(String text, long times) {
        return times <= 0 ? "" : text.repeat((int) times);
    }
}
