package com.druvu.web.php.internal.builtin;

import com.druvu.web.php.internal.PhpProcessingException;
import com.druvu.web.php.internal.runtime.FunctionRegistry;
import com.druvu.web.php.internal.value.PhpInt;
import com.druvu.web.php.internal.value.PhpString;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.IsoFields;
import java.util.Locale;

/**
 * Showing a moment in time, which is all a layout ever needs to do with one.
 *
 * <p>Formatting only. Parsing a date out of text is the host's job — it knows the locale, the timezone the user is in
 * and where the value came from, and a template does not.
 *
 * <p><strong>Times are shown in UTC.</strong> A layout language has no business guessing a timezone: a host wanting
 * local time should convert before handing the value over. Named rather than hidden, because the alternative is a page
 * that is quietly an hour wrong twice a year.
 *
 * @author Deniss Larka
 */
final class DateFunctions {

    private DateFunctions() {}

    static void registerInto(FunctionRegistry registry) {
        Functions.define(registry, "date", 1, 2, (env, a) -> {
            long epochSeconds = a.has(1) ? a.integer(1) : System.currentTimeMillis() / 1000L;
            return PhpString.of(format(a.string(0), epochSeconds));
        });
        Functions.define(registry, "time", 0, (env, a) -> PhpInt.of(System.currentTimeMillis() / 1000L));
        Functions.define(registry, "checkdate", 3, (env, a) -> {
            try {
                java.time.LocalDate.of((int) a.integer(2), (int) a.integer(0), (int) a.integer(1));
                return com.druvu.web.php.internal.value.PhpBool.TRUE;
            } catch (java.time.DateTimeException notADate) {
                return com.druvu.web.php.internal.value.PhpBool.FALSE;
            }
        });
    }

    static String format(String pattern, long epochSeconds) {
        ZonedDateTime when = Instant.ofEpochSecond(epochSeconds).atZone(ZoneOffset.UTC);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '\\' && i + 1 < pattern.length()) {
                out.append(pattern.charAt(++i));
            } else {
                out.append(letter(c, when, epochSeconds));
            }
        }
        return out.toString();
    }

    private static String letter(char c, ZonedDateTime when, long epochSeconds) {
        return switch (c) {
            case 'd' -> two(when.getDayOfMonth());
            case 'j' -> String.valueOf(when.getDayOfMonth());
            case 'D' -> when.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            case 'l' -> when.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            case 'N' -> String.valueOf(when.getDayOfWeek().getValue());
            case 'w' ->
                String.valueOf(
                        when.getDayOfWeek() == DayOfWeek.SUNDAY
                                ? 0
                                : when.getDayOfWeek().getValue());
            case 'z' -> String.valueOf(when.getDayOfYear() - 1);
            case 'W' -> two(when.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
            case 'm' -> two(when.getMonthValue());
            case 'n' -> String.valueOf(when.getMonthValue());
            case 'M' -> when.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            case 'F' -> when.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            case 't' -> String.valueOf(when.toLocalDate().lengthOfMonth());
            case 'L' -> when.toLocalDate().isLeapYear() ? "1" : "0";
            case 'Y' -> String.valueOf(when.getYear());
            case 'y' -> two(when.getYear() % 100);
            case 'a' -> when.getHour() < 12 ? "am" : "pm";
            case 'A' -> when.getHour() < 12 ? "AM" : "PM";
            case 'g' -> String.valueOf(twelveHour(when));
            case 'G' -> String.valueOf(when.getHour());
            case 'h' -> two(twelveHour(when));
            case 'H' -> two(when.getHour());
            case 'i' -> two(when.getMinute());
            case 's' -> two(when.getSecond());
            case 'v' -> String.format(Locale.ROOT, "%03d", when.get(ChronoField.MILLI_OF_SECOND));
            case 'u' -> String.format(Locale.ROOT, "%06d", when.get(ChronoField.MICRO_OF_SECOND));
            case 'e', 'T' -> "UTC";
            case 'O' -> "+0000";
            case 'P', 'p' -> "+00:00";
            case 'Z' -> "0";
            case 'U' -> String.valueOf(epochSeconds);
            case 'c' -> format("Y-m-d\\TH:i:sP", epochSeconds);
            case 'r' -> format("D, d M Y H:i:s O", epochSeconds);
            default -> String.valueOf(c);
        };
    }

    private static int twelveHour(ZonedDateTime when) {
        int hour = when.getHour() % 12;
        return hour == 0 ? 12 : hour;
    }

    private static String two(int value) {
        return value < 10 ? "0" + value : String.valueOf(value);
    }

    static void refuseParsing() {
        throw new PhpProcessingException("Parsing dates is the host's job; pass the template an epoch second");
    }
}
