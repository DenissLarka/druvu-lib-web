package com.druvu.web.php.internal.builtin;

import com.druvu.web.php.internal.runtime.FunctionRegistry;

/**
 * Every function a template can call, gathered in one place.
 *
 * <p>Grouped the way the functions are used rather than the way PHP's manual files them — what puts a value on a page,
 * what works on text, what works on lists — so that adding one has an obvious home.
 *
 * @author Deniss Larka
 */
public final class Builtins {

    private Builtins() {}

    /** A registry holding the whole library. */
    public static FunctionRegistry registry() {
        FunctionRegistry registry = FunctionRegistry.empty();
        OutputFunctions.registerInto(registry);
        StringFunctions.registerInto(registry);
        ArrayFunctions.registerInto(registry);
        TypeFunctions.registerInto(registry);
        DateFunctions.registerInto(registry);
        MathFunctions.registerInto(registry);
        WebFunctions.registerInto(registry);
        return registry;
    }
}
