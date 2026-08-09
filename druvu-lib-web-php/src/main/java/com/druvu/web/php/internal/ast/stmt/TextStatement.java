package com.druvu.web.php.internal.ast.stmt;

import com.druvu.web.php.internal.Location;
import com.druvu.web.php.internal.ast.PhpStatement;
import com.druvu.web.php.internal.ast.Signal;
import com.druvu.web.php.internal.runtime.Env;
import java.util.Objects;

/**
 * Literal template text — everything outside a PHP tag.
 *
 * <p>It goes to the response exactly as written. The engine's escape-by-default rule is about values reaching
 * {@code echo}; the page's own markup is not a value and is never touched.
 *
 * @author Deniss Larka
 */
public final class TextStatement extends PhpStatement {

    private final String text;

    public TextStatement(Location location, String text) {
        super(location);
        this.text = Objects.requireNonNull(text, "text");
    }

    public String text() {
        return text;
    }

    @Override
    public Signal execute(Env env) {
        env.write(text);
        return null;
    }
}
