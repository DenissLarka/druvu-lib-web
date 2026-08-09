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

/** Conversions and truthiness for every value type: the table PHP's docs call "Converting to …". */
public class TestPhpValues {

    @DataProvider(name = "truthiness")
    public Object[][] truthiness() {
        return new Object[][] {
            {PhpNull.NULL, false},
            {PhpBool.FALSE, false},
            {PhpBool.TRUE, true},
            {PhpInt.of(0L), false},
            {PhpInt.of(1L), true},
            {PhpInt.of(-1L), true},
            {PhpFloat.of(0.0), false},
            {PhpFloat.of(-0.0), false},
            {PhpFloat.of(0.1), true},
            {PhpFloat.of(Double.NaN), true},
            {PhpString.of(""), false},
            {PhpString.of("0"), false},
            {PhpString.of("0.0"), true},
            {PhpString.of("00"), true},
            {PhpString.of(" "), true},
            {PhpString.of("false"), true},
            {PhpArray.empty(), false},
            {PhpArray.ofValues(PhpNull.NULL), true},
        };
    }

    @Test(dataProvider = "truthiness")
    public void testTruthiness(PhpValue value, boolean expected) {
        Assert.assertEquals(value.isTruthy(), expected, "truthiness of " + value);
        Assert.assertEquals(value.toBool(), PhpBool.of(expected));
    }

    @DataProvider(name = "stringConversion")
    public Object[][] stringConversion() {
        return new Object[][] {
            {PhpNull.NULL, ""},
            {PhpBool.TRUE, "1"},
            {PhpBool.FALSE, ""},
            {PhpInt.of(42L), "42"},
            {PhpInt.of(-42L), "-42"},
            {PhpFloat.of(1.5), "1.5"},
            {PhpString.of("hello"), "hello"},
            {SafeString.of("<b>"), "<b>"},
            {PhpArray.empty(), "Array"},
        };
    }

    @Test(dataProvider = "stringConversion")
    public void testStringConversion(PhpValue value, String expected) {
        Assert.assertEquals(value.toStr(), expected);
    }

    @DataProvider(name = "intConversion")
    public Object[][] intConversion() {
        return new Object[][] {
            {PhpNull.NULL, 0L},
            {PhpBool.TRUE, 1L},
            {PhpBool.FALSE, 0L},
            {PhpInt.of(42L), 42L},
            {PhpFloat.of(7.9), 7L},
            {PhpFloat.of(-7.9), -7L},
            {PhpFloat.of(Double.NaN), 0L},
            {PhpFloat.of(Double.POSITIVE_INFINITY), 0L},
            {PhpString.of("12abc"), 12L},
            {PhpString.of("abc"), 0L},
            {PhpArray.empty(), 0L},
            {PhpArray.ofValues(PhpInt.of(9L)), 1L},
        };
    }

    @Test(dataProvider = "intConversion")
    public void testIntConversion(PhpValue value, long expected) {
        Assert.assertEquals(value.toInt(), expected);
    }

    @Test
    public void testTypeNamesMatchGettype() {
        Assert.assertEquals(PhpNull.NULL.typeName(), "NULL");
        Assert.assertEquals(PhpBool.TRUE.typeName(), "boolean");
        Assert.assertEquals(PhpInt.of(1L).typeName(), "integer");
        Assert.assertEquals(PhpFloat.of(1.0).typeName(), "double");
        Assert.assertEquals(PhpString.of("x").typeName(), "string");
        Assert.assertEquals(SafeString.of("x").typeName(), "string");
        Assert.assertEquals(PhpArray.empty().typeName(), "array");
    }

    @Test
    public void testNullAndBooleansAreSingletons() {
        Assert.assertSame(PhpBool.of(true), PhpBool.TRUE);
        Assert.assertSame(PhpBool.of(false), PhpBool.FALSE);
        Assert.assertSame(PhpNull.NULL, PhpNull.NULL);
    }

    @Test
    public void testSafetyIsNotPartOfTheValue() {
        SafeString safe = SafeString.of("<b>");
        PhpString plain = PhpString.of("<b>");
        Assert.assertTrue(safe.isSafe());
        Assert.assertFalse(plain.isSafe());
        Assert.assertEquals(safe, plain, "safety must not affect equality");
        Assert.assertTrue(safe.identical(plain), "safety must not affect ===");
    }

    @Test
    public void testKeyConversion() {
        Assert.assertEquals(PhpNull.NULL.toKey(), new ArrayKey.StringKey(""));
        Assert.assertEquals(PhpBool.TRUE.toKey(), new ArrayKey.IntKey(1L));
        Assert.assertEquals(PhpBool.FALSE.toKey(), new ArrayKey.IntKey(0L));
        Assert.assertEquals(PhpInt.of(7L).toKey(), new ArrayKey.IntKey(7L));
        Assert.assertEquals(PhpFloat.of(3.9).toKey(), new ArrayKey.IntKey(3L));
        Assert.assertEquals(PhpString.of("8").toKey(), new ArrayKey.IntKey(8L));
        Assert.assertEquals(PhpString.of("08").toKey(), new ArrayKey.StringKey("08"));
    }

    @Test(expectedExceptions = com.druvu.web.php.internal.PhpProcessingException.class)
    public void testArrayCannotBeAKey() {
        PhpArray.empty().toKey();
    }
}
