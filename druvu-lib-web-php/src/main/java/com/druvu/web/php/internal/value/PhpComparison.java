package com.druvu.web.php.internal.value;

import java.util.Iterator;
import java.util.Map;

/**
 * PHP 8's comparison rules, in one place.
 *
 * <p>Conversion is a property of a single value and lives on the value itself. Comparison is not: what {@code $a == $b}
 * means depends on both operands together, and PHP answers it from a table whose <em>row order is load-bearing</em>.
 * Splitting that table across seven classes would hide the one thing about it worth seeing, so it is written out here
 * as one readable sequence of rules.
 *
 * <p>PHP 8 is the baseline, which matters most for numbers against strings: {@code 0 == "foo"} was true in PHP 7 and is
 * false in PHP 8, because a non-numeric string is now compared <em>as a string</em> rather than converted to 0.
 *
 * @author Deniss Larka
 */
public final class PhpComparison {

    /** What {@link #order} reports when two operands cannot be ordered at all. Only NAN causes it. */
    private static final int UNORDERED = Integer.MIN_VALUE;

    private PhpComparison() {}

    /** PHP's {@code ===}: same type and same value, with no conversion of any kind. */
    public static boolean identical(PhpValue left, PhpValue right) {
        if (left instanceof PhpNull) {
            return right instanceof PhpNull;
        }
        if (left instanceof PhpBool a) {
            return right instanceof PhpBool b && a.value() == b.value();
        }
        if (left instanceof PhpInt a) {
            return right instanceof PhpInt b && a.value() == b.value();
        }
        if (left instanceof PhpFloat a) {
            return right instanceof PhpFloat b && PhpFloats.sameValue(a.value(), b.value());
        }
        if (left instanceof PhpString a) {
            return right instanceof PhpString b && a.value().equals(b.value());
        }
        if (left instanceof PhpArray a) {
            return right instanceof PhpArray b && identicalArrays(a, b);
        }
        // A closure is an object, and an object is identical only to itself.
        return left == right;
    }

    /** PHP's {@code ==}. */
    public static boolean looseEquals(PhpValue left, PhpValue right) {
        return order(left, right) == 0;
    }

    /**
     * PHP's {@code <=>}: negative when the left operand is smaller, positive when it is larger, zero when they are
     * loosely equal.
     *
     * <p>NAN can be ordered against nothing, not even itself — yet PHP's {@code <=>} still answers 1 for it, in either
     * direction, so this does too. The ordering operators below do not: they follow PHP in being false throughout.
     */
    public static int compare(PhpValue left, PhpValue right) {
        int order = order(left, right);
        return order == UNORDERED ? 1 : order;
    }

    /** PHP's {@code <}. False whenever the operands cannot be ordered. */
    public static boolean lessThan(PhpValue left, PhpValue right) {
        int order = order(left, right);
        return order != UNORDERED && order < 0;
    }

    /** PHP's {@code <=}. */
    public static boolean lessOrEqual(PhpValue left, PhpValue right) {
        int order = order(left, right);
        return order != UNORDERED && order <= 0;
    }

    /** PHP's {@code >}. */
    public static boolean greaterThan(PhpValue left, PhpValue right) {
        int order = order(left, right);
        return order != UNORDERED && order > 0;
    }

    /** PHP's {@code >=}. */
    public static boolean greaterOrEqual(PhpValue left, PhpValue right) {
        int order = order(left, right);
        return order != UNORDERED && order >= 0;
    }

    private static int order(PhpValue left, PhpValue right) {
        // The order below is PHP 8's comparison table and it matters. Null against a string is decided before the
        // boolean rule that would otherwise swallow it, which is why null == "0" is false but null == 0 is true.
        if (left instanceof PhpNull && right instanceof PhpString text) {
            return compareStrings("", text.value());
        }
        if (left instanceof PhpString text && right instanceof PhpNull) {
            return compareStrings(text.value(), "");
        }
        if (isBooleanLike(left) || isBooleanLike(right)) {
            return Boolean.compare(left.isTruthy(), right.isTruthy());
        }
        if (left instanceof PhpArray a && right instanceof PhpArray b) {
            return compareArrays(a, b);
        }
        if (left instanceof PhpArray) {
            return 1;
        }
        if (right instanceof PhpArray) {
            return -1;
        }
        if (isObject(left) || isObject(right)) {
            // Two objects are equal only when they are the same object; against anything else the object wins.
            if (isObject(left) && isObject(right)) {
                return left == right ? 0 : 1;
            }
            return isObject(left) ? 1 : -1;
        }
        return compareScalars(left, right);
    }

