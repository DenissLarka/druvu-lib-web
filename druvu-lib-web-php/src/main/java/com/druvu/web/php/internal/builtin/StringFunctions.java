package com.druvu.web.php.internal.builtin;

import com.druvu.web.php.internal.runtime.FunctionRegistry;
import com.druvu.web.php.internal.value.ArrayKey;
import com.druvu.web.php.internal.value.PhpArray;
import com.druvu.web.php.internal.value.PhpBool;
import com.druvu.web.php.internal.value.PhpInt;
import com.druvu.web.php.internal.value.PhpString;
import com.druvu.web.php.internal.value.PhpValue;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Everything that works on text.
 *
 * <p>Lengths and offsets count characters, not bytes — see {@link Text} for why — and the {@code mb_} spellings are
 * registered as aliases of the same implementations rather than as a parallel set that could drift.
 *
 * @author Deniss Larka
 */
final class StringFunctions {

    private StringFunctions() {}

    static void registerInto(FunctionRegistry registry) {
        Functions.define(registry, "strlen", 1, (env, a) -> PhpInt.of(Text.length(a.string(0))));
        Functions.define(
                registry,
                "substr",
                2,
                3,
                (env, a) -> PhpString.of(Text.substring(a.string(0), a.integer(1), a.has(2) ? a.integer(2) : null)));
        Functions.define(registry, "strpos", 2, 3, (env, a) -> {
            int at = Text.indexOf(a.string(0), a.string(1), (int) a.integerOr(2, 0));
            return at < 0 ? PhpBool.FALSE : PhpInt.of(at);
        });
        Functions.define(
                registry, "str_contains", 2, (env, a) -> PhpBool.of(a.string(0).contains(a.string(1))));
        Functions.define(
                registry,
                "str_starts_with",
                2,
                (env, a) -> PhpBool.of(a.string(0).startsWith(a.string(1))));
        Functions.define(
                registry, "str_ends_with", 2, (env, a) -> PhpBool.of(a.string(0).endsWith(a.string(1))));

        Functions.define(registry, "str_replace", 3, (env, a) -> PhpString.of(replace(a)));
        Functions.define(registry, "str_repeat", 2, (env, a) -> PhpString.of(Text.repeat(a.string(0), a.integer(1))));

        Functions.define(registry, "trim", 1, 2, (env, a) -> PhpString.of(trim(a.string(0), cut(a), true, true)));
        Functions.define(registry, "ltrim", 1, 2, (env, a) -> PhpString.of(trim(a.string(0), cut(a), true, false)));
        Functions.define(registry, "rtrim", 1, 2, (env, a) -> PhpString.of(trim(a.string(0), cut(a), false, true)));

        Functions.define(
                registry, "strtolower", 1, (env, a) -> PhpString.of(a.string(0).toLowerCase(Locale.ROOT)));
        Functions.define(
                registry, "strtoupper", 1, (env, a) -> PhpString.of(a.string(0).toUpperCase(Locale.ROOT)));
        Functions.define(registry, "ucfirst", 1, (env, a) -> PhpString.of(changeFirst(a.string(0), true)));
        Functions.define(registry, "lcfirst", 1, (env, a) -> PhpString.of(changeFirst(a.string(0), false)));
        Functions.define(
                registry, "ucwords", 1, 2, (env, a) -> PhpString.of(ucwords(a.string(0), a.stringOr(1, " \t\r\n\f"))));

        Functions.define(
                registry,
                "sprintf",
                1,
                Integer.MAX_VALUE,
                (env, a) -> PhpString.of(Sprintf.format(a.string(0), a.from(1))));
        Functions.define(
                registry,
                "vsprintf",
                2,
                (env, a) -> PhpString.of(Sprintf.format(
                        a.string(0), List.copyOf(a.array(1).entries().values()))));
        Functions.define(registry, "printf", 1, Integer.MAX_VALUE, (env, a) -> {
            String text = Sprintf.format(a.string(0), a.from(1));
            com.druvu.web.php.internal.runtime.Output.write(env, PhpString.of(text));
            return PhpInt.of(Text.length(text));
        });

        Functions.define(
                registry,
                "number_format",
                1,
                4,
                (env, a) -> PhpString.of(
                        numberFormat(a.number(0), (int) a.integerOr(1, 0), a.stringOr(2, "."), a.stringOr(3, ","))));
        Functions.define(
                registry,
                "str_pad",
                2,
                4,
                (env, a) -> PhpString.of(
                        pad(a.string(0), (int) a.integer(1), a.stringOr(2, " "), (int) a.integerOr(3, 1))));

        Functions.define(registry, "implode", 1, 2, (env, a) -> PhpString.of(implode(a)));
        Functions.alias(registry, "implode", "join");
        Functions.define(
                registry,
                "explode",
                2,
                3,
                (env, a) -> explode(a.string(0), a.string(1), a.integerOr(2, Long.MAX_VALUE)));
        Functions.define(registry, "str_split", 1, 2, (env, a) -> split(a.string(0), (int) a.integerOr(1, 1)));
        Functions.define(
                registry,
                "wordwrap",
                1,
                4,
                (env, a) ->
                        PhpString.of(wordwrap(a.string(0), (int) a.integerOr(1, 75), a.stringOr(2, "\n"), a.flag(3))));

        // Same implementations, PHP's multibyte spellings.
        Functions.alias(registry, "strlen", "mb_strlen");
        Functions.alias(registry, "substr", "mb_substr");
        Functions.alias(registry, "strpos", "mb_strpos");
        Functions.alias(registry, "strtolower", "mb_strtolower");
        Functions.alias(registry, "strtoupper", "mb_strtoupper");
        Functions.alias(registry, "str_split", "mb_str_split");
    }

