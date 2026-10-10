package com.druvu.php;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Where the text of a template comes from.
 *
 * <p>The engine asks for a template by its root-relative path ({@code /home.php}, {@code /parts/header.php}) and
 * resolves every include against that same root, refusing a path that climbs above it. A loader only answers one
 * question: what is the source at this path, or is there nothing there.
 *
 * @author Deniss Larka <br>
 *     on 10 Oct 2026
 */
@FunctionalInterface
public interface TemplateLoader {

    /** The source at this path, or null when there is nothing there. */
    String load(String path) throws IOException;

    /**
     * Templates read from the classpath under a root folder, {@code webapp} for instance: the usual choice when the
     * templates ship inside the application's jar.
     */
    static TemplateLoader classpath(String root) {
        String base = root.endsWith("/") ? root.substring(0, root.length() - 1) : root;
        return path -> {
            String resource = base + (path.startsWith("/") ? path : "/" + path);
            InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
            if (stream == null) {
                return null;
            }
            try (InputStream open = stream) {
                return new String(open.readAllBytes(), StandardCharsets.UTF_8);
            }
        };
    }

    /** Templates read from a directory, which is what development wants: an edit shows up on the next render. */
    static TemplateLoader directory(Path root) {
        Path base = root.toAbsolutePath().normalize();
        return path -> {
            Path file = base.resolve(path.startsWith("/") ? path.substring(1) : path)
                    .normalize();
            if (!file.startsWith(base) || !Files.isRegularFile(file)) {
                return null;
            }
            return Files.readString(file);
        };
    }
}
