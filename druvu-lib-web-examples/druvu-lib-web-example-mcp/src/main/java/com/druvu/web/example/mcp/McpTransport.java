package com.druvu.web.example.mcp;

import com.druvu.json.Json;
import com.druvu.json.JsonException;
import com.druvu.json.JsonObject;
import com.druvu.json.JsonValue;
import com.druvu.web.api.handlers.HttpRequest;
import java.net.InetAddress;
import java.net.URI;
import java.util.Optional;
import java.util.Set;

/**
 * The Streamable HTTP transport, protocol {@value #PROTOCOL_VERSION}, as far as this server goes: one JSON-RPC 2.0
 * message per POST, no sessions, no server-to-client stream, which the transport allows, so GET and DELETE are refused.
 * A browser origin that is not local is refused too, which is the transport's guard against DNS rebinding. What comes
 * out is a {@link McpCall} to answer, or the {@link Reply} this layer already knows.
 *
 * @author Deniss Larka <br>
 *     on 11 Oct 2026
 */
final class McpTransport {

    static final String PROTOCOL_VERSION = "2025-06-18";
    private static final Set<String> SUPPORTED_VERSIONS = Set.of(PROTOCOL_VERSION, "2025-03-26");

    private McpTransport() {}

    static Parsed read(HttpRequest request) {
        if (!request.isPost()) {
            return Reply.methodNotAllowed("POST");
        }
        if (!originAllowed(request.header("Origin"))) {
            return Reply.status(403);
        }
        Optional<String> version = request.header("MCP-Protocol-Version");
        if (version.isPresent() && !SUPPORTED_VERSIONS.contains(version.get())) {
            return Reply.badRequest("Unsupported MCP-Protocol-Version: " + version.get());
        }
        JsonValue parsed;
        try {
            parsed = Json.parse(request.body());
        } catch (JsonException e) {
            return Reply.parseError();
        }
        if (!(parsed instanceof JsonObject message)) {
            return Reply.invalidRequest(
                    Optional.empty(), "one JSON-RPC object expected; batches are gone since 2025-06-18");
        }
        if (!message.has("id")) {
            return Reply.accepted(); // a notification, or a response to something this server never asks
        }
        JsonValue id = message.get("id");
        try {
            return new McpCall(
                    id, message.string("method"), message.findObject("params").orElse(McpCall.NO_PARAMS));
        } catch (JsonException e) {
            return Reply.invalidRequest(Optional.of(id), e.getMessage());
        }
    }

    private static boolean originAllowed(Optional<String> origin) {
        if (origin.isEmpty()) {
            return true;
        }
        try {
            String host = URI.create(origin.get()).getHost();
            return host != null && isLocal(host);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * {@code localhost} or a loopback literal; a name is never resolved, which is what the rebinding guard is about.
     */
    private static boolean isLocal(String host) {
        if ("localhost".equals(host)) {
            return true;
        }
        String literal = host.startsWith("[") && host.endsWith("]") ? host.substring(1, host.length() - 1) : host;
        try {
            return InetAddress.ofLiteral(literal).isLoopbackAddress();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
