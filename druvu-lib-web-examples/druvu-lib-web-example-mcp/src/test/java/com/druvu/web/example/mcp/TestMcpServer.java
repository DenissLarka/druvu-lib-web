package com.druvu.web.example.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import com.druvu.json.Json;
import com.druvu.json.JsonArray;
import com.druvu.json.JsonBuilder;
import com.druvu.json.JsonObject;
import com.druvu.json.JsonObjectBuilder;
import com.druvu.json.JsonValue;
import com.druvu.web.core.WebBoot;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * The MCP endpoint over real HTTP: the token gate, the handshake, the tool list, a call and a tool's own error, and
 * what the transport refuses: another method, a foreign origin, an unknown protocol version, unreadable JSON, a batch.
 */
public class TestMcpServer {

    private static final String TOKEN = "test-token-not-for-production";

    private WebBoot boot;
    private HttpClient http;
    private URI endpoint;
    private URI home;

    @BeforeClass
    public void boot() {
        boot = new WebBoot(McpExample.config(0, BootToken.of(TOKEN)));
        boot.start("/");
        endpoint = URI.create("http://127.0.0.1:" + boot.port() + "/mcp");
        home = URI.create("http://127.0.0.1:" + boot.port() + "/");
        http = HttpClient.newHttpClient();
    }

    @AfterClass
    public void stop() {
        boot.stop();
    }

