package com.druvu.web.php.internal.value;

import java.util.Objects;

/**
 * A PHP string.
 *
 * <p>Extended by {@link SafeString}, which marks a string as already fit for HTML output. Safety is a property of how a
 * string reaches the response, not of the string's value, so it takes no part in equality or comparison: a
 * {@code SafeString} and a plain {@code PhpString} with the same characters are the same value.
 *
 * @author Deniss Larka
 */
public sealed class PhpString extends PhpValue permits SafeString {

    private final String value;

    /**
     * Assigns without validating on purpose. The class is extensible, and a constructor that can throw in a non-final
     * class is a finalizer-attack surface (SpotBugs {@code CT_CONSTRUCTOR_THROW}). Both factories reject null before
     * the constructor is ever entered.
     */
    PhpString(String value) {
        this.value = value;
    }

    public static PhpString of(String value) {
        return new PhpString(Objects.requireNonNull(value, "value"));
    }

    public final String value() {
        return value;
    }

    /** Whether this string may be written to the response without HTML escaping. */
    public boolean isSafe() {
        return false;
    }

    @Override
    public final String typeName() {
        return "string";
    }

    /** PHP's only falsy strings are the empty string and {@code "0"}. */
    @Override
    public final boolean isTruthy() {
        return !value.isEmpty() && !"0".equals(value);
    }

    @Override
    public final String toStr() {
        return value;
    }

    @Override
    public final long toInt() {
        return NumericStrings.toLong(value);
    }

    @Override
    public final double toFloat() {
        return NumericStrings.toDouble(value);
    }

    @Override
    public final ArrayKey toKey() {
        return ArrayKey.of(value);
    }

    @Override
    public final boolean equals(Object other) {
        return other instanceof PhpString that && value.equals(that.value);
    }

    @Override
    public final int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return "'" + value + "'";
    }
}
