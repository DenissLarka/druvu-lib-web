package com.druvu.web.php.internal.runtime;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The functions a template may call.
 *
 * <p>Deliberately not an extension point. There is no way for a host to add one, and there is not meant to be: the set
 * of functions a layout language needs is closed enough to be shipped, and a plug-in surface here would be permanent
 * public API bought for a case nobody has asked for. A missing function is a bug report, not a hook.
 *
 * <p>Names are matched without regard to case, as PHP matches them.
 *
 * @author Deniss Larka
 */
public final class FunctionRegistry {

    private final Map<String, PhpFunction> functions = new HashMap<>();
    private final java.util.Set<String> writeBack = new java.util.HashSet<>();

    public static FunctionRegistry empty() {
        return new FunctionRegistry();
    }

    public void register(String name, PhpFunction function) {
        functions.put(normalise(name), function);
    }

    /**
     * A function that rearranges its first argument rather than answering with something new — the sorts, and nothing
     * else. The call site puts the result back where it came from, which is the dialect's only reference.
     */
    public void registerInPlace(String name, PhpFunction function) {
        register(name, function);
        writeBack.add(normalise(name));
    }

    /** Whether this function changes what its first argument named. */
    public boolean writesBackFirstArgument(String name) {
        return writeBack.contains(normalise(name));
    }

    /** The function with this name, or null when there is none. */
    public PhpFunction find(String name) {
        return functions.get(normalise(name));
    }

    public boolean has(String name) {
        return functions.containsKey(normalise(name));
    }

    public int size() {
        return functions.size();
    }

    private static String normalise(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
