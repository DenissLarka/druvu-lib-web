package com.druvu.web.php;

import com.druvu.web.php.internal.value.NumericStrings;
import com.druvu.web.php.internal.value.PhpFloat;
import com.druvu.web.php.internal.value.PhpInt;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/** PHP's two different questions about numbers in strings: is the whole thing a number, and what does it start with. */
public class TestNumericStrings {

    @DataProvider(name = "numeric")
    public Object[][] numeric() {
        return new Object[][] {
            {"123", true},
            {"-123", true},
            {"+123", true},
            {"12.5", true},
            {".5", true},
            {"5.", true},
            {"1e3", true},
            {"-1.5e-3", true},
            {" 12", true},
            {"12 ", true},
            {" 12 ", true},
            {"", false},
            {" ", false},
            {"abc", false},
            {"12abc", false},
            {"1e", false},
            {"0x1A", false},
            {"1,5", false},
            {".", false},
            {"-", false},
        };
    }

    @Test(dataProvider = "numeric")
    public void testIsNumeric(String text, boolean expected) {
        Assert.assertEquals(NumericStrings.isNumeric(text), expected, "is_numeric('" + text + "')");
    }

    @DataProvider(name = "intCast")
    public Object[][] intCast() {
        return new Object[][] {
            {"42", 42L},
            {"12abc", 12L},
            {"abc", 0L},
            {"", 0L},
            {"  42", 42L},
            {"-7.9", -7L},
            {"1e3", 1000L},
            {"3.9xyz", 3L},
            {"+5", 5L},
        };
    }

    @Test(dataProvider = "intCast")
    public void testIntCast(String text, long expected) {
        Assert.assertEquals(NumericStrings.toLong(text), expected, "(int) '" + text + "'");
    }

    @Test
    public void testFloatCast() {
        Assert.assertEquals(NumericStrings.toDouble("1.5xyz"), 1.5);
        Assert.assertEquals(NumericStrings.toDouble("abc"), 0.0);
        Assert.assertEquals(NumericStrings.toDouble("-2.5e2"), -250.0);
    }

    @Test
    public void testNumberKeepsIntegersIntegral() {
        Assert.assertEquals(NumericStrings.toNumber("42"), PhpInt.of(42L));
        Assert.assertEquals(NumericStrings.toNumber("42.0"), PhpFloat.of(42.0));
        Assert.assertEquals(NumericStrings.toNumber("1e3"), PhpFloat.of(1000.0));
        Assert.assertEquals(NumericStrings.toNumber("99999999999999999999"), PhpFloat.of(1.0e20));
    }
}
