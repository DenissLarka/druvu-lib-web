package com.druvu.web.php.internal.value;

/**
 * PHP's {@code null}. A singleton: there is only ever one of it.
 *
 * @author Deniss Larka
 */
public final class PhpNull extends PhpValue {

    /** The one and only null. */
    public static final PhpNull NULL = new PhpNull();

    private PhpNull() {}

    @Override
    public String typeName() {
        return "NULL";
    }

    @Override
    public boolean isTruthy() {
        return false;
    }

    @Override
    public String toStr() {
        return "";
    }

    @Override
    public long toInt() {
        return 0L;
    }

    @Override
    public double toFloat() {
        return 0.0;
    }

    /** PHP keys an array by the empty string when the key is null. */
    @Override
    public ArrayKey toKey() {
        return new ArrayKey.StringKey("");
    }

    @Override
    public String toString() {
        return "null";
    }
}
