package com.druvu.web.php.internal.value;

/**
 * A PHP float, which is an IEEE 754 double.
 *
 * @author Deniss Larka
 */
public final class PhpFloat extends PhpValue {

    private final double value;

    private PhpFloat(double value) {
        this.value = value;
    }

    public static PhpFloat of(double value) {
        return new PhpFloat(value);
    }

    public double value() {
        return value;
    }

    @Override
    public String typeName() {
        return "double";
    }

    /** Only positive and negative zero are falsy; NAN is truthy. */
    @Override
    public boolean isTruthy() {
        return !PhpFloats.sameValue(value, 0.0);
    }

    @Override
    public String toStr() {
        return PhpFloats.toPhpString(value);
    }

    @Override
    public long toInt() {
        return PhpFloats.toInt(value);
    }

    @Override
    public double toFloat() {
        return value;
    }

    /** PHP truncates a float key towards zero. */
    @Override
    public ArrayKey toKey() {
        return new ArrayKey.IntKey(PhpFloats.toInt(value));
    }

    /**
     * Java's equality contract, not PHP's: NAN equals NAN here so that the hash contract holds. PHP's {@code ===} lives
     * in {@link PhpComparison#identical}.
     */
    @Override
    public boolean equals(Object other) {
        return other instanceof PhpFloat that && Double.compare(value, that.value) == 0;
    }

    @Override
    public int hashCode() {
        return Double.hashCode(value);
    }

    @Override
    public String toString() {
        return PhpFloats.toPhpString(value);
    }
}
