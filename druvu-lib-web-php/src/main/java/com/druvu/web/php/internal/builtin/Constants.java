package com.druvu.web.php.internal.builtin;

import com.druvu.web.php.internal.value.PhpInt;
import com.druvu.web.php.internal.value.PhpString;
import com.druvu.web.php.internal.value.PhpValue;
import java.util.Locale;
import java.util.Map;

/**
 * The named constants a template may use.
 *
 * <p>A closed set, resolved while parsing rather than looked up while rendering — a template naming a constant that
 * does not exist should be told so when it loads, not halfway down a page.
 *
 * @author Deniss Larka
 */
public final class Constants {

    private static final Map<String, PhpValue> BY_NAME = Map.ofEntries(
            Map.entry("php_eol", PhpString.of("\n")),
            Map.entry("php_int_max", PhpInt.of(Long.MAX_VALUE)),
            Map.entry("php_int_min", PhpInt.of(Long.MIN_VALUE)),
            Map.entry("php_int_size", PhpInt.of(8L)),
            Map.entry("ent_quotes", PhpInt.of(3L)),
            Map.entry("ent_html5", PhpInt.of(48L)),
            Map.entry("ent_substitute", PhpInt.of(8L)),
            Map.entry("ent_compat", PhpInt.of(2L)),
            Map.entry("ent_noquotes", PhpInt.of(0L)),
            Map.entry("json_hex_tag", PhpInt.of(1L)),
            Map.entry("json_hex_amp", PhpInt.of(2L)),
            Map.entry("json_hex_apos", PhpInt.of(4L)),
            Map.entry("json_hex_quot", PhpInt.of(8L)),
            Map.entry("json_unescaped_slashes", PhpInt.of(64L)),
            Map.entry("json_pretty_print", PhpInt.of(128L)),
            Map.entry("json_unescaped_unicode", PhpInt.of(256L)),
            Map.entry("str_pad_right", PhpInt.of(1L)),
            Map.entry("str_pad_left", PhpInt.of(0L)),
            Map.entry("str_pad_both", PhpInt.of(2L)),
            Map.entry("sort_regular", PhpInt.of(0L)),
            Map.entry("sort_numeric", PhpInt.of(1L)),
            Map.entry("sort_string", PhpInt.of(2L)),
            Map.entry("count_normal", PhpInt.of(0L)),
            Map.entry("count_recursive", PhpInt.of(1L)),
            Map.entry("array_filter_use_key", PhpInt.of(2L)),
            Map.entry("array_filter_use_both", PhpInt.of(1L)),
            Map.entry("php_round_half_up", PhpInt.of(1L)),
            Map.entry("php_round_half_down", PhpInt.of(2L)),
            Map.entry("php_round_half_even", PhpInt.of(3L)),
            Map.entry("php_round_half_odd", PhpInt.of(4L)),
            Map.entry("m_pi", com.druvu.web.php.internal.value.PhpFloat.of(Math.PI)),
            Map.entry("m_e", com.druvu.web.php.internal.value.PhpFloat.of(Math.E)));

    private Constants() {}

    /** The value of a constant, or null when the dialect has no constant by that name. */
    public static PhpValue find(String name) {
        return BY_NAME.get(name.toLowerCase(Locale.ROOT));
    }
}
