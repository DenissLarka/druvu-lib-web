package com.druvu.web.php.internal.builtin;

import com.druvu.web.php.internal.PhpProcessingException;
import com.druvu.web.php.internal.value.PhpArray;
import com.druvu.web.php.internal.value.PhpClosure;
import com.druvu.web.php.internal.value.PhpNull;
import com.druvu.web.php.internal.value.PhpValue;
import java.util.List;

/**
 * What a built-in function was called with.
 *
 * <p>Its job is to keep the functions themselves down to what they actually do. Without it every one of them opens with
 * the same three lines of counting and converting, and the interesting part is buried.
 *
 * @author Deniss Larka
 */
public final class Arguments {

    private final String function;
    private final List<PhpValue> values;

    Arguments(String function, List<PhpValue> values, int least, int most) {
        this.function = function;
        this.values = values;
        if (values.size() < least || values.size() > most) {
            throw new PhpProcessingException(
                    function + "() expects " + expected(least, most) + ", " + values.size() + " given");
        }
    }

    private static String expected(int least, int most) {
        if (least == most) {
            return least + (least == 1 ? " argument" : " arguments");
        }
        return most == Integer.MAX_VALUE ? "at least " + least + " arguments" : least + " to " + most + " arguments";
    }

    public int count() {
        return values.size();
    }

    public PhpValue at(int index) {
        return index < values.size() ? values.get(index) : PhpNull.NULL;
    }

    public boolean has(int index) {
        return index < values.size();
    }

    public String string(int index) {
        return at(index).toStr();
    }

    public long integer(int index) {
        return at(index).toInt();
    }

    public double number(int index) {
        return at(index).toFloat();
    }

    public boolean flag(int index) {
        return at(index).isTruthy();
    }

    public long integerOr(int index, long fallback) {
        return has(index) ? integer(index) : fallback;
    }

    public String stringOr(int index, String fallback) {
        return has(index) ? string(index) : fallback;
    }

    public PhpArray array(int index) {
        if (at(index) instanceof PhpArray array) {
            return array;
        }
        throw new PhpProcessingException(function + "(): argument " + (index + 1) + " must be an array, "
                + at(index).typeName() + " given");
    }

    public PhpClosure closure(int index) {
        if (at(index) instanceof PhpClosure closure) {
            return closure;
        }
        throw new PhpProcessingException(function + "(): argument " + (index + 1) + " must be callable, "
                + at(index).typeName() + " given");
    }

    /** The arguments from {@code index} onwards, for the functions that take however many they are given. */
    public List<PhpValue> from(int index) {
        return index >= values.size() ? List.of() : List.copyOf(values.subList(index, values.size()));
    }

    public String name() {
        return function;
    }
}
