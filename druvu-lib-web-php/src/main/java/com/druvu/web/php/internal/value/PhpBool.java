package com.druvu.web.php.internal.value;

/**
 * PHP's {@code true} and {@code false}. Two singletons: there is no third boolean.
 *
 * @author Deniss Larka
 */
public final class PhpBool extends PhpValue {

    /** PHP's {@code true}. */
    public static final PhpBool TRUE = new PhpBool(true);

    /** PHP's {@code false}. */
    public static final PhpBool FALSE = new PhpBool(false);

    private final boolean value;

    private PhpBool(boolean value) {
        this.value = value;
    }

    public static PhpBool of(boolean value) {
        return value ? TRUE : FALSE;
    }

    public boolean value() {
        return value;
    }

    @Override
    public String typeName() {
        return "boolean";
    }

    @Override
    public boolean isTruthy() {
        return value;
    }

    /** PHP prints {@code true} as {@code "1"} and {@code false} as the empty string. */
    @Override
    public String toStr() {
        return value ? "1" : "";
    }

    @Override
    public long toInt() {
        return value ? 1L : 0L;
    }

    @Override
    public double toFloat() {
        return value ? 1.0 : 0.0;
    }

    @Override
    public ArrayKey toKey() {
        return new ArrayKey.IntKey(value ? 1L : 0L);
    }

    @Override
    public String toString() {
        return value ? "true" : "false";
    }
}
