package com.druvu.web.php.internal.runtime;

import com.druvu.web.php.internal.value.ArrayKey;
import com.druvu.web.php.internal.value.PhpArray;
import com.druvu.web.php.internal.value.PhpBool;
import com.druvu.web.php.internal.value.PhpFloat;
import com.druvu.web.php.internal.value.PhpInt;
import com.druvu.web.php.internal.value.PhpNull;
import com.druvu.web.php.internal.value.PhpObject;
import com.druvu.web.php.internal.value.PhpString;
import com.druvu.web.php.internal.value.PhpValue;
import java.util.Collection;
import java.util.Map;

/**
 * The border between the application's data and the template's.
 *
 * <p>Everything a host hands over crosses here, and the mapping is deliberately shallow: a map becomes an array, a
 * collection becomes a list, the scalars become scalars, and anything else becomes a read-only view of the object
 * itself. Nothing is copied that does not need to be, and nothing the template can do reaches back.
 *
 * @author Deniss Larka
 */
public final class HostValues {

    private HostValues() {}

    public static PhpValue of(Object host) {
        if (host == null) {
            return PhpNull.NULL;
        }
        if (host instanceof PhpValue already) {
            return already;
        }
        if (host instanceof CharSequence text) {
            return PhpString.of(text.toString());
        }
        if (host instanceof Boolean flag) {
            return PhpBool.of(flag);
        }
        if (host instanceof Byte || host instanceof Short || host instanceof Integer || host instanceof Long) {
            return PhpInt.of(((Number) host).longValue());
        }
        if (host instanceof Number number) {
            return PhpFloat.of(number.doubleValue());
        }
        if (host instanceof Character c) {
            return PhpString.of(c.toString());
        }
        if (host instanceof Map<?, ?> map) {
            return fromMap(map);
        }
        if (host instanceof Collection<?> collection) {
            return fromCollection(collection);
        }
        if (host instanceof Object[] array) {
            PhpArray values = PhpArray.empty();
            for (Object each : array) {
                values.append(of(each));
            }
            return values;
        }
        return PhpObject.of(host);
    }

    private static PhpValue fromMap(Map<?, ?> map) {
        PhpArray array = PhpArray.empty();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            array.put(keyOf(entry.getKey()), of(entry.getValue()));
        }
        return array;
    }

    private static PhpValue fromCollection(Collection<?> collection) {
        PhpArray array = PhpArray.empty();
        for (Object each : collection) {
            array.append(of(each));
        }
        return array;
    }

    private static ArrayKey keyOf(Object key) {
        if (key instanceof Number number) {
            return ArrayKey.of(number.longValue());
        }
        return ArrayKey.of(String.valueOf(key));
    }
}
