package com.druvu.web.php.internal.parse;

import com.druvu.web.php.internal.PhpEngineConfig;
import com.druvu.web.php.internal.ast.PhpTemplate;
import com.druvu.web.php.internal.runtime.TemplateSource;
import java.io.IOException;
import java.util.Objects;

/**
 * Parses a template each time it is asked for.
 *
 * <p>The straightforward implementation, and the one that is right during development: a template edited on disk takes
 * effect on the next request. A caching one belongs in front of this, not instead of it.
 *
 * @author Deniss Larka
 */
public final class ParsingTemplateSource implements TemplateSource {

    /** Where the text of a template comes from. */
    @FunctionalInterface
    public interface Loader {

        /** The source at this path, or null when there is nothing there. */
        String load(String path) throws IOException;
    }

    private final Loader loader;
    private final PhpEngineConfig config;

    public ParsingTemplateSource(Loader loader, PhpEngineConfig config) {
        this.loader = Objects.requireNonNull(loader, "loader");
        this.config = Objects.requireNonNull(config, "config");
    }

    @Override
    public PhpTemplate find(String path) {
        String source;
        try {
            source = loader.load(path);
        } catch (IOException notThere) {
            return null;
        }
        return source == null ? null : PhpParser.parse(source, path, config);
    }
}
