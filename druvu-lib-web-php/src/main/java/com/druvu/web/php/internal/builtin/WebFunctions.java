package com.druvu.web.php.internal.builtin;

import com.druvu.web.php.internal.PhpProcessingException;
import com.druvu.web.php.internal.runtime.Env;
import com.druvu.web.php.internal.runtime.FunctionRegistry;
import com.druvu.web.php.internal.value.SafeString;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;

/**
 * The three functions that know where things live: the context path, a link, a WebJar asset.
 *
 * <p>They answer with {@link SafeString} because a URL is not text on a page — escaping one would put {@code &amp;} in
 * an href.
 *
 * <p>The WebJar index is built once at class-loading time, and a failure to build it is thrown <em>then</em> rather
 * than swallowed. The previous version caught the {@link IOException} and carried on with an empty index, which turned
 * a broken classpath into "asset not found" on every page — a much harder thing to diagnose than a startup failure.
 *
 * @author Deniss Larka
 */
final class WebFunctions {

    private static final String WEBJARS_PREFIX = "META-INF/resources/webjars/";
    private static final String RESOURCES_PREFIX = "META-INF/resources/";

    private static final Map<String, String> WEBJAR_INDEX = buildIndex();

    private WebFunctions() {}

    static void registerInto(FunctionRegistry registry) {
        Functions.define(registry, "context", 0, (env, a) -> SafeString.of(contextPath(env, "context")));
        Functions.define(registry, "link", 1, (env, a) -> SafeString.of(contextPath(env, "link") + "/" + a.string(0)));
        Functions.define(registry, "webjar", 1, (env, a) -> {
            String asset = WEBJAR_INDEX.get(a.string(0));
            if (asset == null) {
                throw new PhpProcessingException("webjar(): no such asset on the classpath: " + a.string(0));
            }
            return SafeString.of(contextPath(env, "webjar") + "/" + asset);
        });
    }

    private static String contextPath(Env env, String function) {
        HttpServletRequest request = env.request();
        if (request == null) {
            throw new PhpProcessingException(function + "() needs a request, and this render has none");
        }
        return request.getContextPath();
    }

    private static Map<String, String> buildIndex() {
        Map<String, String> index = new HashMap<>();
        try {
            Enumeration<URL> roots = WebFunctions.class.getClassLoader().getResources("META-INF/resources/webjars");
            while (roots.hasMoreElements()) {
                // Resolved through the URL connection rather than by parsing the URL text, so that the
                // jar:nested: URLs an executable jar uses for bundled webjars resolve as well as plain ones.
                if (roots.nextElement().openConnection() instanceof JarURLConnection connection) {
                    connection.setUseCaches(false);
                    try (JarFile jar = connection.getJarFile()) {
                        scan(jar, index);
                    }
                }
            }
        } catch (IOException unreadableClasspath) {
            throw new IllegalStateException("Could not index the WebJars on the classpath", unreadableClasspath);
        }
        return Map.copyOf(index);
    }

    private static void scan(JarFile jar, Map<String, String> index) {
        jar.stream()
                .filter(entry -> !entry.isDirectory() && entry.getName().startsWith(WEBJARS_PREFIX))
                .forEach(entry -> {
                    String name = entry.getName();
                    index.putIfAbsent(
                            name.substring(name.lastIndexOf('/') + 1), name.substring(RESOURCES_PREFIX.length()));
                });
    }
}
