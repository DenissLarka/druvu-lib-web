package com.druvu.web.php.internal.ast.stmt;

import com.druvu.web.php.internal.Location;
import com.druvu.web.php.internal.ast.PhpExpression;
import com.druvu.web.php.internal.ast.PhpStatement;
import com.druvu.web.php.internal.ast.Signal;
import com.druvu.web.php.internal.runtime.Env;

/** {@code while (…)}, braced or {@code endwhile}-terminated. */
public final class WhileStatement extends PhpStatement {

    private final PhpExpression condition;
    private final PhpStatement body;

    public WhileStatement(Location location, PhpExpression condition, PhpStatement body) {
        super(location);
        this.condition = condition;
        this.body = body;
    }

    @Override
    public Signal execute(Env env) {
        LoopGuard guard = new LoopGuard(env, location());
        while (condition.eval(env).isTruthy()) {
            guard.tick();
            LoopStep step = LoopStep.of(body.execute(env));
            if (step.finished()) {
                return step.propagate();
            }
        }
        return null;
    }
}
