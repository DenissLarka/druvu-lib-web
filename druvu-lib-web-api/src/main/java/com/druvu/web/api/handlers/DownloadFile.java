package com.druvu.web.api.handlers;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * A file a handler sends back for the browser to save, with {@code response.commit(file)}: a file on disk, read as it
 * is sent, or bytes made in memory.
 *
 * <pre>{@code
 * response.commit(DownloadFile.of(Path.of("dist/app-1.0.0.dmg")));          // streamed from disk
 * response.commit(DownloadFile.of("orders.csv", "text/csv", csvBytes));     // made in memory
 * }</pre>
 *
 * @author Deniss Larka <br>
 *     on 14 July 2022
 */
public final class DownloadFile {

    /** The content type when none is given: plain bytes, which a browser saves as they are. */
    public static final String BINARY = "application/octet-stream";

    private final String filename;
    private final String contentType;
    // Exactly one of the two is set: the file on disk, or the bytes in memory.
    private final Path file;
    private final byte[] content;

    private DownloadFile(String filename, String contentType, Path file, byte[] content) {
        this.filename = requireUsableName(filename);
        this.contentType = Objects.requireNonNull(contentType, "contentType");
        this.file = file;
        this.content = content;
    }

    /** A file on disk, under its own name, sent as {@value #BINARY}. */
    public static DownloadFile of(Path file) {
        return of(file, BINARY);
    }

    /** A file on disk, under its own name, sent with this content type. */
    public static DownloadFile of(Path file, String contentType) {
        Objects.requireNonNull(file, "file");
        Path name = file.getFileName();
        if (name == null || !Files.isRegularFile(file)) {
            throw new IllegalArgumentException("Not a file to send: " + file);
        }
        return new DownloadFile(name.toString(), contentType, file, null);
    }

    /** Bytes made in memory, under this name and with this content type; the array is copied. */
    public static DownloadFile of(String filename, String contentType, byte[] content) {
        byte[] copy = Objects.requireNonNull(content, "content").clone();
        return new DownloadFile(filename, contentType, null, copy);
    }

    /** The name the browser saves it under. */
    public String filename() {
        return filename;
    }

    /** The content type it is sent with. */
    public String contentType() {
        return contentType;
    }

    /** The length in bytes: the file's size on disk, or the length of the bytes. */
    public long length() throws IOException {
        return file != null ? Files.size(file) : content.length;
    }

    /** Writes the content to this stream: the file's bytes as they are read, or the bytes in memory. */
    public void writeTo(OutputStream out) throws IOException {
        if (file != null) {
            Files.copy(file, out);
        } else {
            out.write(content);
        }
    }

    private static String requireUsableName(String filename) {
        Objects.requireNonNull(filename, "filename");
        if (filename.isBlank() || filename.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("A download needs a file name without control characters: " + filename);
        }
        return filename;
    }
}
