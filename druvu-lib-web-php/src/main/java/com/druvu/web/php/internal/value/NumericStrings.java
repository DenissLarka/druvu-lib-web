package com.druvu.web.php.internal.value;

/**
 * PHP's numeric-string rules, which two different questions depend on.
 *
 * <p>Comparison asks whether a string is numeric <em>as a whole</em> — that is what decides, in PHP 8, whether
 * {@code $x == 5} compares numbers or strings. Casting asks something weaker: what number the string <em>starts</em>
 * with, so {@code (int) "12abc"} is 12. Keeping both in one class keeps the scanner that answers them single.
 *
 * @author Deniss Larka
 */
public final class NumericStrings {

    private NumericStrings() {}

    /**
     * Whether the whole string is a numeric string. Leading whitespace has always been allowed; PHP 8.0 also allows
     * trailing whitespace.
     */
    public static boolean isNumeric(String text) {
        int start = skipWhitespace(text, 0);
        Scan scan = scan(text, start);
        return scan != null && skipWhitespace(text, scan.end()) == text.length();
    }

    /**
     * The number a numeric string denotes: an integer when it is written as one and fits, a float otherwise. Used by
     * comparison, which needs the number rather than a cast.
     */
    public static PhpValue toNumber(String text) {
        int start = skipWhitespace(text, 0);
        Scan scan = scan(text, start);
        if (scan == null) {
            return PhpInt.of(0L);
        }
        String number = text.substring(start, scan.end());
        if (!scan.floating()) {
            try {
                return PhpInt.of(Long.parseLong(number));
            } catch (NumberFormatException tooLargeForAnInteger) {
                return PhpFloat.of(Double.parseDouble(number));
            }
        }
        return PhpFloat.of(Double.parseDouble(number));
    }

    /**
     * Whether the string begins with a number at all. Arithmetic accepts {@code "5abc"} and refuses {@code "abc"}, and
     * this is the line between them.
     */
    public static boolean startsWithNumber(String text) {
        return scan(text, skipWhitespace(text, 0)) != null;
    }

    /** The {@code (int)} cast of a string: its leading number, or 0 when it does not start with one. */
    public static long toLong(String text) {
        int start = skipWhitespace(text, 0);
        Scan scan = scan(text, start);
        if (scan == null) {
            return 0L;
        }
        String number = text.substring(start, scan.end());
        if (!scan.floating()) {
            try {
                return Long.parseLong(number);
            } catch (NumberFormatException tooLargeForAnInteger) {
                return PhpFloats.toInt(Double.parseDouble(number));
            }
        }
        return PhpFloats.toInt(Double.parseDouble(number));
    }

    /** The {@code (float)} cast of a string: its leading number, or 0.0 when it does not start with one. */
    public static double toDouble(String text) {
        int start = skipWhitespace(text, 0);
        Scan scan = scan(text, start);
        return scan == null ? 0.0 : Double.parseDouble(text.substring(start, scan.end()));
    }

    /**
     * How far a number reaches from {@code from}, and whether it is written as a float. Null when nothing numeric
     * starts there.
     */
    private static Scan scan(String text, int from) {
        int at = from;
        if (at < text.length() && (text.charAt(at) == '+' || text.charAt(at) == '-')) {
            at++;
        }

        int wholeDigits = countDigits(text, at);
        at += wholeDigits;

        boolean floating = false;
        if (at < text.length() && text.charAt(at) == '.') {
            int fractionDigits = countDigits(text, at + 1);
            if (wholeDigits > 0 || fractionDigits > 0) {
                floating = true;
                at += 1 + fractionDigits;
            }
        }
        if (wholeDigits == 0 && !floating) {
            return null;
        }

        if (at < text.length() && (text.charAt(at) == 'e' || text.charAt(at) == 'E')) {
            int afterE = at + 1;
            if (afterE < text.length() && (text.charAt(afterE) == '+' || text.charAt(afterE) == '-')) {
                afterE++;
            }
            int exponentDigits = countDigits(text, afterE);
            if (exponentDigits > 0) {
                floating = true;
                at = afterE + exponentDigits;
            }
        }
        return new Scan(at, floating);
    }

    private static int countDigits(String text, int from) {
        int at = from;
        while (at < text.length() && text.charAt(at) >= '0' && text.charAt(at) <= '9') {
            at++;
        }
        return at - from;
    }

    private static int skipWhitespace(String text, int from) {
        int at = from;
        while (at < text.length() && isWhitespace(text.charAt(at))) {
            at++;
        }
        return at;
    }

    /** What PHP's own scanner skips: space, tab, newline, carriage return, vertical tab and form feed. */
    private static boolean isWhitespace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\u000B' || c == '\f';
    }

    private record Scan(int end, boolean floating) {}
}
