package com.druvu.web.php.internal.builtin;

import com.druvu.web.php.internal.runtime.Env;
import com.druvu.web.php.internal.runtime.FunctionRegistry;
import com.druvu.web.php.internal.value.PhpValue;

/**
 * How a built-in is written down.
 *
 * <p>One line per function, its arity beside its name, so a wave of them reads as a list of what exists rather than as
 * a hundred lines of argument checking.
 *
 * @author Deniss Larka
 */
final class Functions {

    /** What a built-in does. */
    @FunctionalInterface
    interface Body {
        PhpValue call(Env env, Arguments arguments);
    }

    private Functions() {}

    static void define(FunctionRegistry registry, String name, int least, int most, Body body) {
        registry.register(name, (env, values) -> body.call(env, new Arguments(name, values, least, most)));
    }

    /** A function taking exactly the number of arguments given. */
    static void define(FunctionRegistry registry, String name, int arity, Body body) {
        define(registry, name, arity, arity, body);
    }

    /** Another name for a function that already exists. */
    static void alias(FunctionRegistry registry, String existing, String alternative) {
        registry.register(alternative, registry.find(existing));
    }
}
