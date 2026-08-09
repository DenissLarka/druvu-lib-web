package com.druvu.web.php.internal.builtin;

import com.druvu.web.php.internal.value.ArrayKey;
import com.druvu.web.php.internal.value.PhpArray;
import com.druvu.web.php.internal.value.PhpBool;
import com.druvu.web.php.internal.value.PhpFloat;
import com.druvu.web.php.internal.value.PhpInt;
import com.druvu.web.php.internal.value.PhpNull;
import com.druvu.web.php.internal.value.PhpString;
import com.druvu.web.php.internal.value.PhpValue;
import java.util.Map;

/**
 * JSON, written by hand rather than pulled in.
 *
 * <p>A library would be one more dependency in a published artifact for something this small, and the part that matters
 * is not the syntax but PHP's two peculiarities: an array becomes a JSON array only when its keys are exactly 0..n-1,
 * and the default output escapes slashes and non-ASCII so the result is safe to drop into a script tag.
 *
 * @author Deniss Larka
 */
final class Json {

    static final int HEX_TAG = 1;
    static final int HEX_AMP = 2;
    static final int HEX_APOS = 4;
    static final int HEX_QUOT = 8;
    static final int PRETTY_PRINT = 128;
    static final int UNESCAPED_SLASHES = 64;
    static final int UNESCAPED_UNICODE = 256;

    private final int flags;

    private Json(int flags) {
        this.flags = flags;
    }

    static String encode(PhpValue value, int flags) {
        StringBuilder out = new StringBuilder();
        new Json(flags).write(out, value, 0);
        return out.toString();
    }

    private void write(StringBuilder out, PhpValue value, int depth) {
        if (value instanceof PhpNull) {
            out.append("null");
        } else if (value instanceof PhpBool flag) {
            out.append(flag.value());
        } else if (value instanceof PhpInt number) {
            out.append(number.value());
        } else if (value instanceof PhpFloat number) {
            out.append(number.toStr());
        } else if (value instanceof PhpString text) {
            writeString(out, text.value());
        } else if (value instanceof PhpArray array) {
            writeArray(out, array, depth);
        } else {
            out.append("null");
        }
    }

    private void writeArray(StringBuilder out, PhpArray array, int depth) {
        boolean asList = isList(array);
        out.append(asList ? '[' : '{');
        boolean first = true;
        for (Map.Entry<ArrayKey, PhpValue> entry : array.entries().entrySet()) {
            if (!first) {
                out.append(',');
            }
            first = false;
            newLine(out, depth + 1);
            if (!asList) {
                writeString(out, keyOf(entry.getKey()));
                out.append(':');
                if (isSet(PRETTY_PRINT)) {
                    out.append(' ');
                }
            }
            write(out, entry.getValue(), depth + 1);
        }
        if (!first) {
            newLine(out, depth);
        }
        out.append(asList ? ']' : '}');
    }

    /** PHP writes a JSON array only when the keys are exactly the integers 0 to n-1, in that order. */
    private static boolean isList(PhpArray array) {
        long expected = 0;
        for (ArrayKey key : array.entries().keySet()) {
            if (!(key instanceof ArrayKey.IntKey index) || index.value() != expected++) {
                return false;
            }
        }
        return true;
    }

    private static String keyOf(ArrayKey key) {
        return key instanceof ArrayKey.IntKey index ? Long.toString(index.value()) : ((ArrayKey.StringKey) key).value();
    }

    private void newLine(StringBuilder out, int depth) {
        if (!isSet(PRETTY_PRINT)) {
            return;
        }
        out.append('\n');
        out.append("    ".repeat(depth));
    }

    private void writeString(StringBuilder out, String text) {
        out.append('"');
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"' -> out.append(isSet(HEX_QUOT) ? "\\u0022" : "\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                case '<' -> out.append(isSet(HEX_TAG) ? "\\u003C" : "<");
                case '>' -> out.append(isSet(HEX_TAG) ? "\\u003E" : ">");
                case '&' -> out.append(isSet(HEX_AMP) ? "\\u0026" : "&");
                case '\'' -> out.append(isSet(HEX_APOS) ? "\\u0027" : "'");
                case '/' -> out.append(isSet(UNESCAPED_SLASHES) ? "/" : "\\/");
                default -> writeOther(out, c);
            }
        }
        out.append('"');
    }

    private void writeOther(StringBuilder out, char c) {
        if (c < 0x20) {
            out.append(String.format(java.util.Locale.ROOT, "\\u%04X", (int) c));
        } else if (c > 127 && !isSet(UNESCAPED_UNICODE)) {
            out.append(String.format(java.util.Locale.ROOT, "\\u%04X", (int) c));
        } else {
            out.append(c);
        }
    }

    private boolean isSet(int flag) {
        return (flags & flag) != 0;
    }
}
