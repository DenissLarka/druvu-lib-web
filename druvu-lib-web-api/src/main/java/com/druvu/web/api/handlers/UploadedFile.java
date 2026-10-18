package com.druvu.web.api.handlers;

import java.io.InputStream;
import java.nio.file.Path;

/**
 * A file sent in a {@code multipart/form-data} request, the way {@code <input type="file">} sends it. It can be read
 * while the handler runs; the server deletes what it kept of it when the request ends. A failure to read it is an
 * {@link java.io.UncheckedIOException}, so a handler needs no {@code throws}.
 *
 * <pre>{@code
 * request.upload("installer").ifPresent(file -> file.saveTo(incoming.resolve(file.filename())));
 * }</pre>
 *
 * @author Deniss Larka <br>
 *     on 18 Oct 2026
 */
public interface UploadedFile {

    /** The form field it came in: the {@code name} of the input. */
    String fieldName();

    /**
     * The name the browser gave the file without any directory part, so it can name a file inside a directory of yours;
     * {@code "upload"} when nothing usable is left of it.
     */
    String filename();

    /** The content type the browser declared, or {@code application/octet-stream} when it declared none. */
    String contentType();

    /** The size in bytes. */
    long size();

    /** The bytes as a stream, for a large file; the caller closes it. */
    InputStream openStream();

    /** All the bytes at once, for a small file. */
    byte[] content();

    /** Copies the file to this path, replacing a file already there; the directory must exist. */
    void saveTo(Path target);
}
