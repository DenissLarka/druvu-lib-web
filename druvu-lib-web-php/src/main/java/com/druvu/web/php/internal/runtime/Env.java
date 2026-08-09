package com.druvu.web.php.internal.runtime;

import com.druvu.web.php.internal.Location;
import com.druvu.web.php.internal.PhpEngineConfig;
import com.druvu.web.php.internal.PhpProcessingException;
import com.druvu.web.php.internal.value.PhpValue;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Everything one render of one template needs, and deliberately nothing else.
 *
 * <p>Its whole job is to be small. A PHP runtime's environment object is the classic place for a code base to rot into
 * a god object holding every service anyone ever needed; this one holds an output sink, the variables in view, the
 * include bookkeeping and a way to reach the request. Anything else belongs to the node that needs it.
 *
 * <p>Variables are reached through this class rather than by handing out the {@link Scope} itself, so no statement can
 * hold on to a scope across a push and find itself writing into one that is no longer in view.
 *
 * <p>One instance serves one render on one thread and is not shared.
 *
 * @author Deniss Larka
 */
public final class Env {

    private final PhpEngineConfig config;
    private final HttpServletRequest request;
    private final FunctionRegistry functions;
    private final StringBuilder output = new StringBuilder();
    private final Set<String> includedOnce = new HashSet<>();
    private final List<Diagnostic> diagnostics = new ArrayList<>();

    private final TemplateSource templates;

    private Scope scope = new Scope();
    private String currentTemplate;
    private int includeDepth;

    /**
     * @param config the policy this render runs under
     * @param request the request being served, or null when a template is rendered outside a container
     */
    public Env(PhpEngineConfig config, HttpServletRequest request) {
        this(config, request, FunctionRegistry.empty(), TemplateSource.NONE, "/");
    }

    public Env(PhpEngineConfig config, HttpServletRequest request, FunctionRegistry functions) {
        this(config, request, functions, TemplateSource.NONE, "/");
    }

    /**
     * @param templates where an include finds what it names
     * @param currentTemplate the path of the template about to run, which relative includes resolve against
     */
    public Env(
            PhpEngineConfig config,
            HttpServletRequest request,
            FunctionRegistry functions,
            TemplateSource templates,
            String currentTemplate) {
        this.config = Objects.requireNonNull(config, "config");
        this.request = request;
        this.functions = Objects.requireNonNull(functions, "functions");
        this.templates = Objects.requireNonNull(templates, "templates");
        this.currentTemplate = Objects.requireNonNull(currentTemplate, "currentTemplate");
    }

    public PhpEngineConfig config() {
        return config;
    }

    /** The function with this name, or null when the library has none. */
    public PhpFunction findFunction(String name) {
        return functions.find(name);
    }

    /** Whether calling this function rearranges what its first argument named — true only of the sorts. */
    public boolean functionWritesBackFirstArgument(String name) {
        return functions.writesBackFirstArgument(name);
    }

    public TemplateSource templates() {
        return templates;
    }

    /** The template currently running, which is what a relative include resolves against. */
    public String currentTemplate() {
        return currentTemplate;
    }

    /** Starts running an included template and returns the one it displaced, for {@link #leaveTemplate}. */
    public String enterTemplate(String path) {
        String previous = currentTemplate;
        currentTemplate = path;
        return previous;
    }

    public void leaveTemplate(String previous) {
        currentTemplate = previous;
    }

    /** The request being served, or null when there is none. */
    public HttpServletRequest request() {
        return request;
    }

    /** The value of a variable in the scope currently in view, or null when it is not set. */
    public PhpValue getVariable(String name) {
        return scope.get(name);
    }

    /** Whether a variable is set, which is not the same as holding a non-null value. */
    public boolean isDefined(String name) {
        return scope.isDefined(name);
    }

    public void setVariable(String name, PhpValue value) {
        scope.set(name, value);
    }

    public void unsetVariable(String name) {
        scope.unset(name);
    }

    /**
     * Installs a fresh scope and returns the one it displaced, which the caller hands back to {@link #popScope}.
     * Swapping the pointer is all there is to it — no stack, and the displaced scope stays intact.
     */
    public Scope pushScope() {
        Scope previous = scope;
        scope = new Scope();
        return previous;
    }

    public void popScope(Scope previous) {
        scope = Objects.requireNonNull(previous, "previous");
    }

    /**
     * Puts the host's data where the template can see it: each entry becomes an ordinary variable.
     *
     * <p>Ordinary is the point. There is no {@code $model} to reach through and no accessor to learn — a handler that
     * passes {@code title} makes {@code $title}, which is what a template author expects and what makes a layout
     * readable without knowing how it is driven.
     */
    public void bind(Map<String, ?> model) {
        model.forEach((name, value) -> setVariable(name, HostValues.of(value)));
    }

    /**
     * A copy of every variable in view, taken by value. This is what an arrow function captures at the moment it is
     * created — which is why changing a variable afterwards does not change what the closure sees.
     */
    public Map<String, PhpValue> captureVariables() {
        Map<String, PhpValue> captured = new HashMap<>();
        for (String name : scope.names()) {
            captured.put(name, scope.get(name).copyForAssignment());
        }
        return captured;
    }

    /** Records something PHP would have warned about and carried on from. */
    public void warn(Location where, String message) {
        diagnostics.add(new Diagnostic(where, message));
    }

    /** Everything worth mentioning about this render, in the order it happened. */
    public List<Diagnostic> diagnostics() {
        return List.copyOf(diagnostics);
    }

    /** Writes to the response. Whether the text needed escaping was decided before it got here. */
    public void write(String text) {
        output.append(text);
    }

    /** Everything written so far. */
    public String output() {
        return output.toString();
    }

    /**
     * Enters an included template, enforcing the configured depth limit.
     *
     * @throws PhpProcessingException if the limit is reached
     */
    public void enterInclude() {
        if (includeDepth >= config.maxIncludeDepth()) {
            throw new PhpProcessingException("Maximum include depth exceeded (" + config.maxIncludeDepth() + ")");
        }
        includeDepth++;
    }

    public void leaveInclude() {
        includeDepth--;
    }

    public int includeDepth() {
        return includeDepth;
    }

    /**
     * Records that a template was included by {@code include_once} or {@code require_once}.
     *
     * <p>Only the {@code _once} forms may call this. A plain {@code include} that marked its path would make a later
     * {@code include_once} of the same file silently do nothing — the bug this naming exists to prevent.
     *
     * @return false if the path had already been recorded, meaning the include should be skipped
     */
    public boolean markIncludedOnce(String path) {
        return includedOnce.add(path);
    }

    public boolean wasIncludedOnce(String path) {
        return includedOnce.contains(path);
    }
}
