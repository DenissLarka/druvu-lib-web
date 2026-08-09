package com.druvu.web.php;

import com.druvu.web.php.internal.PhpProcessingException;
import com.druvu.web.php.internal.value.ArrayKey;
import com.druvu.web.php.internal.value.PhpArray;
import com.druvu.web.php.internal.value.PhpInt;
import com.druvu.web.php.internal.value.PhpString;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/** Array keys, ordering and the value semantics that make {@code $b = $a} a copy rather than an alias. */
public class TestPhpArray {

    @DataProvider(name = "keyNormalisation")
    public Object[][] keyNormalisation() {
        return new Object[][] {
            {"8", new ArrayKey.IntKey(8L)},
            {"0", new ArrayKey.IntKey(0L)},
            {"-5", new ArrayKey.IntKey(-5L)},
            {"08", new ArrayKey.StringKey("08")},
            {"-0", new ArrayKey.StringKey("-0")},
            {"+1", new ArrayKey.StringKey("+1")},
            {"1.5", new ArrayKey.StringKey("1.5")},
            {" 1", new ArrayKey.StringKey(" 1")},
            {"", new ArrayKey.StringKey("")},
            {"abc", new ArrayKey.StringKey("abc")},
            {"99999999999999999999", new ArrayKey.StringKey("99999999999999999999")},
        };
    }

    @Test(dataProvider = "keyNormalisation")
    public void testKeyNormalisation(String written, ArrayKey expected) {
        Assert.assertEquals(ArrayKey.of(written), expected, "key '" + written + "'");
    }

    @Test
    public void testStringAndIntegerKeysMeetInTheSameSlot() {
        PhpArray array = PhpArray.empty();
        array.put(ArrayKey.of(8L), PhpString.of("first"));
        array.put(ArrayKey.of("8"), PhpString.of("second"));
        Assert.assertEquals(array.size(), 1);
        Assert.assertEquals(array.get(ArrayKey.of(8L)), PhpString.of("second"));
    }

    @Test
    public void testAppendUsesTheNextFreeIndex() {
        PhpArray array = PhpArray.ofValues(PhpInt.of(1L), PhpInt.of(2L));
        Assert.assertEquals(array.nextIndex(), 2L);
        array.put(ArrayKey.of(10L), PhpInt.of(3L));
        array.append(PhpInt.of(4L));
        Assert.assertEquals(array.get(ArrayKey.of(11L)), PhpInt.of(4L));
    }

    /** PHP 8.3 changed this: before it, an append after a negative key landed on 0. Checked against PHP 8.5.9. */
    @Test
    public void testANegativeKeyMovesTheNextIndexToo() {
        PhpArray array = PhpArray.empty();
        array.put(ArrayKey.of(-5L), PhpInt.of(1L));
        array.append(PhpInt.of(2L));
        array.append(PhpInt.of(3L));
        Assert.assertEquals(
                List.copyOf(array.entries().keySet()),
                List.of(new ArrayKey.IntKey(-5L), new ArrayKey.IntKey(-4L), new ArrayKey.IntKey(-3L)));
    }

    @Test
    public void testTheHighestKeyDecidesTheNextOneNotTheMostRecent() {
        PhpArray array = PhpArray.empty();
        array.put(ArrayKey.of(5L), PhpInt.of(1L));
        array.put(ArrayKey.of(-100L), PhpInt.of(2L));
        array.append(PhpInt.of(3L));
        Assert.assertEquals(array.get(ArrayKey.of(6L)), PhpInt.of(3L));
    }

    @Test(
            expectedExceptions = PhpProcessingException.class,
            expectedExceptionsMessageRegExp = ".*next element is already occupied.*")
    public void testAppendRefusesWhenThereIsNoNextIndexLeft() {
        PhpArray array = PhpArray.empty();
        array.put(ArrayKey.of(Long.MAX_VALUE), PhpInt.of(1L));
        array.append(PhpInt.of(2L));
    }

    @Test
    public void testRemovingDoesNotFreeTheIndexAgain() {
        PhpArray array = PhpArray.ofValues(PhpInt.of(1L), PhpInt.of(2L));
        array.remove(ArrayKey.of(1L));
        array.append(PhpInt.of(3L));
        Assert.assertEquals(array.get(ArrayKey.of(2L)), PhpInt.of(3L));
        Assert.assertFalse(array.containsKey(ArrayKey.of(1L)));
    }

    @Test
    public void testIterationIsInsertionOrdered() {
        PhpArray array = PhpArray.empty();
        array.put(ArrayKey.of("zebra"), PhpInt.of(1L));
        array.put(ArrayKey.of("apple"), PhpInt.of(2L));
        array.put(ArrayKey.of(3L), PhpInt.of(3L));
        Assert.assertEquals(
                List.copyOf(array.entries().keySet()),
                List.of(new ArrayKey.StringKey("zebra"), new ArrayKey.StringKey("apple"), new ArrayKey.IntKey(3L)));
    }

    @Test
    public void testCopyIsDeepForNestedArrays() {
        PhpArray inner = PhpArray.ofValues(PhpInt.of(1L));
        PhpArray outer = PhpArray.ofValues(inner);

        PhpArray copy = outer.copy();
        ((PhpArray) copy.get(ArrayKey.of(0L))).append(PhpInt.of(99L));

        Assert.assertEquals(inner.size(), 1, "the original nested array must not see the change");
        Assert.assertEquals(((PhpArray) copy.get(ArrayKey.of(0L))).size(), 2);
    }

    @Test
    public void testCopyKeepsTheNextIndex() {
        PhpArray array = PhpArray.ofValues(PhpInt.of(1L), PhpInt.of(2L));
        array.remove(ArrayKey.of(1L));
        Assert.assertEquals(array.copy().nextIndex(), array.nextIndex());
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void testEntriesAreNotWritableFromOutside() {
        PhpArray.empty().entries().put(ArrayKey.of(0L), PhpInt.of(1L));
    }
}
