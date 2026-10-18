package com.druvu.web.core.handlers;

import java.nio.charset.StandardCharsets;

/**
 * The {@code Content-Disposition} header of a download: the name in quotes, in ASCII every browser reads, and for a
 * name that does not fit that, its UTF-8 form as well (RFC 6266 with the encoding of RFC 5987), which browsers prefer.
 *
 * @author Deniss Larka <br>
 *     on 18 Oct 2026
 */
final class ContentDisposition {

    /** The characters RFC 5987 lets through as they are, besides letters and digits. */
    private static final String PLAIN_MARKS = "!#$&+-.^_`|~";

    private ContentDisposition() {}

    static String attachment(String filename) {
        String ascii = asciiFallback(filename);
        if (ascii.equals(filename)) {
            return "attachment; filename=\"" + ascii + "\"";
        }
        return "attachment; filename=\"" + ascii + "\"; filename*=UTF-8''" + percentEncoded(filename);
    }

    /** Printable ASCII kept; a quote, a backslash or any other character becomes an underscore. */
    private static String asciiFallback(String filename) {
        StringBuilder out = new StringBuilder(filename.length());
        filename.codePoints().forEach(c -> out.append(isPlainAscii(c) ? (char) c : '_'));
        return out.toString();
    }

    private static boolean isPlainAscii(int c) {
        return c >= 0x20 && c < 0x7F && c != '"' && c != '\\';
    }

    /** The UTF-8 bytes, with letters, digits and {@value #PLAIN_MARKS} as they are and every other byte as %XX. */
    private static String percentEncoded(String filename) {
        StringBuilder out = new StringBuilder();
        for (byte b : filename.getBytes(StandardCharsets.UTF_8)) {
            int c = b & 0xFF;
            if (isAsciiLetterOrDigit(c) || PLAIN_MARKS.indexOf(c) >= 0) {
                out.append((char) c);
            } else {
                out.append(String.format("%%%02X", c));
            }
        }
        return out.toString();
    }

    private static boolean isAsciiLetterOrDigit(int c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9');
    }
}
