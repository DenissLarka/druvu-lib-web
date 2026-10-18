package com.druvu.web.core.handlers;

import com.druvu.web.api.handlers.UploadedFile;
import jakarta.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

/**
 * An uploaded file over the servlet's part of a multipart body; it can be read while the request lasts.
 *
 * @author Deniss Larka <br>
 *     on 18 Oct 2026
 */
final class PartUpload implements UploadedFile {

    private static final String NAMELESS = "upload";
    private static final String BINARY = "application/octet-stream";

    private final Part part;
    private final String filename;

    PartUpload(Part part) {
        this.part = Objects.requireNonNull(part, "part");
        this.filename = usableName(part.getSubmittedFileName());
    }

    @Override
    public String fieldName() {
        return part.getName();
    }

    @Override
    public String filename() {
        return filename;
    }

    @Override
    public String contentType() {
        String type = part.getContentType();
        return type == null ? BINARY : type;
    }

    @Override
    public long size() {
        return part.getSize();
    }

    @Override
    public InputStream openStream() {
        try {
            return part.getInputStream();
        } catch (IOException e) {
            throw unreadable(e);
        }
    }

    @Override
    public byte[] content() {
        try (InputStream in = openStream()) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw unreadable(e);
        }
    }

    @Override
    public void saveTo(Path target) {
        Objects.requireNonNull(target, "target");
        try (InputStream in = openStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("The upload " + filename + " could not be saved to " + target, e);
        }
    }

    private UncheckedIOException unreadable(IOException e) {
        return new UncheckedIOException("The upload " + filename + " could not be read", e);
    }

    /**
     * The browser's name without a directory part or control characters, so a hostile one such as {@code ../../x}
     * cannot climb out of the directory it is saved in; a name with nothing usable left becomes {@value #NAMELESS}.
     */
    static String usableName(String submitted) {
        String name = submitted == null ? "" : submitted;
        name = name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) + 1);
        name = name.replaceAll("\\p{Cntrl}", "");
        boolean onlyDots = name.chars().allMatch(c -> c == '.');
        return name.isBlank() || onlyDots ? NAMELESS : name;
    }
}
