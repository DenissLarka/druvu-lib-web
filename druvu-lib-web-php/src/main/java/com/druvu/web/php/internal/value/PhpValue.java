package com.druvu.web.php.internal.value;

/**
 * Base of the PHP value hierarchy. The engine's expressions evaluate to a {@code PhpValue}, never to a Java
 * {@code String}, so conditions, arithmetic and arrays all have somewhere to live.
 *
 * <p>Conversions are virtual methods rather than {@code instanceof} chains in the evaluator: each type knows how PHP
 * converts it. The two-operand rules — comparison and equality — cannot be expressed that way and live in
 * {@link PhpComparison}, which holds PHP 8's table in one auditable place.
 *
 * <p>Values are immutable except {@link PhpArray}, which PHP itself treats as a mutable container copied on assignment.
 *
 * @author Deniss Larka
 */
public abstract sealed class PhpValue
        permits PhpNull, PhpBool, PhpInt, PhpFloat, PhpString, PhpArray, PhpClosure, PhpObject {

    /** The type name {@code gettype()} reports for this value. */
    public abstract String typeName();

    /**
     * Truthiness. PHP's falsy set is exactly {@code null}, {@code false}, {@code 0}, {@code 0.0}, {@code ""},
     * {@code "0"} and the empty array; everything else is true.
     */
    public abstract boolean isTruthy();

    /**
     * String conversion as performed by {@code echo} and the {@code (string)} cast. Distinct from
     * {@link Object#toString()}, which stays a debugging representation.
     */
    public abstract String toStr();

    /** Integer conversion as performed by the {@code (int)} cast. */
    public abstract long toInt();

    /** Float conversion as performed by the {@code (float)} cast. */
    public abstract double toFloat();

    /** Array-key normalisation as performed by {@code $a[$this]}. */
    public abstract ArrayKey toKey();

    /**
     * The value to store when this one is assigned to a variable or an array element. PHP arrays are values, so
     * assigning one copies it; every other type is immutable and can be shared.
     */
    public PhpValue copyForAssignment() {
        return this;
    }

    /** Boolean conversion as performed by the {@code (bool)} cast. */
    public final PhpBool toBool() {
        return PhpBool.of(isTruthy());
    }

    /** PHP's {@code ==}. */
    public final boolean looseEquals(PhpValue other) {
        return PhpComparison.looseEquals(this, other);
    }

    /** PHP's {@code ===}. */
    public final boolean identical(PhpValue other) {
        return PhpComparison.identical(this, other);
    }

    /** PHP's {@code <=>}: negative, zero or positive. */
    public final int compare(PhpValue other) {
        return PhpComparison.compare(this, other);
    }
}
