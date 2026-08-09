package com.druvu.web.php.internal.builtin;

import com.druvu.web.php.internal.PhpProcessingException;
import com.druvu.web.php.internal.runtime.FunctionRegistry;
import com.druvu.web.php.internal.value.ArrayKey;
import com.druvu.web.php.internal.value.PhpArray;
import com.druvu.web.php.internal.value.PhpBool;
import com.druvu.web.php.internal.value.PhpClosure;
import com.druvu.web.php.internal.value.PhpComparison;
import com.druvu.web.php.internal.value.PhpInt;
import com.druvu.web.php.internal.value.PhpNull;
import com.druvu.web.php.internal.value.PhpString;
import com.druvu.web.php.internal.value.PhpValue;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Everything that works on lists and maps.
 *
 * <p>The sorts are the dialect's only functions that change what they are given rather than answering with something
 * new. They are registered as such, and the call site puts the result back where it came from — the one place a
 * reference exists at all, kept to the handful of functions that genuinely need one.
 *
 * @author Deniss Larka
 */
final class ArrayFunctions {

    private ArrayFunctions() {}

    static void registerInto(FunctionRegistry registry) {
        Functions.define(
                registry, "count", 1, 2, (env, a) -> PhpInt.of(a.array(0).size()));
        Functions.alias(registry, "count", "sizeof");

        Functions.define(registry, "array_keys", 1, 2, (env, a) -> {
            PhpArray keys = PhpArray.empty();
            for (Map.Entry<ArrayKey, PhpValue> entry : a.array(0).entries().entrySet()) {
                if (!a.has(1) || PhpComparison.looseEquals(entry.getValue(), a.at(1))) {
                    keys.append(StringFunctions.keyAsValue(entry.getKey()));
                }
            }
            return keys;
        });
        Functions.define(registry, "array_values", 1, (env, a) -> {
            PhpArray values = PhpArray.empty();
            a.array(0).entries().values().forEach(values::append);
            return values;
        });
        Functions.define(registry, "in_array", 2, 3, (env, a) -> {
            for (PhpValue value : a.array(1).entries().values()) {
                boolean found =
                        a.flag(2) ? PhpComparison.identical(value, a.at(0)) : PhpComparison.looseEquals(value, a.at(0));
                if (found) {
                    return PhpBool.TRUE;
                }
            }
            return PhpBool.FALSE;
        });
        Functions.define(registry, "array_search", 2, 3, (env, a) -> {
            for (Map.Entry<ArrayKey, PhpValue> entry : a.array(1).entries().entrySet()) {
                boolean found = a.flag(2)
                        ? PhpComparison.identical(entry.getValue(), a.at(0))
                        : PhpComparison.looseEquals(entry.getValue(), a.at(0));
                if (found) {
                    return StringFunctions.keyAsValue(entry.getKey());
                }
            }
            return PhpBool.FALSE;
        });
        Functions.define(
                registry,
                "array_key_exists",
                2,
                (env, a) -> PhpBool.of(a.array(1).containsKey(a.at(0).toKey())));
        Functions.alias(registry, "array_key_exists", "key_exists");

        Functions.define(registry, "array_merge", 1, Integer.MAX_VALUE, (env, a) -> {
            PhpArray merged = PhpArray.empty();
            for (int i = 0; i < a.count(); i++) {
                for (Map.Entry<ArrayKey, PhpValue> entry : a.array(i).entries().entrySet()) {
                    StringFunctions.copyEntry(merged, entry, entry.getKey() instanceof ArrayKey.StringKey);
                }
            }
            return merged;
        });

        Functions.define(registry, "array_slice", 2, 4, (env, a) -> slice(a));
        Functions.define(registry, "array_reverse", 1, 2, (env, a) -> {
            List<Map.Entry<ArrayKey, PhpValue>> entries =
                    new ArrayList<>(a.array(0).entries().entrySet());
            PhpArray reversed = PhpArray.empty();
            for (int i = entries.size() - 1; i >= 0; i--) {
                StringFunctions.copyEntry(
                        reversed, entries.get(i), a.flag(1) || entries.get(i).getKey() instanceof ArrayKey.StringKey);
            }
            return reversed;
        });
        Functions.define(registry, "range", 2, 3, (env, a) -> range(a));
        Functions.define(registry, "array_chunk", 2, 3, (env, a) -> chunk(a));
        Functions.define(registry, "array_column", 2, 3, (env, a) -> column(a));
        Functions.define(registry, "array_unique", 1, 2, (env, a) -> {
            PhpArray unique = PhpArray.empty();
            List<String> seen = new ArrayList<>();
            for (Map.Entry<ArrayKey, PhpValue> entry : a.array(0).entries().entrySet()) {
                String asText = entry.getValue().toStr();
                if (!seen.contains(asText)) {
                    seen.add(asText);
                    unique.put(entry.getKey(), entry.getValue());
                }
            }
            return unique;
        });
        Functions.define(registry, "array_combine", 2, (env, a) -> {
            List<PhpValue> keys = List.copyOf(a.array(0).entries().values());
            List<PhpValue> values = List.copyOf(a.array(1).entries().values());
            if (keys.size() != values.size()) {
                throw new PhpProcessingException("array_combine(): the two arrays must be the same length");
            }
            PhpArray combined = PhpArray.empty();
            for (int i = 0; i < keys.size(); i++) {
                combined.put(keys.get(i).toKey(), values.get(i));
            }
            return combined;
        });
        Functions.define(registry, "array_flip", 1, (env, a) -> {
            PhpArray flipped = PhpArray.empty();
            for (Map.Entry<ArrayKey, PhpValue> entry : a.array(0).entries().entrySet()) {
                flipped.put(entry.getValue().toKey(), StringFunctions.keyAsValue(entry.getKey()));
            }
            return flipped;
        });
        Functions.define(registry, "array_fill", 3, (env, a) -> {
            PhpArray filled = PhpArray.empty();
            long start = a.integer(0);
            for (long i = 0; i < a.integer(1); i++) {
                filled.put(ArrayKey.of(start + i), a.at(2));
            }
            return filled;
        });
        Functions.define(registry, "array_key_first", 1, (env, a) -> firstOrLastKey(a.array(0), true));
        Functions.define(registry, "array_key_last", 1, (env, a) -> firstOrLastKey(a.array(0), false));

        Functions.define(registry, "array_sum", 1, (env, a) -> fold(a.array(0), true));
        Functions.define(registry, "array_product", 1, (env, a) -> fold(a.array(0), false));
        Functions.define(registry, "min", 1, Integer.MAX_VALUE, (env, a) -> extreme(a, true));
        Functions.define(registry, "max", 1, Integer.MAX_VALUE, (env, a) -> extreme(a, false));

        // The sorts, which rearrange what they are handed.
        sort(registry, "sort", byValue(false), false);
        sort(registry, "rsort", byValue(true), false);
        sort(registry, "asort", byValue(false), true);
        sort(registry, "arsort", byValue(true), true);
        sort(registry, "ksort", byKey(false), true);
        sort(registry, "krsort", byKey(true), true);

        Functions.define(registry, "array_map", 2, (env, a) -> {
            PhpClosure mapper = a.closure(0);
            PhpArray mapped = PhpArray.empty();
            for (Map.Entry<ArrayKey, PhpValue> entry : a.array(1).entries().entrySet()) {
                mapped.put(entry.getKey(), mapper.call(List.of(entry.getValue())));
            }
            return mapped;
        });
        Functions.define(registry, "array_filter", 1, 3, (env, a) -> filter(a));
        registry.registerInPlace("usort", (env, values) -> {
            Arguments a = new Arguments("usort", values, 2, 2);
            return userSort(a.array(0), a.closure(1), false, false);
        });
        registry.registerInPlace("uasort", (env, values) -> {
            Arguments a = new Arguments("uasort", values, 2, 2);
            return userSort(a.array(0), a.closure(1), true, false);
        });
        registry.registerInPlace("uksort", (env, values) -> {
            Arguments a = new Arguments("uksort", values, 2, 2);
            return userSort(a.array(0), a.closure(1), true, true);
        });
    }