    private static String cut(Arguments a) {
        return a.stringOr(1, " \t\n\r\0" + (char) 0x0B);
    }

    private static String trim(String text, String cutSet, boolean fromStart, boolean fromEnd) {
        int start = 0;
        int end = text.length();
        while (fromStart && start < end && cutSet.indexOf(text.charAt(start)) >= 0) {
            start++;
        }
        while (fromEnd && end > start && cutSet.indexOf(text.charAt(end - 1)) >= 0) {
            end--;
        }
        return text.substring(start, end);
    }

    /** {@code str_replace} takes arrays on either side; the shorter replacement list runs out into empty strings. */
    private static String replace(Arguments a) {
        List<String> search = asStrings(a.at(0));
        List<String> replace = asStrings(a.at(1));
        String subject = a.string(2);
        for (int i = 0; i < search.size(); i++) {
            String with = a.at(1) instanceof PhpArray ? (i < replace.size() ? replace.get(i) : "") : replace.get(0);
            subject = subject.replace(search.get(i), with);
        }
        return subject;
    }

    private static List<String> asStrings(PhpValue value) {
        if (!(value instanceof PhpArray array)) {
            return List.of(value.toStr());
        }
        List<String> strings = new ArrayList<>();
        for (PhpValue each : array.entries().values()) {
            strings.add(each.toStr());
        }
        return strings;
    }

    private static String changeFirst(String text, boolean upper) {
        if (text.isEmpty()) {
            return text;
        }
        int first = text.codePointAt(0);
        int changed = upper ? Character.toUpperCase(first) : Character.toLowerCase(first);
        return new String(Character.toChars(changed)) + text.substring(Character.charCount(first));
    }

    private static String ucwords(String text, String delimiters) {
        StringBuilder out = new StringBuilder(text);
        boolean atWordStart = true;
        for (int i = 0; i < out.length(); i++) {
            if (atWordStart) {
                out.setCharAt(i, Character.toUpperCase(out.charAt(i)));
            }
            atWordStart = delimiters.indexOf(out.charAt(i)) >= 0;
        }
        return out.toString();
    }

