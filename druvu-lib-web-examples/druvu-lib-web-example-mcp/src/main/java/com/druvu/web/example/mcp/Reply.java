package com.druvu.web.example.mcp;

import com.druvu.json.Json;
import com.druvu.json.JsonBuilder;
import com.druvu.json.JsonObjectBuilder;
import com.druvu.json.JsonValue;
import com.druvu.web.api.handlers.HttpResponse;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * What goes back over HTTP: a status, headers, and a JSON body when there is one. The JSON-RPC envelopes, a result or
 * an error with its code, are made here, so the codes live with the shape they belong to.
 *
 * @author Deniss Larka <br>
 *     on 11 Oct 2026
 */
record Reply(int status, Map<String, String> headers, Optional<String> json) implements Parsed {

    private static final int PARSE_ERROR = -32700;
    private static final int INVALID_REQUEST = -32600;
    private static final int METHOD_NOT_FOUND = -32601;
    private static final int INVALID_PARAMS = -32602;

    Reply {
        headers = Map.copyOf(headers);
        Objects.requireNonNull(json, "json");
    }

    /** 202: a notification taken, nothing to say back. */
    static Reply accepted() {
        return new Reply(202, Map.of(), Optional.empty());
    }

    /** A bare status, 403 say. */
    static Reply status(int status) {
        return new Reply(status, Map.of(), Optional.empty());
    }

    /** 405, naming what is allowed. */
    static Reply methodNotAllowed(String allowed) {
        return new Reply(405, Map.of("Allow", allowed), Optional.empty());
    }

    /** 400 with a JSON-RPC error body: the transport refused the request before reading it as a call. */
    static Reply badRequest(String message) {
        return new Reply(
                400,
                Map.of(),
                Optional.of(error(Optional.empty(), INVALID_REQUEST, message).toJson()));
    }

    static Reply parseError() {
        return json(error(Optional.empty(), PARSE_ERROR, "Parse error"));
    }

    static Reply invalidRequest(Optional<JsonValue> id, String what) {
        return json(error(id, INVALID_REQUEST, "Invalid Request: " + what));
    }

    static Reply methodNotFound(JsonValue id, String method) {
        return json(error(Optional.of(id), METHOD_NOT_FOUND, "Method not found: " + method));
    }

    static Reply invalidParams(JsonValue id, String what) {
        return json(error(Optional.of(id), INVALID_PARAMS, what));
    }

    /** 200 with a JSON-RPC result for the call with this id. */
    static Reply result(JsonValue id, JsonBuilder result) {
        return json(envelope(Optional.of(id)).add("result", result));
    }

    /** 200 with a JSON-RPC result that was parsed or filled rather than built. */
    static Reply result(JsonValue id, JsonValue result) {
        return json(envelope(Optional.of(id)).add("result", result));
    }

    /** Writes this reply: the headers, then the body with its status, or the status alone. */
    void writeTo(HttpResponse response) {
        headers.forEach(response::header);
        if (json.isPresent()) {
            response.commit(status, "application/json", json.get());
        } else {
            response.commit(status);
        }
    }

    private static Reply json(JsonBuilder body) {
        return new Reply(200, Map.of(), Optional.of(body.toJson()));
    }

    private static JsonObjectBuilder<?> error(Optional<JsonValue> id, int code, String message) {
        return envelope(id).addObject("error", e -> e.add("code", code).add("message", message));
    }

    /**
     * The envelope. JSON-RPC wants {@code "id": null} when the id is unknown; the library never builds a JSON null, so
     * the member is left out instead.
     */
    private static JsonObjectBuilder<?> envelope(Optional<JsonValue> id) {
        JsonObjectBuilder<?> envelope = Json.object().add("jsonrpc", "2.0");
        id.ifPresent(value -> envelope.add("id", value));
        return envelope;
    }
}
