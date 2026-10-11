package com.druvu.web.example.mcp;

import com.druvu.json.Json;
import com.druvu.json.JsonValue;

/**
 * The server behind the transport: answers a call with the handshake, a ping, the tool list or a tool call, and names
 * what it does not know.
 *
 * @author Deniss Larka <br>
 *     on 11 Oct 2026
 */
final class McpServer {

    static final String SERVER_NAME = "druvu-lib-web-example-mcp";

    Reply answer(McpCall call) {
        return switch (call.method()) {
            case "initialize" -> Reply.result(call.id(), initialize());
            case "ping" -> Reply.result(call.id(), Json.object());
            case "tools/list" -> Reply.result(call.id(), Json.object().add("tools", Tools.list()));
            case "tools/call" -> toolsCall(call);
            default -> Reply.methodNotFound(call.id(), call.method());
        };
    }

    /** A fixed shape with two values that move: written down as the JSON it is, filled, not built. */
    private static JsonValue initialize() {
        return Json.fill("""
                {
                  "protocolVersion": ?,
                  "capabilities": { "tools": { "listChanged": false } },
                  "serverInfo": { "name": ?, "version": "1" },
                  "instructions": "Two tools: echo returns its text, time_now tells the time in a zone."
                }
                """, McpTransport.PROTOCOL_VERSION, SERVER_NAME);
    }

    private static Reply toolsCall(McpCall call) {
        String name = call.param("name");
        return Tools.call(name, call.object("arguments"))
                .map(result -> Reply.result(call.id(), result))
                .orElseGet(() -> Reply.invalidParams(call.id(), "Unknown tool: " + name));
    }
}
