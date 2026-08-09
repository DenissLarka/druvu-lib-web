package com.druvu.web.php.internal.value;

import java.util.Objects;

/**
 * A normalised PHP array key. PHP has exactly two kinds, integer and string, and normalises everything else into one of
 * them. Doing that in a single place is what keeps {@code $a[1]}, {@code $a["1"]} and {@code $a[true]} agreeing with
 * each other.
 *
 * @author Deniss Larka
 */
public sealed interface ArrayKey {

    /** An integer key. */
    record IntKey(long value) implements ArrayKey {}

    /** A string key. */
    record StringKey(String value) implements ArrayKey {
        public StringKey {
            Objects.requireNonNull(value, "value");
        }
    }

    static ArrayKey of(long value) {
        return new IntKey(value);
    }

    /**
     * Normalises a string key. A string that spells a canonical decimal integer becomes an integer key, so
     * {@code $a["8"]} and {@code $a[8]} are the same slot. Anything else stays a string key: {@code "08"},
     * {@code "-0"}, {@code "+1"}, {@code "1.5"}, {@code " 1"} and values too large for an integer all keep their string
     * form, exactly as PHP does.
     */
    static ArrayKey of(String value) {
        return isCanonicalInteger(value) ? new IntKey(Long.parseLong(value)) : new StringKey(value);
    }

    private static boolean isCanonicalInteger(String value) {
        int start = value.startsWith("-") ? 1 : 0;
        if (value.length() == start) {
            return false;
        }
        for (int i = start; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        boolean paddedWithZero = value.charAt(start) == '0' && value.length() > start + 1;
        boolean negativeZero = start == 1 && value.charAt(start) == '0';
        if (paddedWithZero || negativeZero) {
            return false;
        }
        try {
            Long.parseLong(value);
            return true;
        } catch (NumberFormatException tooLargeForAnInteger) {
            return false;
        }
    }
}
