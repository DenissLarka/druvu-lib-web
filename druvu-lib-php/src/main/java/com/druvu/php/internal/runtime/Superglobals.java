package com.druvu.php.internal.runtime;

import com.druvu.php.HostRequest;
import com.druvu.php.internal.value.ArrayKey;
import com.druvu.php.internal.value.PhpArray;
import com.druvu.php.internal.value.PhpString;
import java.util.Map;

/**
 * PHP's request arrays, filled from the {@link HostRequest} the host hands in.
 *
 * <p>Read-only in the way that matters: they are ordinary arrays holding copies, so a template can read and even
 * rewrite its own copy without any of it reaching the request.
 *
 * <p>{@code $_SESSION}, {@code $_FILES}, {@code $_ENV} and {@code $GLOBALS} are absent and are meant to be. Sessions
 * and uploads are the application's business, and a template that reads the environment is a template that can leak it.
 *
 * @author Deniss Larka
 */
public final class Superglobals {

    private Superglobals() {}

    /** Binds {@code $_GET}, {@code $_POST}, {@code $_REQUEST}, {@code $_COOKIE} and {@code $_SERVER}. */
    public static void bindInto(Env env, HostRequest request) {
        if (request == null) {
            return;
        }
        PhpArray get = fromMap(request.queryParameters());
        PhpArray post = fromMap(request.formParameters());
        PhpArray all = get.copy();
        post.entries().forEach(all::put);
        env.setVariable("_GET", get);
        env.setVariable("_POST", post);
        env.setVariable("_REQUEST", all);
        env.setVariable("_COOKIE", fromMap(request.cookies()));
        env.setVariable("_SERVER", fromMap(request.serverVariables()));
    }

    private static PhpArray fromMap(Map<String, String> values) {
        PhpArray array = PhpArray.empty();
        values.forEach((name, value) -> array.put(ArrayKey.of(name), PhpString.of(value == null ? "" : value)));
        return array;
    }
}
