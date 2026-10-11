package com.druvu.web.example.mcp;

import com.druvu.json.Json;
import com.druvu.json.JsonArrayBuilder;
import com.druvu.json.JsonBuilder;
import com.druvu.json.JsonObject;
import com.druvu.json.JsonValue;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * The tools this server offers: what {@code tools/list} describes and what {@code tools/call} runs. A tool's own
 * failure is a result with {@code isError}, as the protocol wants; an unknown tool is the caller's error and is
 * reported by the server.
 *
 * @author Deniss Larka <br>
 *     on 11 Oct 2026
 */
final class Tools {

    static final String ECHO = "echo";
    static final String TIME_NOW = "time_now";

    private Tools() {}

    /** The {@code tools} array of a {@code tools/list} result. */
    static JsonArrayBuilder<?> list() {
        return Json.array()
                .add(tool(ECHO, "Echo", "Returns the text it is given.", schema("text", "The text to echo", true)))
                .add(tool(
                        TIME_NOW,
                        "Current time",
                        "The current time in a zone as ISO-8601 with offset; UTC when no zone is given.",
                        schema("zone", "An IANA zone such as Europe/Zurich", false)));
    }

    /** The result of calling a tool, or empty for a tool this server does not have. */
    static Optional<JsonValue> call(String name, JsonObject arguments) {
        return switch (name) {
            case ECHO -> Optional.of(text(arguments.string("text", ""), false));
            case TIME_NOW -> Optional.of(timeNow(arguments.string("zone", "")));
            default -> Optional.empty();
        };
    }

    private static JsonValue timeNow(String zone) {
        try {
            ZoneId zoneId = zone.isEmpty() ? ZoneOffset.UTC : ZoneId.of(zone);
            return text(ZonedDateTime.now(zoneId).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME), false);
        } catch (DateTimeException e) {
            return text("Unknown zone: " + zone, true);
        }
    }

    /** A tool result with one text content block: the protocol's shape, written down, with the two values that move. */
    private static JsonValue text(String value, boolean isError) {
        return Json.fill("""
                {
                    "content": [ { "type": "text", "text": ? } ],
                    "isError": ?
                }
                """, value, isError);
    }

    private static JsonBuilder tool(String name, String title, String description, JsonBuilder inputSchema) {
        return Json.object()
                .add("name", name)
                .add("title", title)
                .add("description", description)
                .add("inputSchema", inputSchema);
    }

    /** A JSON schema for an object with one string property: written as bodies, because one member is conditional. */
    private static JsonBuilder schema(String property, String description, boolean required) {
        return Json.object(obj -> {
            obj.add("type", "object");
            obj.addObject(
                    "properties",
                    props -> props.addObject(
                            property, prop -> prop.add("type", "string").add("description", description)));
            if (required) {
                obj.addArray("required", req -> req.add(property));
            }
        });
    }
}