    private static boolean isObject(PhpValue value) {
        return value instanceof PhpClosure || value instanceof PhpObject;
    }

    /** Null and booleans drag whatever they are compared with down to a boolean. */
    private static boolean isBooleanLike(PhpValue value) {
        return value instanceof PhpBool || value instanceof PhpNull;
    }

    /** Both operands are an integer, a float or a string here — everything else has already been decided. */
    private static int compareScalars(PhpValue left, PhpValue right) {
        if (left instanceof PhpString a && right instanceof PhpString b) {
            if (NumericStrings.isNumeric(a.value()) && NumericStrings.isNumeric(b.value())) {
                return compareNumbers(NumericStrings.toNumber(a.value()), NumericStrings.toNumber(b.value()));
            }
            return compareStrings(a.value(), b.value());
        }
        if (left instanceof PhpString a) {
            return NumericStrings.isNumeric(a.value())
                    ? compareNumbers(NumericStrings.toNumber(a.value()), right)
                    : compareStrings(a.value(), right.toStr());
        }
        if (right instanceof PhpString b) {
            return NumericStrings.isNumeric(b.value())
                    ? compareNumbers(left, NumericStrings.toNumber(b.value()))
                    : compareStrings(left.toStr(), b.value());
        }
        return compareNumbers(left, right);
    }

    private static int compareNumbers(PhpValue left, PhpValue right) {
        if (left instanceof PhpInt a && right instanceof PhpInt b) {
            return Long.compare(a.value(), b.value());
        }
        double a = left.toFloat();
        double b = right.toFloat();
        if (Double.isNaN(a) || Double.isNaN(b)) {
            return UNORDERED;
        }
        if (a < b) {
            return -1;
        }
        if (a > b) {
            return 1;
        }
        return 0;
    }

    /**
     * PHP compares non-numeric strings by content. This compares by UTF-16 order, which differs from PHP's byte order
     * only for text outside the basic multilingual plane and never changes whether two strings are equal.
     */
    private static int compareStrings(String left, String right) {
        return Integer.signum(left.compareTo(right));
    }

    /** Fewer entries is smaller; otherwise the first key whose values differ decides. */
    private static int compareArrays(PhpArray left, PhpArray right) {
        int bySize = Integer.compare(left.size(), right.size());
        if (bySize != 0) {
            return bySize;
        }
        for (Map.Entry<ArrayKey, PhpValue> entry : left.entries().entrySet()) {
            PhpValue other = right.get(entry.getKey());
            if (other == null) {
                // PHP calls arrays with differing keys uncomparable and reports the left one as greater.
                return 1;
            }
            int byValue = order(entry.getValue(), other);
            if (byValue != 0) {
                return byValue;
            }
        }
        return 0;
    }

    /** {@code ===} on arrays is order-sensitive, unlike {@code ==}. */
    private static boolean identicalArrays(PhpArray left, PhpArray right) {
        if (left.size() != right.size()) {
            return false;
        }
        Iterator<Map.Entry<ArrayKey, PhpValue>> mine = left.entries().entrySet().iterator();
        Iterator<Map.Entry<ArrayKey, PhpValue>> theirs =
                right.entries().entrySet().iterator();
        while (mine.hasNext()) {
            Map.Entry<ArrayKey, PhpValue> a = mine.next();
            Map.Entry<ArrayKey, PhpValue> b = theirs.next();
            if (!a.getKey().equals(b.getKey()) || !identical(a.getValue(), b.getValue())) {
                return false;
            }
        }
        return true;
    }
}
