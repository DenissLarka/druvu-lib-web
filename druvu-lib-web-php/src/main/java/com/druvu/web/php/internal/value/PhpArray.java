package com.druvu.web.php.internal.value;

import com.druvu.web.php.internal.PhpProcessingException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A PHP array: an ordered map keyed by integers or strings, iterated in insertion order.
 *
 * <p>PHP arrays are values, not references — assigning one copies it. Rather than the copy-on-write machinery a full
 * PHP runtime needs, {@link #copy()} copies eagerly and deeply. Layout data is small, and the alternative costs clarity
 * everywhere to save allocations nothing measures.
 *
 * @author Deniss Larka
 */
public final class PhpArray extends PhpValue {

    private final Map<ArrayKey, PhpValue> entries = new LinkedHashMap<>();

    /** The highest integer key this array has ever held. Meaningless until it has held one. */
    private long highestIndex;

    private boolean hasIntegerKey;

    private PhpArray() {}

    public static PhpArray empty() {
        return new PhpArray();
    }

    /** A list-style array: the values keyed 0, 1, 2 and so on. */
    public static PhpArray ofValues(PhpValue... values) {
        PhpArray array = new PhpArray();
        for (PhpValue value : values) {
            array.append(value);
        }
        return array;
    }

    public void put(ArrayKey key, PhpValue value) {
        entries.put(key, value);
        if (key instanceof ArrayKey.IntKey index && (!hasIntegerKey || index.value() > highestIndex)) {
            highestIndex = index.value();
            hasIntegerKey = true;
        }
    }

    /** {@code $a[] = $value}. */
    public void append(PhpValue value) {
        put(new ArrayKey.IntKey(nextIndex()), value);
    }

    /** The value at a key, or null when the array has no such key. */
    public PhpValue get(ArrayKey key) {
        return entries.get(key);
    }

    public boolean containsKey(ArrayKey key) {
        return entries.containsKey(key);
    }

    public void remove(ArrayKey key) {
        entries.remove(key);
    }

    public int size() {
        return entries.size();
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /** The entries in insertion order. Unmodifiable: changes go through this class so the next index stays right. */
    public Map<ArrayKey, PhpValue> entries() {
        return Collections.unmodifiableMap(entries);
    }

    /**
     * The key an append will take: one past the <em>highest</em> integer key the array has ever held.
     *
     * <p>Highest, not most recent, and never reused: removing an entry does not free its key. Since PHP 8.3 a negative
     * key counts as well, which is why {@code $a[-5] = 1; $a[] = 2;} lands on -4 and not on 0. Verified against PHP
     * 8.5.9 rather than remembered — the pre-8.3 rule is the one this got wrong first.
     *
     * @throws PhpProcessingException if the highest key is already the largest integer there is
     */
    public long nextIndex() {
        if (!hasIntegerKey) {
            return 0L;
        }
        if (highestIndex == Long.MAX_VALUE) {
            throw new PhpProcessingException("Cannot add element to the array as the next element is already occupied");
        }
        return highestIndex + 1;
    }

    /** A copy with PHP's value semantics: nested arrays are copied too; every other value type is immutable. */
    public PhpArray copy() {
        PhpArray copy = new PhpArray();
        for (Map.Entry<ArrayKey, PhpValue> entry : entries.entrySet()) {
            PhpValue value = entry.getValue();
            copy.put(entry.getKey(), value instanceof PhpArray nested ? nested.copy() : value);
        }
        copy.highestIndex = highestIndex;
        copy.hasIntegerKey = hasIntegerKey;
        return copy;
    }

    @Override
    public PhpValue copyForAssignment() {
        return copy();
    }

    @Override
    public String typeName() {
        return "array";
    }

    @Override
    public boolean isTruthy() {
        return !entries.isEmpty();
    }

    /** PHP prints the literal word {@code Array} for an array. */
    @Override
    public String toStr() {
        return "Array";
    }

    @Override
    public long toInt() {
        return entries.isEmpty() ? 0L : 1L;
    }

    @Override
    public double toFloat() {
        return entries.isEmpty() ? 0.0 : 1.0;
    }

    @Override
    public ArrayKey toKey() {
        throw new PhpProcessingException("Illegal offset type: array cannot be used as an array key");
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof PhpArray that) || entries.size() != that.entries.size()) {
            return false;
        }
        var mine = entries.entrySet().iterator();
        var theirs = that.entries.entrySet().iterator();
        while (mine.hasNext()) {
            if (!mine.next().equals(theirs.next())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return entries.hashCode();
    }

    @Override
    public String toString() {
        return "array(" + entries.size() + ")";
    }
}
