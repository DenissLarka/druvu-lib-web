package com.druvu.web.php.internal.ast.stmt;

import com.druvu.web.php.internal.Location;
import com.druvu.web.php.internal.ast.PhpExpression;
import com.druvu.web.php.internal.ast.PhpStatement;
import com.druvu.web.php.internal.ast.Signal;
import com.druvu.web.php.internal.runtime.Env;

/** {@code do … while (…);} — the one loop PHP gives no colon form, so this one is braced only. */
public final class DoWhileStatement extends PhpStatement {

    private final PhpStatement body;
    private final PhpExpression condition;

    public DoWhileStatement(Location location, PhpStatement body, PhpExpression condition) {
        super(location);
        this.body = body;
        this.condition = condition;
    }

    @Override
    public Signal execute(Env env) {
        LoopGuard guard = new LoopGuard(env, location());
        do {
            guard.tick();
            LoopStep step = LoopStep.of(body.execute(env));
            if (step.finished()) {
                return step.propagate();
            }
        } while (condition.eval(env).isTruthy());
        return null;
    }
}
