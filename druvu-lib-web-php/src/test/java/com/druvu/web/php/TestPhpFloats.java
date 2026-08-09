package com.druvu.web.php;

import com.druvu.web.php.internal.value.PhpFloats;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * How PHP prints a float. Every expectation here is what {@code php -r 'echo $x;'} produces, which is 14 significant
 * digits with trailing zeros dropped — not Java's shortest-round-trip representation.
 */
public class TestPhpFloats {

    @DataProvider(name = "printed")
    public Object[][] printed() {
        return new Object[][] {
            {1.0, "1"},
            {-1.0, "-1"},
            {0.0, "0"},
            {-0.0, "-0"},
            {1.5, "1.5"},
            {100.0, "100"},
            {1234.5678, "1234.5678"},
            {0.1 + 0.2, "0.3"},
            {1.0 / 3.0, "0.33333333333333"},
            {2.0 / 3.0, "0.66666666666667"},
            {0.0001, "0.0001"},
            {0.00001, "1.0E-5"},
            {1.0e13, "10000000000000"},
            {1.0e14, "1.0E+14"},
            {1.0e25, "1.0E+25"},
            {-1.0e25, "-1.0E+25"},
            {1.234567890123456e15, "1.2345678901235E+15"},
            {Double.NaN, "NAN"},
            {Double.POSITIVE_INFINITY, "INF"},
            {Double.NEGATIVE_INFINITY, "-INF"},
        };
    }

    @Test(dataProvider = "printed")
    public void testPrinted(double value, String expected) {
        Assert.assertEquals(PhpFloats.toPhpString(value), expected);
    }

    @Test
    public void testSameValueFollowsPhpIdentity() {
        Assert.assertTrue(PhpFloats.sameValue(1.5, 1.5));
        Assert.assertTrue(PhpFloats.sameValue(0.0, -0.0), "PHP: 0.0 === -0.0");
        Assert.assertFalse(PhpFloats.sameValue(Double.NaN, Double.NaN), "PHP: NAN === NAN is false");
        Assert.assertFalse(PhpFloats.sameValue(1.0, 1.0000001));
    }

    @Test
    public void testIntCastTruncatesTowardsZero() {
        Assert.assertEquals(PhpFloats.toInt(7.9), 7L);
        Assert.assertEquals(PhpFloats.toInt(-7.9), -7L);
        Assert.assertEquals(PhpFloats.toInt(0.0), 0L);
        Assert.assertEquals(PhpFloats.toInt(Double.NaN), 0L);
        Assert.assertEquals(PhpFloats.toInt(Double.POSITIVE_INFINITY), 0L);
        Assert.assertEquals(PhpFloats.toInt(Double.NEGATIVE_INFINITY), 0L);
    }
}
