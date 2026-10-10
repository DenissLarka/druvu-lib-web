package com.druvu.php.internal.builtin;

import com.druvu.php.internal.runtime.FunctionRegistry;
import com.druvu.php.internal.runtime.Html;
import com.druvu.php.internal.value.ArrayKey;
import com.druvu.php.internal.value.PhpArray;
import com.druvu.php.internal.value.PhpNull;
import com.druvu.php.internal.value.PhpString;
import com.druvu.php.internal.value.PhpValue;
import com.druvu.php.internal.value.SafeString;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Everything to do with getting a value safely onto a page.
 *
 * <p>The functions here are the ones that decide what is markup and what is text, so this is where {@link SafeString}
 * is handed out. {@code raw()} is the only one a template author reaches for deliberately; the rest produce markup as
 * their whole purpose and would be nonsense escaped.
 *
 * @author Deniss Larka
 */
final class OutputFunctions {

    private OutputFunctions() {}

    static void registerInto(FunctionRegistry registry) {
        // The opt-out. Short, and easy to search a code base for, which is the point of it being a function.
        Functions.define(registry, "raw", 1, (env, args) -> SafeString.of(args.string(0)));

        Functions.define(registry, "htmlspecialchars", 1, 2, (env, args) -> SafeString.of(Html.escape(args.string(0))));
        Functions.define(registry, "htmlentities", 1, 2, (env, args) -> SafeString.of(Html.escapeAll(args.string(0))));
        Functions.define(
                registry, "htmlspecialchars_decode", 1, 2, (env, args) -> PhpString.of(Html.unescape(args.string(0))));
        Functions.alias(registry, "htmlspecialchars_decode", "html_entity_decode");

        // PHP puts the tag before the newline and keeps the newline.
        Functions.define(
                registry,
                "nl2br",
                1,
                2,
                (env, args) -> SafeString.of(args.string(0).replaceAll("(\\r\\n|\\n|\\r)", "<br />$1")));

        Functions.define(
                registry,
                "strip_tags",
                1,
                2,
                (env, args) -> PhpString.of(args.string(0).replaceAll("<[^>]*>", "")));

        Functions.define(
                registry,
                "urlencode",
                1,
                (env, args) -> PhpString.of(URLEncoder.encode(args.string(0), StandardCharsets.UTF_8)));
        Functions.define(
                registry,
                "rawurlencode",
                1,
                (env, args) -> PhpString.of(URLEncoder.encode(args.string(0), StandardCharsets.UTF_8)
                        .replace("+", "%20")));

        Functions.define(registry, "http_build_query", 1, 2, (env, args) -> PhpString.of(query(args.array(0))));

        Functions.define(
                registry,
                "json_encode",
                1,
                2,
                (env, args) -> PhpString.of(Json.encode(args.at(0), (int) args.integerOr(1, 0))));
    }

    private static String query(PhpArray array) {
        StringBuilder query = new StringBuilder();
        appendQuery(query, null, array);
        return query.toString();
    }

    /** As PHP does it: a nested array becomes {@code parent[child]=value} pairs, and a null entry is left out. */
    private static void appendQuery(StringBuilder query, String prefix, PhpArray array) {
        for (Map.Entry<ArrayKey, PhpValue> entry : array.entries().entrySet()) {
            String name = prefix == null ? keyOf(entry.getKey()) : prefix + "[" + keyOf(entry.getKey()) + "]";
            PhpValue value = entry.getValue();
            if (value instanceof PhpNull) {
                continue;
            }
            if (value instanceof PhpArray nested) {
                appendQuery(query, name, nested);
                continue;
            }
            if (!query.isEmpty()) {
                query.append('&');
            }
            query.append(URLEncoder.encode(name, StandardCharsets.UTF_8))
                    .append('=')
                    .append(URLEncoder.encode(value.toStr(), StandardCharsets.UTF_8));
        }
    }

    private static String keyOf(ArrayKey key) {
        return key instanceof ArrayKey.IntKey index ? Long.toString(index.value()) : ((ArrayKey.StringKey) key).value();
    }
}
