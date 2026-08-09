package com.druvu.web.php.internal.parse;

import com.druvu.web.php.internal.ast.PhpTemplate;
import com.druvu.web.php.internal.runtime.TemplateSource;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Parses each template once and keeps the tree.
 *
 * <p>The one optimisation the design admits, and it earns its place twice over: re-parsing every partial on every
 * request is work with nothing to show for it, and the cache is a single map in front of something that already works.
 * A parsed template is immutable, so many requests can share one safely; all the state a render needs lives in its own
 * {@code Env}.
 *
 * <p>Off while developing, so an edited template takes effect on the next request. On in production, where templates do
 * not change between deployments.
 *
 * @author Deniss Larka
 */
public final class CachingTemplateSource implements TemplateSource {

    private final TemplateSource templates;
    private final Map<String, PhpTemplate> parsed = new ConcurrentHashMap<>();

    public CachingTemplateSource(TemplateSource templates) {
        this.templates = Objects.requireNonNull(templates, "templates");
    }

    @Override
    public PhpTemplate find(String path) {
        PhpTemplate cached = parsed.get(path);
        if (cached != null) {
            return cached;
        }
        PhpTemplate template = templates.find(path);
        if (template != null) {
            parsed.put(path, template);
        }
        // A missing template is not remembered: adding the file should be enough to make it appear.
        return template;
    }

    /** Forgets everything, so the next request parses afresh. */
    public void clear() {
        parsed.clear();
    }

    public int size() {
        return parsed.size();
    }
}
