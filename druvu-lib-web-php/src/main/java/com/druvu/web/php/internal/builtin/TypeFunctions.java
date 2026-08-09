package com.druvu.web.php.internal.builtin;

import com.druvu.web.php.internal.PhpProcessingException;
import com.druvu.web.php.internal.runtime.FunctionRegistry;
import com.druvu.web.php.internal.value.ArrayKey;
import com.druvu.web.php.internal.value.NumericStrings;
import com.druvu.web.php.internal.value.PhpArray;
import com.druvu.web.php.internal.value.PhpBool;
import com.druvu.web.php.internal.value.PhpClosure;
import com.druvu.web.php.internal.value.PhpFloat;
import com.druvu.web.php.internal.value.PhpInt;
import com.druvu.web.php.internal.value.PhpNull;
import com.druvu.web.php.internal.value.PhpString;
import com.druvu.web.php.internal.value.PhpValue;
import java.util.Map;

/**
 * Asking a value what it is, and turning it into something else.
 *
 * <p>The three that dump a value are only callable when the host turns debugging on. A template that leaks its data
 * model into a page is a mistake in production even when it is exactly what you want while writing the thing.
 *
 * @author Deniss Larka
 */
final class TypeFunctions {

    private TypeFunctions() {}

    static void registerInto(FunctionRegistry registry) {
        Functions.define(registry, "is_array", 1, (env, a) -> PhpBool.of(a.at(0) instanceof PhpArray));
        Functions.define(registry, "is_string", 1, (env, a) -> PhpBool.of(a.at(0) instanceof PhpString));
        Functions.define(registry, "is_int", 1, (env, a) -> PhpBool.of(a.at(0) instanceof PhpInt));
        Functions.alias(registry, "is_int", "is_integer");
        Functions.alias(registry, "is_int", "is_long");
        Functions.define(registry, "is_float", 1, (env, a) -> PhpBool.of(a.at(0) instanceof PhpFloat));
        Functions.alias(registry, "is_float", "is_double");
        Functions.define(registry, "is_bool", 1, (env, a) -> PhpBool.of(a.at(0) instanceof PhpBool));
        Functions.define(registry, "is_null", 1, (env, a) -> PhpBool.of(a.at(0) instanceof PhpNull));
        Functions.define(registry, "is_object", 1, (env, a) -> PhpBool.of(a.at(0) instanceof PhpClosure));
        Functions.define(registry, "is_iterable", 1, (env, a) -> PhpBool.of(a.at(0) instanceof PhpArray));
        Functions.define(registry, "is_numeric", 1, (env, a) -> PhpBool.of(isNumeric(a.at(0))));
        Functions.define(registry, "is_callable", 1, (env, a) -> PhpBool.of(a.at(0) instanceof PhpClosure));

        Functions.define(registry, "intval", 1, 2, (env, a) -> PhpInt.of(a.at(0).toInt()));
        Functions.define(
                registry, "floatval", 1, (env, a) -> PhpFloat.of(a.at(0).toFloat()));
        Functions.alias(registry, "floatval", "doubleval");
        Functions.define(registry, "strval", 1, (env, a) -> PhpString.of(a.at(0).toStr()));
        Functions.define(registry, "boolval", 1, (env, a) -> a.at(0).toBool());
        Functions.define(
                registry, "gettype", 1, (env, a) -> PhpString.of(a.at(0).typeName()));
        Functions.define(registry, "get_debug_type", 1, (env, a) -> PhpString.of(debugType(a.at(0))));

        Functions.define(registry, "print_r", 1, 2, (env, a) -> {
            requireDebugging(env, "print_r");
            String dumped = dump(a.at(0), 0);
            if (a.flag(1)) {
                return PhpString.of(dumped);
            }
            env.write(dumped);
            return PhpBool.TRUE;
        });
        Functions.define(registry, "var_export", 1, 2, (env, a) -> {
            requireDebugging(env, "var_export");
            String dumped = dump(a.at(0), 0);
            if (a.flag(1)) {
                return PhpString.of(dumped);
            }
            env.write(dumped);
            return PhpNull.NULL;
        });
        Functions.define(registry, "var_dump", 1, Integer.MAX_VALUE, (env, a) -> {
            requireDebugging(env, "var_dump");
            for (PhpValue value : a.from(0)) {
                env.write(value.typeName() + "(" + dump(value, 0) + ")\n");
            }
            return PhpNull.NULL;
        });
    }

    private static void requireDebugging(com.druvu.web.php.internal.runtime.Env env, String name) {
        if (!env.config().debugFunctions()) {
            throw new PhpProcessingException(name + "() is only available when the host turns the debug functions on");
        }
    }

    private static boolean isNumeric(PhpValue value) {
        if (value instanceof PhpInt || value instanceof PhpFloat) {
            return true;
        }
        return value instanceof PhpString text && NumericStrings.isNumeric(text.value());
    }

    /** PHP 8's short type names, which are the ones worth showing a template author. */
    private static String debugType(PhpValue value) {
        return switch (value.typeName()) {
            case "NULL" -> "null";
            case "boolean" -> "bool";
            case "integer" -> "int";
            case "double" -> "float";
            case "object" -> "Closure";
            default -> value.typeName();
        };
    }

    private static String dump(PhpValue value, int depth) {
        if (!(value instanceof PhpArray array)) {
            return value instanceof PhpString text ? "'" + text.value() + "'" : value.toStr();
        }
        String indent = "    ".repeat(depth + 1);
        StringBuilder out = new StringBuilder("[\n");
        for (Map.Entry<ArrayKey, PhpValue> entry : array.entries().entrySet()) {
            out.append(indent)
                    .append(StringFunctions.keyAsValue(entry.getKey()).toStr())
                    .append(" => ")
                    .append(dump(entry.getValue(), depth + 1))
                    .append('\n');
        }
        return out.append("    ".repeat(depth)).append(']').toString();
    }
}
