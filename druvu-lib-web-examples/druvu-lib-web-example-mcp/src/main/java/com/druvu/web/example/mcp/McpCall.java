package com.druvu.web.example.mcp;

import com.druvu.json.Json;
import com.druvu.json.JsonObject;
import com.druvu.json.JsonValue;
import java.util.Objects;

/**
 * A JSON-RPC request that wants an answer: its id, which the answer carries back, the method, and the params, an empty
 * object when the request had none.
 *
 * @author Deniss Larka <br>
 *     on 11 Oct 2026
 */
record McpCall(JsonValue id, String method, JsonObject params) implements Parsed {

    /** The params of a request that sent none. */
    static final JsonObject NO_PARAMS = Json.object().build().asObject();

    McpCall {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(params, "params");
    }

    /** A string parameter, or empty text when absent. */
    String param(String name) {
        return params.string(name, "");
    }

    /** An object parameter, or an empty object when absent. */
    JsonObject object(String name) {
        return params.findObject(name).orElse(NO_PARAMS);
    }
}
