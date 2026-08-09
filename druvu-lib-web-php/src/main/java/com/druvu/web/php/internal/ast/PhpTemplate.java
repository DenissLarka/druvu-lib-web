package com.druvu.web.php.internal.ast;

import com.druvu.web.php.internal.runtime.Env;
import com.druvu.web.php.internal.value.PhpInt;
import com.druvu.web.php.internal.value.PhpValue;
import java.util.List;

/**
 * A parsed template: its statements, ready to run.
 *
 * <p>Immutable once parsed, which is what will let one parse serve many requests and many threads.
 *
 * @author Deniss Larka
 */
public final class PhpTemplate {

    private final String path;
    private final List<PhpStatement> statements;

    public PhpTemplate(String path, List<PhpStatement> statements) {
        this.path = path;
        this.statements = List.copyOf(statements);
    }

    public String path() {
        return path;
    }

    /**
     * Runs the template, writing into the environment.
     *
     * @return what a {@code return} at the top level produced, or 1 when the template said nothing
     */
    public PhpValue render(Env env) {
        for (PhpStatement statement : statements) {
            Signal signal = statement.execute(env);
            if (signal instanceof Signal.Return returned) {
                return returned.value();
            }
            if (signal != null) {
                throw new com.druvu.web.php.internal.PhpProcessingException(
                        statement.location() + ": break or continue outside a loop");
            }
        }
        return PhpInt.of(1L);
    }
}
