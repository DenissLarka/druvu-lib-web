package com.druvu.php;

import com.druvu.php.internal.ast.PhpTemplate;
import com.druvu.php.internal.builtin.Builtins;
import com.druvu.php.internal.parse.CachingTemplateSource;
import com.druvu.php.internal.parse.ParsingTemplateSource;
import com.druvu.php.internal.runtime.Env;
import com.druvu.php.internal.runtime.FunctionRegistry;
import com.druvu.php.internal.runtime.Superglobals;
import com.druvu.php.internal.runtime.TemplateSource;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * The engine, assembled: a PHP 8 layout language, executed in Java.
 *
 * <p>One of these serves a whole application. Everything it holds is finished being written before the first render —
 * the function library, the policy, the parsed templates — and everything a render needs to change lives in the
 * environment it makes for that render alone. That is the entire thread-safety story, and it is the reason it fits in a
 * paragraph.
 *
 * <pre>{@code
 * PhpEngine engine = new PhpEngine(TemplateLoader.classpath("templates"), PhpEngineConfig.DEFAULTS);
 * String page = engine.render("/home.php", Map.of("visitor", "Ada")).orElseThrow();
 * }</pre>
 *
 * @author Deniss Larka
 */
public final class PhpEngine {

    private final PhpEngineConfig config;
    private final FunctionRegistry functions;
    private final TemplateSource templates;

    /**
     * @param templates where the text of a template comes from
     * @param config the policy every render runs under, including whether parsed templates are kept
     */
    public PhpEngine(TemplateLoader templates, PhpEngineConfig config) {
        this.config = Objects.requireNonNull(config, "config");
        this.functions = Builtins.registry();
        TemplateSource parsing = new ParsingTemplateSource(Objects.requireNonNull(templates, "templates"), config);
        this.templates = config.cacheTemplates() ? new CachingTemplateSource(parsing) : parsing;
    }

    /** Renders a template that has no request behind it: a mail body, a file, a test. */
    public Optional<String> render(String path, Map<String, ?> model) {
        return render(path, model, null);
    }

    /**
     * Renders one template.
     *
     * @param path the template, root-relative: {@code /home.php}
     * @param model what the application wants the template to see, each entry arriving as an ordinary variable
     * @param request the request being served, or null when there is none
     * @return the finished page, or empty when there is no template at that path
     * @throws PhpSyntaxException when the template, or one it includes, does not parse
     * @throws PhpProcessingException when rendering fails
     */
    public Optional<String> render(String path, Map<String, ?> model, HostRequest request) {
        PhpTemplate template = templates.find(path);
        if (template == null) {
            return Optional.empty();
        }
        Env env = new Env(config, request, functions, templates, path);
        Superglobals.bindInto(env, request);
        if (model != null) {
            env.bind(model);
        }
        template.render(env);
        return Optional.of(env.output());
    }

    public PhpEngineConfig config() {
        return config;
    }
}