    private static void sort(
            FunctionRegistry registry, String name, Comparator<Map.Entry<ArrayKey, PhpValue>> order, boolean keepKeys) {
        registry.registerInPlace(name, (env, values) -> {
            PhpArray array = new Arguments(name, values, 1, 2).array(0);
            rearrange(array, sorted(array, order), keepKeys);
            return PhpBool.TRUE;
        });
    }

    private static Comparator<Map.Entry<ArrayKey, PhpValue>> byValue(boolean descending) {
        Comparator<Map.Entry<ArrayKey, PhpValue>> order =
                (left, right) -> PhpComparison.compare(left.getValue(), right.getValue());
        return descending ? order.reversed() : order;
    }

    private static Comparator<Map.Entry<ArrayKey, PhpValue>> byKey(boolean descending) {
        Comparator<Map.Entry<ArrayKey, PhpValue>> order = (left, right) -> PhpComparison.compare(
                StringFunctions.keyAsValue(left.getKey()), StringFunctions.keyAsValue(right.getKey()));
        return descending ? order.reversed() : order;
    }

    private static List<Map.Entry<ArrayKey, PhpValue>> sorted(
            PhpArray array, Comparator<Map.Entry<ArrayKey, PhpValue>> order) {
        List<Map.Entry<ArrayKey, PhpValue>> entries =
                new ArrayList<>(array.entries().entrySet());
        entries.sort(order);
        return entries;
    }

