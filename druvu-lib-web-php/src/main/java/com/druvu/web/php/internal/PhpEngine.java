package com.druvu.web.php.internal;

import com.druvu.web.php.internal.ast.PhpTemplate;
import com.druvu.web.php.internal.builtin.Builtins;
import com.druvu.web.php.internal.parse.CachingTemplateSource;
import com.druvu.web.php.internal.parse.ParsingTemplateSource;
import com.druvu.web.php.internal.runtime.Env;
import com.druvu.web.php.internal.runtime.FunctionRegistry;
import com.druvu.web.php.internal.runtime.Superglobals;
import com.druvu.web.php.internal.runtime.TemplateSource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Objects;

/**
 * The engine, assembled.
 *
 * <p>One of these serves a whole application. Everything it holds is finished being written before the first request
 * arrives — the function library, the policy, the parsed templates — and everything a render needs to change lives in
 * the {@link Env} it makes for that render alone. That is the entire thread-safety story, and it is the reason it fits
 * in a paragraph.
 *
 * @author Deniss Larka
 */
public final class PhpEngine {

    private final PhpEngineConfig config;
    private final FunctionRegistry functions;
    private final TemplateSource templates;

    /**
     * @param loader where the text of a template comes from
     * @param config the policy every render runs under
     * @param cacheTemplates whether a parsed template is kept; false while developing, so an edit shows up at once
     */
    public PhpEngine(ParsingTemplateSource.Loader loader, PhpEngineConfig config, boolean cacheTemplates) {
        this.config = Objects.requireNonNull(config, "config");
        this.functions = Builtins.registry();
        TemplateSource parsing = new ParsingTemplateSource(Objects.requireNonNull(loader, "loader"), config);
        this.templates = cacheTemplates ? new CachingTemplateSource(parsing) : parsing;
    }

    /**
     * Renders one template.
     *
     * @param model what the handler wants the template to see, each entry arriving as an ordinary variable
     * @return the finished page, or null when there is no template at that path
     */
    public String render(String path, HttpServletRequest request, Map<String, ?> model) {
        PhpTemplate template = templates.find(path);
        if (template == null) {
            return null;
        }
        Env env = new Env(config, request, functions, templates, path);
        Superglobals.bindInto(env, request);
        if (model != null) {
            env.bind(model);
        }
        template.render(env);
        return env.output();
    }

    public PhpEngineConfig config() {
        return config;
    }
}