    @Test
    public void theHomePageSaysWhatThisIs() throws IOException, InterruptedException {
        HttpResponse<String> response =
                http.send(HttpRequest.newBuilder(home).GET().build(), ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("MCP").contains("/mcp");
    }

    @Test
    public void withoutATokenTheEndpointChallenges() throws IOException, InterruptedException {
        HttpResponse<String> response = http.send(
                HttpRequest.newBuilder(endpoint)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(request(1, "ping", null)))
                        .build(),
                ofString());
        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.headers().firstValue("WWW-Authenticate"))
                .get()
                .asString()
                .startsWith("Bearer");
    }

    @Test
    public void initializeAnswersTheProtocolVersionAndTheToolsCapability() throws IOException, InterruptedException {
        JsonBuilder params = Json.object()
                .add("protocolVersion", "2025-06-18")
                .addObject("capabilities")
                .end();
        JsonObject result = rpc(1, "initialize", params).object("result");
        assertThat(result.string("protocolVersion")).isEqualTo("2025-06-18");
        assertThat(result.object("capabilities").has("tools")).isTrue();
        assertThat(result.object("serverInfo").string("name")).isEqualTo(McpServer.SERVER_NAME);
    }

    @Test
    public void theInitializedNotificationIsAcceptedWithNoBody() throws IOException, InterruptedException {
        HttpResponse<String> response = post("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}");
        assertThat(response.statusCode()).isEqualTo(202);
        assertThat(response.body()).isEmpty();
    }

    @Test
    public void toolsListNamesEchoAndTimeNowWithSchemas() throws IOException, InterruptedException {
        JsonArray tools = rpc(2, "tools/list", null).object("result").array("tools");
        assertThat(tools.stream().map(tool -> tool.asObject().string("name"))).containsExactly("echo", "time_now");
        for (JsonValue tool : tools) {
            assertThat(tool.asObject().object("inputSchema").string("type")).isEqualTo("object");
        }
    }

    @Test
    public void echoReturnsItsTextInUtf8() throws IOException, InterruptedException {
        JsonObject result =
                rpc(3, "tools/call", call("echo", "text", "héllo ✓")).object("result");
        assertThat(result.bool("isError")).isFalse();
        assertThat(firstText(result)).isEqualTo("héllo ✓");
    }

    @Test
    public void timeNowAnswersInTheZoneAsked() throws IOException, InterruptedException {
        JsonObject result =
                rpc(4, "tools/call", call("time_now", "zone", "Europe/Zurich")).object("result");
        OffsetDateTime time = OffsetDateTime.parse(firstText(result));
        assertThat(time.getOffset().getTotalSeconds()).isIn(3600, 7200);
    }

    @Test
    public void anUnknownZoneIsTheToolsOwnError() throws IOException, InterruptedException {
        JsonObject result =
                rpc(5, "tools/call", call("time_now", "zone", "Mars/Olympus")).object("result");
        assertThat(result.bool("isError")).isTrue();
        assertThat(firstText(result)).contains("Unknown zone");
    }

    @Test
    public void anUnknownToolIsInvalidParams() throws IOException, InterruptedException {
        assertThat(rpc(6, "tools/call", call("nope", "text", "x"))
                        .object("error")
                        .intValue("code"))
                .isEqualTo(-32602);
    }

    @Test
    public void anUnknownMethodIsMethodNotFound() throws IOException, InterruptedException {
        assertThat(rpc(7, "resources/list", null).object("error").intValue("code"))
                .isEqualTo(-32601);
    }

    @Test
    public void unreadableJsonIsAParseError() throws IOException, InterruptedException {
        HttpResponse<String> response = post("{not json");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(errorCode(response)).isEqualTo(-32700);
    }

    @Test
    public void aBatchIsNotAcceptedAnyMore() throws IOException, InterruptedException {
        HttpResponse<String> response = post("[" + request(8, "ping", null) + "]");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(errorCode(response)).isEqualTo(-32600);
    }

    @Test
    public void aGetIsRefusedWithAllow() throws IOException, InterruptedException {
        HttpResponse<String> response = http.send(
                HttpRequest.newBuilder(endpoint)
                        .header("Authorization", "Bearer " + TOKEN)
                        .GET()
                        .build(),
                ofString());
        assertThat(response.statusCode()).isEqualTo(405);
        assertThat(response.headers().firstValue("Allow")).contains("POST");
    }

    @Test
    public void aForeignOriginIsRefused() throws IOException, InterruptedException {
        HttpResponse<String> response = post(request(9, "ping", null), "Origin", "http://evil.example");
        assertThat(response.statusCode()).isEqualTo(403);
    }

    @Test
    public void anUnsupportedProtocolVersionIsABadRequest() throws IOException, InterruptedException {
        HttpResponse<String> response = post(request(10, "ping", null), "MCP-Protocol-Version", "1999-01-01");
        assertThat(response.statusCode()).isEqualTo(400);
    }

    private JsonObject rpc(int id, String method, JsonBuilder params) throws IOException, InterruptedException {
        HttpResponse<String> response = post(request(id, method, params));
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type"))
                .get()
                .asString()
                .startsWith("application/json");
        JsonObject message = Json.parse(response.body()).asObject();
        assertThat(message.string("jsonrpc")).isEqualTo("2.0");
        assertThat(message.intValue("id")).isEqualTo(id);
        return message;
    }

    private HttpResponse<String> post(String body, String... headers) throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder(endpoint)
                .header("Authorization", "Bearer " + TOKEN)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        for (int i = 0; i < headers.length; i += 2) {
            request.header(headers[i], headers[i + 1]);
        }
        return http.send(request.build(), ofString());
    }

    private static String request(int id, String method, JsonBuilder params) {
        JsonObjectBuilder<?> message =
                Json.object().add("jsonrpc", "2.0").add("id", id).add("method", method);
        if (params != null) {
            message.add("params", params);
        }
        return message.toJson();
    }

    private static JsonBuilder call(String tool, String argument, String value) {
        return Json.object()
                .add("name", tool)
                .addObject("arguments")
                .add(argument, value)
                .end();
    }

    private static String firstText(JsonObject result) {
        return result.array("content").get(0).asObject().string("text");
    }

    private static int errorCode(HttpResponse<String> response) {
        return Json.parse(response.body()).asObject().object("error").intValue("code");
    }

    private static HttpResponse.BodyHandler<String> ofString() {
        return HttpResponse.BodyHandlers.ofString();
    }
}
