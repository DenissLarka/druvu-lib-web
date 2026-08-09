package com.druvu.web.php.internal.value;

import java.util.Objects;

/**
 * A string that is already fit for HTML output and so passes through {@code echo} unescaped.
 *
 * <p>This is the escape hatch for the engine's escape-by-default policy. Only two things produce one: the {@code raw()}
 * builtin, which template authors call deliberately, and builtins whose whole purpose is to emit markup. Concatenating
 * a safe string with anything else yields a plain string — safety does not spread.
 *
 * @author Deniss Larka
 */
public final class SafeString extends PhpString {

    private SafeString(String value) {
        super(value);
    }

    public static SafeString of(String value) {
        return new SafeString(Objects.requireNonNull(value, "value"));
    }

    @Override
    public boolean isSafe() {
        return true;
    }

    @Override
    public String toString() {
        return "safe'" + value() + "'";
    }
}
