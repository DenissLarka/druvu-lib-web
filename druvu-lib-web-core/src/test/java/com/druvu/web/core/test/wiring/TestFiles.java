package com.druvu.web.core.test.wiring;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import com.druvu.web.api.config.Route;
import com.druvu.web.api.config.WebConfig;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Files over real HTTP: a download made in memory and one streamed from disk; uploads that arrive byte for byte and
 * apart from the form's text fields, in order, with a hostile name cut back, an empty file input ignored, and a body
 * over the upload limit refused before its handler runs.
 */
public class TestFiles {

    private static final int UPLOAD_LIMIT = 64 * 1024;
    private static final String BOUNDARY = "druvu-test-boundary";

    private BootedApp app;
    private HttpClient http;
    private Path downloadable;

    @BeforeClass
    public void boot() throws IOException {
        downloadable = Files.createTempFile("druvu-installer", ".bin");
        Files.write(downloadable, bytes(100_000));
        WebConfig config = WebConfig.builder()
                .port(0)
                .uploadLimit(UPLOAD_LIMIT)
                .globalObject("downloadable", downloadable)
                .route(Route.forEveryone(UploadEchoHandler.class))
                .route(Route.forEveryone(DownloadBytesHandler.class))
                .route(Route.forEveryone(DownloadDiskHandler.class))
                .build();
        app = BootedApp.start(config, "/f");
        http = HttpClient.newHttpClient();
    }

    @AfterClass
    public void stop() throws IOException {
        app.close();
        Files.deleteIfExists(downloadable);
    }

    @Test
    public void aFileMadeInMemoryIsSentForTheBrowserToSave() {
        HttpResponse<byte[]> response = send(HttpRequest.newBuilder(app.uri("/download-bytes")));
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(header(response, "content-type")).startsWith("text/csv");
        assertThat(header(response, "content-disposition"))
                .isEqualTo("attachment; filename=\"Rechnung M_rz.csv\"; filename*=UTF-8''Rechnung%20M%C3%A4rz.csv");
        assertThat(new String(response.body(), UTF_8)).isEqualTo(DownloadBytesHandler.CONTENT);
    }

    @Test
    public void aFileOnDiskIsStreamedWhole() throws IOException {
        HttpResponse<byte[]> response = send(HttpRequest.newBuilder(app.uri("/download-disk")));
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(header(response, "content-type")).startsWith("application/octet-stream");
        assertThat(header(response, "content-length")).isEqualTo("100000");
        assertThat(header(response, "content-disposition"))
                .isEqualTo("attachment; filename=\"" + downloadable.getFileName() + "\"");
        assertThat(response.body()).isEqualTo(Files.readAllBytes(downloadable));
    }

    @Test
    public void anUploadArrivesByteForByteAndApartFromTheTextFields() {
        byte[] content = bytes(5000);
        String echo = upload(
                new PartSpec("note", null, null, "hello".getBytes(UTF_8)),
                new PartSpec("file", "data.bin", "application/octet-stream", content));
        assertThat(echo)
                .isEqualTo("note=hello\nfile-param=null\nupload(file)=data.bin\n"
                        + "file|data.bin|application/octet-stream|5000|" + Sha256.hex(content) + "\n");
    }

    @Test
    public void severalFilesArriveInTheOrderSent() {
        byte[] first = "first".getBytes(UTF_8);
        byte[] second = bytes(300);
        String echo = upload(
                new PartSpec("one", "a.txt", "text/plain", first), new PartSpec("two", "b.png", "image/png", second));
        assertThat(echo)
                .endsWith("one|a.txt|text/plain|5|" + Sha256.hex(first) + "\n" + "two|b.png|image/png|300|"
                        + Sha256.hex(second) + "\n");
    }

    @Test
    public void aFileNameKeepsNoDirectoryPart() {
        String echo = upload(new PartSpec("file", "../../etc/passwd", "text/plain", "x".getBytes(UTF_8)));
        assertThat(echo).contains("upload(file)=passwd\n");
    }

    @Test
    public void anEmptyFileInputIsNoUpload() {
        String echo = upload(new PartSpec("file", "", "application/octet-stream", new byte[0]));
        assertThat(echo).isEqualTo("note=null\nfile-param=null\nupload(file)=-\n");
    }

    @Test
    public void aBodyOverTheUploadLimitIsRefusedBeforeTheHandlerRuns() {
        byte[] body = multipart(new PartSpec("file", "big.bin", "application/octet-stream", bytes(100_000)));
        HttpResponse<byte[]> response = send(multipartPost(BodyPublishers.ofByteArray(body)));
        assertThat(response.statusCode()).isEqualTo(413);
    }

    @Test
    public void aBodyOverTheUploadLimitWithoutALengthIsRefusedToo() {
        byte[] body = multipart(new PartSpec("file", "big.bin", "application/octet-stream", bytes(100_000)));
        HttpResponse<byte[]> response =
                send(multipartPost(BodyPublishers.ofInputStream(() -> new ByteArrayInputStream(body))));
        // Sent in chunks, the body declares no length: Jetty finds the limit while reading and answers 400 itself.
        assertThat(response.statusCode()).isEqualTo(400);
    }

    /** One part of a multipart body: a file when it has a file name, a text field when it has none. */
    private record PartSpec(String name, String filename, String contentType, byte[] content) {}

    private String upload(PartSpec... parts) {
        HttpResponse<byte[]> response = send(multipartPost(BodyPublishers.ofByteArray(multipart(parts))));
        assertThat(response.statusCode()).isEqualTo(200);
        return new String(response.body(), UTF_8);
    }

    private HttpRequest.Builder multipartPost(BodyPublisher body) {
        return HttpRequest.newBuilder(app.uri("/upload-echo"))
                .header("Content-Type", "multipart/form-data; boundary=" + BOUNDARY)
                .POST(body);
    }

    /** The body a browser sends for a form with these parts. */
    private static byte[] multipart(PartSpec... parts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (PartSpec part : parts) {
            StringBuilder head = new StringBuilder("--" + BOUNDARY + "\r\n");
            head.append("Content-Disposition: form-data; name=\"")
                    .append(part.name())
                    .append('"');
            if (part.filename() != null) {
                head.append("; filename=\"").append(part.filename()).append('"');
            }
            head.append("\r\n");
            if (part.contentType() != null) {
                head.append("Content-Type: ").append(part.contentType()).append("\r\n");
            }
            head.append("\r\n");
            out.writeBytes(head.toString().getBytes(UTF_8));
            out.writeBytes(part.content());
            out.writeBytes("\r\n".getBytes(UTF_8));
        }
        out.writeBytes(("--" + BOUNDARY + "--\r\n").getBytes(UTF_8));
        return out.toByteArray();
    }

    /** Every byte value in turn, so a body that is not binary-safe shows. */
    private static byte[] bytes(int length) {
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = (byte) i;
        }
        return bytes;
    }

    private static String header(HttpResponse<?> response, String name) {
        return response.headers().firstValue(name).orElse("");
    }

    private HttpResponse<byte[]> send(HttpRequest.Builder request) {
        try {
            return http.send(request.build(), HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException e) {
            throw new AssertionError(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }
}
