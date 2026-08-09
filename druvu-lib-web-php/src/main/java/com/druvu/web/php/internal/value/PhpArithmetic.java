package com.druvu.web.php.internal.value;

import com.druvu.web.php.internal.PhpProcessingException;
import java.util.Map;

/**
 * PHP's arithmetic, which is not Java's.
 *
 * <p>Three things are easy to get wrong and are therefore written out here rather than spread across operator nodes:
 * {@code /} yields an integer when the division comes out even and a float otherwise; {@code %} works on integers and
 * takes the sign of its left operand; and {@code +} on two arrays is a union, not addition.
 *
 * <p>A string that is not a number at all is refused rather than treated as zero — PHP 8 raises a TypeError for it, and
 * a template that adds 1 to a name has a bug worth hearing about. A string that merely <em>starts</em> with a number is
 * accepted, as PHP accepts it.
 *
 * @author Deniss Larka
 */
public final class PhpArithmetic {

    private PhpArithmetic() {}

    /** Addition, or — when both operands are arrays — PHP's array union, where the left side keeps its keys. */
    public static PhpValue add(PhpValue left, PhpValue right) {
        if (left instanceof PhpArray a && right instanceof PhpArray b) {
            return union(a, b);
        }
        PhpValue x = toNumber(left, "+", right);
        PhpValue y = toNumber(right, "+", left);
        if (x instanceof PhpInt a && y instanceof PhpInt b) {
            return PhpInt.of(a.value() + b.value());
        }
        return PhpFloat.of(x.toFloat() + y.toFloat());
    }

    public static PhpValue subtract(PhpValue left, PhpValue right) {
        PhpValue x = toNumber(left, "-", right);
        PhpValue y = toNumber(right, "-", left);
        if (x instanceof PhpInt a && y instanceof PhpInt b) {
            return PhpInt.of(a.value() - b.value());
        }
        return PhpFloat.of(x.toFloat() - y.toFloat());
    }

    public static PhpValue multiply(PhpValue left, PhpValue right) {
        PhpValue x = toNumber(left, "*", right);
        PhpValue y = toNumber(right, "*", left);
        if (x instanceof PhpInt a && y instanceof PhpInt b) {
            return PhpInt.of(a.value() * b.value());
        }
        return PhpFloat.of(x.toFloat() * y.toFloat());
    }

    /** Integer when both operands are integers and the division is exact; a float in every other case. */
    public static PhpValue divide(PhpValue left, PhpValue right) {
        PhpValue x = toNumber(left, "/", right);
        PhpValue y = toNumber(right, "/", left);
        if (PhpFloats.sameValue(y.toFloat(), 0.0)) {
            throw new PhpProcessingException("Division by zero");
        }
        if (x instanceof PhpInt a && y instanceof PhpInt b && a.value() % b.value() == 0L) {
            return PhpInt.of(a.value() / b.value());
        }
        return PhpFloat.of(x.toFloat() / y.toFloat());
    }

    /** Integer remainder taking the sign of the left operand, which is what Java's {@code %} already does. */
    public static PhpValue modulo(PhpValue left, PhpValue right) {
        long divisor = toNumber(right, "%", left).toInt();
        if (divisor == 0L) {
            throw new PhpProcessingException("Modulo by zero");
        }
        return PhpInt.of(toNumber(left, "%", right).toInt() % divisor);
    }

    /** Integer when both operands are integers, the exponent is not negative, and the result still fits. */
    public static PhpValue power(PhpValue left, PhpValue right) {
        PhpValue base = toNumber(left, "**", right);
        PhpValue exponent = toNumber(right, "**", left);
        double result = Math.pow(base.toFloat(), exponent.toFloat());
        if (base instanceof PhpInt && exponent instanceof PhpInt e && e.value() >= 0 && isExactlyAnInteger(result)) {
            return PhpInt.of((long) result);
        }
        return PhpFloat.of(result);
    }

    public static PhpValue negate(PhpValue value) {
        return multiply(value, PhpInt.of(-1L));
    }

    /** Unary {@code +}, which is not a no-op: it converts its operand to a number. */
    public static PhpValue identity(PhpValue value) {
        return multiply(value, PhpInt.of(1L));
    }

    /** Concatenation. Safety never survives it, so the result is a plain string even next to a {@link SafeString}. */
    public static PhpString concat(PhpValue left, PhpValue right) {
        return PhpString.of(left.toStr() + right.toStr());
    }

    /** {@code $a + $b} on arrays: every entry of the left, plus the right's entries whose keys the left lacks. */
    private static PhpArray union(PhpArray left, PhpArray right) {
        PhpArray result = left.copy();
        for (Map.Entry<ArrayKey, PhpValue> entry : right.entries().entrySet()) {
            if (!result.containsKey(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    private static boolean isExactlyAnInteger(double result) {
        return !Double.isNaN(result)
                && !Double.isInfinite(result)
                && PhpFloats.sameValue(result, Math.rint(result))
                && result >= (double) Long.MIN_VALUE
                && result <= (double) Long.MAX_VALUE;
    }

    /**
     * The number an operand contributes, or a refusal naming both types the way PHP names them.
     *
     * @param other the operand on the other side, needed only so the refusal can describe the whole expression
     */
    private static PhpValue toNumber(PhpValue value, String operator, PhpValue other) {
        if (value instanceof PhpInt || value instanceof PhpFloat) {
            return value;
        }
        if (value instanceof PhpNull) {
            return PhpInt.of(0L);
        }
        if (value instanceof PhpBool flag) {
            return PhpInt.of(flag.value() ? 1L : 0L);
        }
        if (value instanceof PhpString text && NumericStrings.startsWithNumber(text.value())) {
            return NumericStrings.toNumber(text.value());
        }
        throw new PhpProcessingException(
                "Unsupported operand types: " + value.typeName() + " " + operator + " " + other.typeName());
    }
}
