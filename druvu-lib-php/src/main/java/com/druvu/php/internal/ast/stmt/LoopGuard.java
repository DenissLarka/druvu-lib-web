package com.druvu.php.internal.ast.stmt;

import com.druvu.php.Location;
import com.druvu.php.PhpProcessingException;
import com.druvu.php.internal.runtime.Env;

/**
 * Counts a loop's turns and stops the render if there are too many.
 *
 * <p>A template is written by whoever writes the markup, and {@code while (true)} in one would otherwise pin a request
 * thread until something else killed it. The limit is high enough that no honest layout meets it.
 */
final class LoopGuard {

    private final long limit;
    private final Location where;

    private long turns;

    LoopGuard(Env env, Location where) {
        this.limit = env.config().maxLoopIterations();
        this.where = where;
    }

    void tick() {
        if (++turns > limit) {
            throw new PhpProcessingException(where + ": loop ran more than " + limit + " times and was stopped");
        }
    }
}
