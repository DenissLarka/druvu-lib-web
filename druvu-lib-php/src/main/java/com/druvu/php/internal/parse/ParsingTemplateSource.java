package com.druvu.php.internal.parse;

import com.druvu.php.PhpEngineConfig;
import com.druvu.php.TemplateLoader;
import com.druvu.php.internal.ast.PhpTemplate;
import com.druvu.php.internal.runtime.TemplateSource;
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

    private final TemplateLoader loader;
    private final PhpEngineConfig config;

    public ParsingTemplateSource(TemplateLoader loader, PhpEngineConfig config) {
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
