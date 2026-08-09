package com.druvu.web.php.internal.builtin;

import com.druvu.web.php.internal.PhpProcessingException;
import com.druvu.web.php.internal.value.PhpValue;
import java.util.List;
import java.util.Locale;

/**
 * PHP's {@code sprintf}.
 *
 * <p>Not Java's: the conversions overlap but the flags do not, PHP numbers its arguments from one, and its
 * {@code %'x10d} pads with whatever character follows the apostrophe. Written out rather than mapped onto
 * {@link String#format} because the mapping is where the surprises would live.
 *
 * @author Deniss Larka
 */
final class Sprintf {

    private Sprintf() {}

    static String format(String pattern, List<PhpValue> arguments) {
        StringBuilder out = new StringBuilder();
        int next = 0;
        int at = 0;
        while (at < pattern.length()) {
            char c = pattern.charAt(at++);
            if (c != '%') {
                out.append(c);
                continue;
            }
            if (at < pattern.length() && pattern.charAt(at) == '%') {
                out.append('%');
                at++;
                continue;
            }
            Directive directive = new Directive(pattern, at);
            at = directive.end;
            int index = directive.argument > 0 ? directive.argument - 1 : next++;
            if (index >= arguments.size()) {
                throw new PhpProcessingException("sprintf(): too few arguments for " + pattern);
            }
            out.append(directive.render(arguments.get(index)));
        }
        return out.toString();
    }

    /** One {@code %...} in the pattern, read once and then able to render a value. */
    private static final class Directive {

        private final int argument;
        private final boolean leftAlign;
        private final boolean alwaysSign;
        private final char pad;
        private final int width;
        private final int precision;
        private final char conversion;
        private final int end;

        private Directive(String pattern, int from) {
            int at = from;

            int argument = 0;
            int digitsStart = at;
            while (at < pattern.length() && isDigit(pattern.charAt(at))) {
                at++;
            }
            if (at < pattern.length() && pattern.charAt(at) == '$' && at > digitsStart) {
                argument = Integer.parseInt(pattern.substring(digitsStart, at));
                at++;
            } else {
                at = digitsStart;
            }
            this.argument = argument;

            boolean leftAlign = false;
            boolean alwaysSign = false;
            char pad = ' ';
            boolean reading = true;
            while (reading && at < pattern.length()) {
                switch (pattern.charAt(at)) {
                    case '-' -> {
                        leftAlign = true;
                        at++;
                    }
                    case '+' -> {
                        alwaysSign = true;
                        at++;
                    }
                    case '0' -> {
                        pad = '0';
                        at++;
                    }
                    case ' ' -> {
                        pad = ' ';
                        at++;
                    }
                    case '\'' -> {
                        pad = pattern.charAt(at + 1);
                        at += 2;
                    }
                    default -> reading = false;
                }
            }
            this.leftAlign = leftAlign;
            this.alwaysSign = alwaysSign;
            this.pad = pad;

            int width = 0;
            while (at < pattern.length() && isDigit(pattern.charAt(at))) {
                width = width * 10 + pattern.charAt(at++) - '0';
            }
            this.width = width;

            int precision = -1;
            if (at < pattern.length() && pattern.charAt(at) == '.') {
                at++;
                precision = 0;
                while (at < pattern.length() && isDigit(pattern.charAt(at))) {
                    precision = precision * 10 + pattern.charAt(at++) - '0';
                }
            }
            this.precision = precision;

            if (at >= pattern.length()) {
                throw new PhpProcessingException("sprintf(): the pattern ends in the middle of a conversion");
            }
            this.conversion = pattern.charAt(at++);
            this.end = at;
        }

        private String render(PhpValue value) {
            String body = convert(value);
            if (body.length() >= width) {
                return body;
            }
            String padding = String.valueOf(pad).repeat(width - body.length());
            if (leftAlign) {
                return body + padding;
            }
            // A zero-padded negative number keeps its sign in front of the zeros.
            if (pad == '0' && !body.isEmpty() && (body.charAt(0) == '-' || body.charAt(0) == '+')) {
                return body.charAt(0) + padding + body.substring(1);
            }
            return padding + body;
        }

        private String convert(PhpValue value) {
            return switch (conversion) {
                case 's' -> precision >= 0 ? Text.substring(value.toStr(), 0, (long) precision) : value.toStr();
                case 'd', 'i' -> signed(Long.toString(Math.abs(value.toInt())), value.toInt() < 0);
                case 'u' -> Long.toUnsignedString(value.toInt());
                case 'f', 'F' ->
                    signed(
                            String.format(
                                    Locale.ROOT,
                                    "%." + (precision < 0 ? 6 : precision) + "f",
                                    Math.abs(value.toFloat())),
                            value.toFloat() < 0);
                case 'e', 'E' -> {
                    String formatted = String.format(
                            Locale.ROOT,
                            "%." + (precision < 0 ? 6 : precision) + (conversion == 'e' ? "e" : "E"),
                            value.toFloat());
                    yield formatted.replaceAll("([eE][+-])0(\\d)$", "$1$2");
                }
                case 'b' -> Long.toBinaryString(value.toInt());
                case 'o' -> Long.toOctalString(value.toInt());
                case 'x' -> Long.toHexString(value.toInt());
                case 'X' -> Long.toHexString(value.toInt()).toUpperCase(Locale.ROOT);
                case 'c' -> String.valueOf((char) value.toInt());
                default -> throw new PhpProcessingException("sprintf(): unknown conversion %" + conversion);
            };
        }

        private String signed(String digits, boolean negative) {
            if (negative) {
                return "-" + digits;
            }
            return alwaysSign ? "+" + digits : digits;
        }

        private static boolean isDigit(char c) {
            return c >= '0' && c <= '9';
        }
    }
}
