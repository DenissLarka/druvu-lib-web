package com.druvu.web.php.internal.value;

/**
 * A PHP integer. PHP's integer is platform-word-sized; on any machine this engine runs on that is 64 bits, so it maps
 * onto a Java {@code long}.
 *
 * @author Deniss Larka
 */
public final class PhpInt extends PhpValue {

    private final long value;

    private PhpInt(long value) {
        this.value = value;
    }

    public static PhpInt of(long value) {
        return new PhpInt(value);
    }

    public long value() {
        return value;
    }

    @Override
    public String typeName() {
        return "integer";
    }

    @Override
    public boolean isTruthy() {
        return value != 0L;
    }

    @Override
    public String toStr() {
        return Long.toString(value);
    }

    @Override
    public long toInt() {
        return value;
    }

    @Override
    public double toFloat() {
        return (double) value;
    }

    @Override
    public ArrayKey toKey() {
        return new ArrayKey.IntKey(value);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof PhpInt that && value == that.value;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(value);
    }

    @Override
    public String toString() {
        return Long.toString(value);
    }
}
