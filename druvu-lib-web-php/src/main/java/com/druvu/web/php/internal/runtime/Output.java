package com.druvu.web.php.internal.runtime;

import com.druvu.web.php.internal.value.PhpString;
import com.druvu.web.php.internal.value.PhpValue;

/**
 * The one place a value becomes part of the page.
 *
 * <p>This is where the engine's single deliberate departure from PHP lives: a value on its way out is HTML-escaped
 * unless it says it is already safe. In PHP the default is the other way round, and every template that forgets
 * {@code htmlspecialchars} is a hole; here forgetting is safe and {@code raw()} is the thing you have to write on
 * purpose — and therefore the thing that shows up in a search.
 *
 * <p>Escaping belongs to the act of writing, not to the value: a string that has been through here is not marked
 * afterwards, and concatenating a safe string with anything else produces an ordinary one.
 *
 * @author Deniss Larka
 */
public final class Output {

    private Output() {}

    public static void write(Env env, PhpValue value) {
        String text = value.toStr();
        if (!env.config().escapeOutput() || isAlreadySafe(value)) {
            env.write(text);
            return;
        }
        env.write(Html.escape(text));
    }

    private static boolean isAlreadySafe(PhpValue value) {
        return value instanceof PhpString text && text.isSafe();
    }
}