    private static String numberFormat(double value, int decimals, String point, String thousands) {
        BigDecimal rounded = BigDecimal.valueOf(value).setScale(decimals, RoundingMode.HALF_UP);
        String digits = rounded.abs().toPlainString();
        String whole = decimals > 0 ? digits.substring(0, digits.indexOf('.')) : digits;
        String fraction = decimals > 0 ? digits.substring(digits.indexOf('.') + 1) : "";

        StringBuilder grouped = new StringBuilder();
        for (int i = 0; i < whole.length(); i++) {
            if (i > 0 && (whole.length() - i) % 3 == 0) {
                grouped.append(thousands);
            }
            grouped.append(whole.charAt(i));
        }
        String sign = rounded.signum() < 0 ? "-" : "";
        return sign + grouped + (decimals > 0 ? point + fraction : "");
    }

    /** {@code STR_PAD_LEFT} is 0, {@code STR_PAD_RIGHT} is 1 and {@code STR_PAD_BOTH} is 2, as PHP numbers them. */
    private static String pad(String text, int length, String with, int where) {
        int missing = length - Text.length(text);
        if (missing <= 0 || with.isEmpty()) {
            return text;
        }
        return switch (where) {
            case 0 -> padding(with, missing) + text;
            case 2 -> padding(with, missing / 2) + text + padding(with, missing - missing / 2);
            default -> text + padding(with, missing);
        };
    }

    private static String padding(String with, int length) {
        StringBuilder padded = new StringBuilder();
        while (padded.length() < length) {
            padded.append(with);
        }
        return padded.substring(0, length);
    }

    private static String implode(Arguments a) {
        boolean glueFirst = !(a.at(0) instanceof PhpArray);
        String glue = glueFirst ? a.string(0) : "";
        PhpArray array = glueFirst ? a.array(1) : a.array(0);
        StringBuilder joined = new StringBuilder();
        for (PhpValue value : array.entries().values()) {
            if (!joined.isEmpty()) {
                joined.append(glue);
            }
            joined.append(value.toStr());
        }
        return joined.toString();
    }

    private static PhpValue explode(String separator, String subject, long limit) {
        if (separator.isEmpty()) {
            throw new com.druvu.web.php.internal.PhpProcessingException("explode(): the separator cannot be empty");
        }
        PhpArray pieces = PhpArray.empty();
        int from = 0;
        while (pieces.size() + 1 < limit) {
            int at = subject.indexOf(separator, from);
            if (at < 0) {
                break;
            }
            pieces.append(PhpString.of(subject.substring(from, at)));
            from = at + separator.length();
        }
        pieces.append(PhpString.of(subject.substring(from)));
        return pieces;
    }

    private static PhpValue split(String text, int size) {
        PhpArray pieces = PhpArray.empty();
        int length = Text.length(text);
        if (length == 0) {
            pieces.append(PhpString.of(""));
            return pieces;
        }
        for (int at = 0; at < length; at += size) {
            pieces.append(PhpString.of(Text.substring(text, at, (long) size)));
        }
        return pieces;
    }

    private static String wordwrap(String text, int width, String lineBreak, boolean cutLongWords) {
        StringBuilder out = new StringBuilder();
        int lineLength = 0;
        for (String word : text.split(" ", -1)) {
            while (cutLongWords && Text.length(word) > width) {
                if (lineLength > 0) {
                    out.append(lineBreak);
                    lineLength = 0;
                }
                out.append(Text.substring(word, 0, (long) width)).append(lineBreak);
                word = Text.substring(word, width, null);
            }
            if (lineLength > 0 && lineLength + 1 + Text.length(word) > width) {
                out.append(lineBreak);
                lineLength = 0;
            } else if (lineLength > 0) {
                out.append(' ');
                lineLength++;
            }
            out.append(word);
            lineLength += Text.length(word);
        }
        return out.toString();
    }

    /** Used by the array functions when they need a key as a value. */
    static PhpValue keyAsValue(ArrayKey key) {
        return key instanceof ArrayKey.IntKey index
                ? PhpInt.of(index.value())
                : PhpString.of(((ArrayKey.StringKey) key).value());
    }

    /** Used by the array functions that copy entries across. */
    static void copyEntry(PhpArray into, Map.Entry<ArrayKey, PhpValue> entry, boolean keepKey) {
        if (keepKey) {
            into.put(entry.getKey(), entry.getValue());
        } else {
            into.append(entry.getValue());
        }
    }
}