    /** Empties the array and puts the entries back in their new order, keeping or dropping the keys as PHP does. */
    private static void rearrange(PhpArray array, List<Map.Entry<ArrayKey, PhpValue>> entries, boolean keepKeys) {
        List<Map.Entry<ArrayKey, PhpValue>> ordered = List.copyOf(entries);
        for (ArrayKey key : List.copyOf(array.entries().keySet())) {
            array.remove(key);
        }
        for (Map.Entry<ArrayKey, PhpValue> entry : ordered) {
            StringFunctions.copyEntry(array, entry, keepKeys);
        }
    }

    private static PhpValue userSort(PhpArray array, PhpClosure order, boolean keepKeys, boolean onKeys) {
        List<Map.Entry<ArrayKey, PhpValue>> entries =
                new ArrayList<>(array.entries().entrySet());
        entries.sort((left, right) -> (int) Math.signum(order.call(List.of(
                        onKeys ? StringFunctions.keyAsValue(left.getKey()) : left.getValue(),
                        onKeys ? StringFunctions.keyAsValue(right.getKey()) : right.getValue()))
                .toInt()));
        rearrange(array, entries, keepKeys);
        return PhpBool.TRUE;
    }

    private static PhpValue filter(Arguments a) {
        PhpArray kept = PhpArray.empty();
        boolean byKey = a.integerOr(2, 0) == 2;
        boolean byBoth = a.integerOr(2, 0) == 1;
        for (Map.Entry<ArrayKey, PhpValue> entry : a.array(0).entries().entrySet()) {
            boolean keep;
            if (!a.has(1)) {
                keep = entry.getValue().isTruthy();
            } else if (byKey) {
                keep = a.closure(1)
                        .call(List.of(StringFunctions.keyAsValue(entry.getKey())))
                        .isTruthy();
            } else if (byBoth) {
                keep = a.closure(1)
                        .call(List.of(entry.getValue(), StringFunctions.keyAsValue(entry.getKey())))
                        .isTruthy();
            } else {
                keep = a.closure(1).call(List.of(entry.getValue())).isTruthy();
            }
            if (keep) {
                kept.put(entry.getKey(), entry.getValue());
            }
        }
        return kept;
    }

    private static PhpValue slice(Arguments a) {
        List<Map.Entry<ArrayKey, PhpValue>> entries =
                new ArrayList<>(a.array(0).entries().entrySet());
        int size = entries.size();
        long offset = a.integer(1);
        int from = (int) (offset < 0 ? Math.max(0, size + offset) : Math.min(offset, size));
        int to = size;
        if (a.has(2) && !(a.at(2) instanceof PhpNull)) {
            long length = a.integer(2);
            to = (int) (length < 0 ? Math.max(from, size + length) : Math.min(size, from + length));
        }
        boolean keepKeys = a.flag(3);
        PhpArray sliced = PhpArray.empty();
        for (int i = from; i < to; i++) {
            StringFunctions.copyEntry(
                    sliced, entries.get(i), keepKeys || entries.get(i).getKey() instanceof ArrayKey.StringKey);
        }
        return sliced;
    }

