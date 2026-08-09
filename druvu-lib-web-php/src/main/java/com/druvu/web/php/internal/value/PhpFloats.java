package com.druvu.web.php.internal.value;

import java.util.Locale;

/**
 * The two float behaviours PHP does not share with Java: how a float is printed, and what {@code ===} means for one.
 *
 * @author Deniss Larka
 */
public final class PhpFloats {

    /** PHP's default {@code precision} ini setting, which is what {@code echo} formats a float with. */
    private static final int PRECISION = 14;

    private PhpFloats() {}

    /**
     * PHP's {@code ===} for floats: NAN is equal to nothing at all, and negative zero equals positive zero. Written
     * without {@code ==} on doubles so that it says what it means and no static analyser has to guess.
     */
    public static boolean sameValue(double left, double right) {
        return left <= right && left >= right;
    }

    /**
     * The {@code (int)} cast of a float. PHP truncates towards zero and yields 0 for NAN and the infinities; values
     * beyond the integer range are undefined in PHP and saturate here.
     */
    public static long toInt(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0L;
        }
        return (long) value;
    }

    /**
     * The string PHP's {@code echo} produces for a float: 14 significant digits, trailing zeros dropped, and an
     * exponent only when the number is too large or too small to write out.
     *
     * <p>This is C's {@code %.14G} with PHP's two additions — an exponential result always shows a fractional part
     * ({@code 1.0E+25}, never {@code 1E+25}) and the exponent carries no leading zeros ({@code 1.0E-5}, not
     * {@code 1.0E-05}). Ties at the fourteenth digit round half-up rather than half-even, which is Java's formatter
     * rather than C's; nothing a layout renders can tell the difference.
     */
    public static String toPhpString(double value) {
        if (Double.isNaN(value)) {
            return "NAN";
        }
        if (Double.isInfinite(value)) {
            return value > 0 ? "INF" : "-INF";
        }
        if (sameValue(value, 0.0)) {
            return isNegative(value) ? "-0" : "0";
        }

        String scientific = String.format(Locale.ROOT, "%." + (PRECISION - 1) + "E", value);
        int exponentAt = scientific.indexOf('E');
        int exponent = Integer.parseInt(scientific.substring(exponentAt + 1));

        if (exponent < -4 || exponent >= PRECISION) {
            String mantissa = dropTrailingZeros(scientific.substring(0, exponentAt));
            if (mantissa.indexOf('.') < 0) {
                mantissa += ".0";
            }
            return mantissa + "E" + (exponent < 0 ? "-" : "+") + Math.abs(exponent);
        }
        return dropTrailingZeros(String.format(Locale.ROOT, "%." + (PRECISION - 1 - exponent) + "f", value));
    }

    /** True for negative zero as well as for genuinely negative numbers. */
    private static boolean isNegative(double value) {
        return Double.doubleToRawLongBits(value) < 0L;
    }

    private static String dropTrailingZeros(String text) {
        if (text.indexOf('.') < 0) {
            return text;
        }
        int end = text.length();
        while (end > 0 && text.charAt(end - 1) == '0') {
            end--;
        }
        if (end > 0 && text.charAt(end - 1) == '.') {
            end--;
        }
        return text.substring(0, end);
    }
}
