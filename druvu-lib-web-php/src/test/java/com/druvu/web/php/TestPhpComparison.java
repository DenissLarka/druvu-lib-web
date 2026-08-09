package com.druvu.web.php;

import com.druvu.web.php.internal.value.ArrayKey;
import com.druvu.web.php.internal.value.PhpArray;
import com.druvu.web.php.internal.value.PhpBool;
import com.druvu.web.php.internal.value.PhpFloat;
import com.druvu.web.php.internal.value.PhpInt;
import com.druvu.web.php.internal.value.PhpNull;
import com.druvu.web.php.internal.value.PhpString;
import com.druvu.web.php.internal.value.PhpValue;
import com.druvu.web.php.internal.value.SafeString;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * PHP 8's comparison table. Half-faithful juggling is worse than none, so the rows that changed between PHP 7 and PHP 8
 * — a number against a non-numeric string, and null against a string — are all here explicitly.
 */
public class TestPhpComparison {

    @DataProvider(name = "looseEquality")
    public Object[][] looseEquality() {
        return new Object[][] {
            // The PHP 8 change: a non-numeric string is compared as a string, not converted to 0.
            {PhpInt.of(0L), PhpString.of("foo"), false},
            {PhpInt.of(0L), PhpString.of(""), false},
            {PhpInt.of(0L), PhpString.of("0"), true},
            {PhpString.of("abc"), PhpInt.of(0L), false},
            {PhpInt.of(1L), PhpString.of("1"), true},
            {PhpInt.of(100L), PhpString.of("1e2"), true},
            {PhpInt.of(1L), PhpString.of(" 1"), true},
            {PhpInt.of(1L), PhpString.of("1 "), true},

            // null against a string is decided as a string, before the boolean rule can reach it.
            {PhpNull.NULL, PhpString.of(""), true},
            {PhpNull.NULL, PhpString.of("0"), false},
            {PhpNull.NULL, PhpString.of("a"), false},

            // null and booleans drag everything else down to a boolean.
            {PhpNull.NULL, PhpInt.of(0L), true},
            {PhpNull.NULL, PhpBool.FALSE, true},
            {PhpNull.NULL, PhpArray.empty(), true},
            {PhpBool.FALSE, PhpString.of("0"), true},
            {PhpBool.FALSE, PhpString.of(""), true},
            {PhpBool.TRUE, PhpString.of("anything"), true},
            {PhpBool.FALSE, PhpArray.empty(), true},
            {PhpBool.TRUE, PhpArray.ofValues(PhpInt.of(1L)), true},

            // Two numeric strings compare as numbers; anything else compares as text.
            {PhpString.of("1"), PhpString.of("01"), true},
            {PhpString.of("10"), PhpString.of("1e1"), true},
            {PhpString.of("1"), PhpString.of("1.0"), true},
            {PhpString.of("abc"), PhpString.of("abc"), true},
            {PhpString.of("abc"), PhpString.of("ABC"), false},

            // Mixed numbers.
            {PhpInt.of(1L), PhpFloat.of(1.0), true},
            {PhpFloat.of(0.0), PhpFloat.of(-0.0), true},
            {PhpFloat.of(Double.NaN), PhpFloat.of(Double.NaN), false},

            // An array is greater than anything that is not an array or a boolean.
            {PhpArray.ofValues(PhpInt.of(1L)), PhpInt.of(1L), false},
        };
    }

    @Test(dataProvider = "looseEquality")
    public void testLooseEquality(PhpValue left, PhpValue right, boolean expected) {
        Assert.assertEquals(left.looseEquals(right), expected, left + " == " + right);
        Assert.assertEquals(right.looseEquals(left), expected, "symmetry: " + right + " == " + left);
    }

    @DataProvider(name = "identity")
    public Object[][] identity() {
        return new Object[][] {
            {PhpInt.of(1L), PhpInt.of(1L), true},
            {PhpInt.of(1L), PhpFloat.of(1.0), false},
            {PhpInt.of(1L), PhpString.of("1"), false},
            {PhpString.of("a"), SafeString.of("a"), true},
            {PhpNull.NULL, PhpBool.FALSE, false},
            {PhpNull.NULL, PhpNull.NULL, true},
            {PhpFloat.of(0.0), PhpFloat.of(-0.0), true},
            {PhpFloat.of(Double.NaN), PhpFloat.of(Double.NaN), false},
            {PhpBool.TRUE, PhpInt.of(1L), false},
        };
    }

    @Test(dataProvider = "identity")
    public void testIdentity(PhpValue left, PhpValue right, boolean expected) {
        Assert.assertEquals(left.identical(right), expected, left + " === " + right);
        Assert.assertEquals(right.identical(left), expected, "symmetry: " + right + " === " + left);
    }

    @Test
    public void testOrdering() {
        Assert.assertTrue(PhpInt.of(1L).compare(PhpInt.of(2L)) < 0);
        Assert.assertTrue(PhpInt.of(2L).compare(PhpInt.of(1L)) > 0);
        Assert.assertEquals(PhpInt.of(2L).compare(PhpInt.of(2L)), 0);
        Assert.assertTrue(PhpString.of("apple").compare(PhpString.of("banana")) < 0);
        Assert.assertTrue(PhpString.of("10").compare(PhpString.of("9")) > 0, "numeric strings order as numbers");
        Assert.assertTrue(PhpString.of("a10").compare(PhpString.of("a9")) < 0, "other strings order as text");
    }

    @Test
    public void testNanIsNeverEqualAndNeverOrdered() {
        PhpFloat nan = PhpFloat.of(Double.NaN);
        Assert.assertFalse(nan.looseEquals(PhpFloat.of(1.0)));
        Assert.assertFalse(nan.looseEquals(nan));
        Assert.assertNotEquals(nan.compare(PhpFloat.of(1.0)), 0);
    }

    @Test
    public void testArrayEqualityIgnoresOrderButIdentityDoesNot() {
        PhpArray first = PhpArray.empty();
        first.put(ArrayKey.of("x"), PhpInt.of(1L));
        first.put(ArrayKey.of("y"), PhpInt.of(2L));

        PhpArray reordered = PhpArray.empty();
        reordered.put(ArrayKey.of("y"), PhpInt.of(2L));
        reordered.put(ArrayKey.of("x"), PhpInt.of(1L));

        Assert.assertTrue(first.looseEquals(reordered), "PHP: ['x'=>1,'y'=>2] == ['y'=>2,'x'=>1]");
        Assert.assertFalse(first.identical(reordered), "PHP: the same arrays are not ===");
    }

    @Test
    public void testArrayComparisonUsesSizeFirst() {
        PhpArray small = PhpArray.ofValues(PhpInt.of(9L));
        PhpArray large = PhpArray.ofValues(PhpInt.of(1L), PhpInt.of(1L));
        Assert.assertTrue(small.compare(large) < 0);
        Assert.assertTrue(large.compare(small) > 0);
    }

    @Test
    public void testArrayValuesCompareLoosely() {
        PhpArray numbers = PhpArray.ofValues(PhpInt.of(1L));
        PhpArray strings = PhpArray.ofValues(PhpString.of("1"));
        Assert.assertTrue(numbers.looseEquals(strings));
        Assert.assertFalse(numbers.identical(strings));
    }
}