    private static PhpValue range(Arguments a) {
        PhpArray values = PhpArray.empty();
        if (a.at(0) instanceof PhpString from
                && a.at(1) instanceof PhpString to
                && Text.length(from.value()) == 1
                && Text.length(to.value()) == 1
                && !Character.isDigit(from.value().charAt(0))) {
            char start = from.value().charAt(0);
            char end = to.value().charAt(0);
            int step = start <= end ? 1 : -1;
            for (char c = start; step > 0 ? c <= end : c >= end; c += step) {
                values.append(PhpString.of(String.valueOf(c)));
            }
            return values;
        }
        double start = a.number(0);
        double end = a.number(1);
        double step = a.has(2) ? Math.abs(a.number(2)) : 1.0;
        if (com.druvu.web.php.internal.value.PhpFloats.sameValue(step, 0.0)) {
            throw new PhpProcessingException("range(): the step cannot be zero");
        }
        boolean whole =
                a.at(0) instanceof PhpInt && a.at(1) instanceof PhpInt && (!a.has(2) || a.at(2) instanceof PhpInt);
        for (double v = start; start <= end ? v <= end + 1e-9 : v >= end - 1e-9; v += start <= end ? step : -step) {
            values.append(whole ? PhpInt.of((long) v) : com.druvu.web.php.internal.value.PhpFloat.of(v));
        }
        return values;
    }

    private static PhpValue chunk(Arguments a) {
        int size = (int) a.integer(1);
        if (size < 1) {
            throw new PhpProcessingException("array_chunk(): the chunk size must be at least 1");
        }
        boolean keepKeys = a.flag(2);
        PhpArray chunks = PhpArray.empty();
        PhpArray current = PhpArray.empty();
        for (Map.Entry<ArrayKey, PhpValue> entry : a.array(0).entries().entrySet()) {
            StringFunctions.copyEntry(current, entry, keepKeys);
            if (current.size() == size) {
                chunks.append(current);
                current = PhpArray.empty();
            }
        }
        if (!current.isEmpty()) {
            chunks.append(current);
        }
        return chunks;
    }

    private static PhpValue column(Arguments a) {
        PhpArray picked = PhpArray.empty();
        for (PhpValue row : a.array(0).entries().values()) {
            if (!(row instanceof PhpArray fields)) {
                continue;
            }
            PhpValue value =
                    a.at(1) instanceof PhpNull ? row : fields.get(a.at(1).toKey());
            if (value == null) {
                continue;
            }
            PhpValue key = a.has(2) ? fields.get(a.at(2).toKey()) : null;
            if (key == null) {
                picked.append(value);
            } else {
                picked.put(key.toKey(), value);
            }
        }
        return picked;
    }

    private static PhpValue firstOrLastKey(PhpArray array, boolean first) {
        ArrayKey found = null;
        for (ArrayKey key : array.entries().keySet()) {
            found = key;
            if (first) {
                break;
            }
        }
        return found == null ? PhpNull.NULL : StringFunctions.keyAsValue(found);
    }

    private static PhpValue fold(PhpArray array, boolean adding) {
        PhpValue total = PhpInt.of(adding ? 0L : 1L);
        for (PhpValue value : array.entries().values()) {
            total = adding
                    ? com.druvu.web.php.internal.value.PhpArithmetic.add(total, value)
                    : com.druvu.web.php.internal.value.PhpArithmetic.multiply(total, value);
        }
        return total;
    }

    /** {@code min}/{@code max} take either one array or several values, as PHP does. */
    private static PhpValue extreme(Arguments a, boolean smallest) {
        List<PhpValue> candidates = a.count() == 1 && a.at(0) instanceof PhpArray array
                ? List.copyOf(array.entries().values())
                : a.from(0);
        if (candidates.isEmpty()) {
            throw new PhpProcessingException(a.name() + "(): there must be at least one value to compare");
        }
        PhpValue best = candidates.get(0);
        for (PhpValue candidate : candidates) {
            boolean better =
                    smallest ? PhpComparison.lessThan(candidate, best) : PhpComparison.greaterThan(candidate, best);
            if (better) {
                best = candidate;
            }
        }
        return best;
    }
}
