package com.druvu.web.php.internal.ast.expr;

import com.druvu.web.php.internal.Location;
import com.druvu.web.php.internal.PhpProcessingException;
import com.druvu.web.php.internal.ast.PhpExpression;
import com.druvu.web.php.internal.ast.PhpTemplate;
import com.druvu.web.php.internal.runtime.Env;
import com.druvu.web.php.internal.runtime.TemplatePaths;
import com.druvu.web.php.internal.value.PhpBool;
import com.druvu.web.php.internal.value.PhpValue;

/**
 * {@code include}, {@code require} and their {@code _once} forms — the mechanism a layout is built from.
 *
 * <p>The included template runs in the <em>same scope</em> as the line that included it. That is not an accident of the
 * implementation, it is the whole feature: it is how a page hands its data to a partial without any parameter passing.
 *
 * <p>Two things it does that the engine's previous version did not. Only the {@code _once} forms record that a path was
 * included, so a plain {@code include} can no longer poison a later {@code include_once} into doing nothing. And a
 * failure is reported: {@code include} says so and carries on with false, {@code require} stops the render, where
 * before a missing partial silently rendered as an empty string.
 */
public final class IncludeExpression extends PhpExpression {

    /** The four spellings, and what each one means when the template is missing or has already been included. */
    public enum Kind {
        INCLUDE(false, false),
        INCLUDE_ONCE(false, true),
        REQUIRE(true, false),
        REQUIRE_ONCE(true, true);

        private final boolean required;
        private final boolean once;

        Kind(boolean required, boolean once) {
            this.required = required;
            this.once = once;
        }

        /** The word as it is written, for error messages. */
        public String spelling() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    private final Kind kind;
    private final PhpExpression path;

    public IncludeExpression(Location location, Kind kind, PhpExpression path) {
        super(location);
        this.kind = kind;
        this.path = path;
    }

    @Override
    public PhpValue eval(Env env) {
        String requested = path.eval(env).toStr();
        String resolved = TemplatePaths.resolve(requested, env.currentTemplate());

        if (kind.once && !env.markIncludedOnce(resolved)) {
            // Already included once. PHP answers true and does nothing.
            return PhpBool.TRUE;
        }

        PhpTemplate template = env.templates().find(resolved);
        if (template == null) {
            return missing(env, requested, resolved);
        }

        env.enterInclude();
        String previous = env.enterTemplate(resolved);
        try {
            return template.render(env);
        } finally {
            env.leaveTemplate(previous);
            env.leaveInclude();
        }
    }

    private PhpValue missing(Env env, String requested, String resolved) {
        String complaint = kind.spelling() + "(): failed to open '" + requested + "' (resolved to " + resolved + ")";
        if (kind.required) {
            throw new PhpProcessingException(location() + ": " + complaint);
        }
        env.warn(location(), complaint);
        return PhpBool.FALSE;
    }
}
