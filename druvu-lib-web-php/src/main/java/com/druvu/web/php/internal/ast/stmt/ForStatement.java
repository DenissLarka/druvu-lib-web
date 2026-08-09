package com.druvu.web.php.internal.ast.stmt;

import com.druvu.web.php.internal.Location;
import com.druvu.web.php.internal.ast.PhpExpression;
import com.druvu.web.php.internal.ast.PhpStatement;
import com.druvu.web.php.internal.ast.Signal;
import com.druvu.web.php.internal.runtime.Env;
import java.util.List;

/**
 * {@code for (init; condition; step)}.
 *
 * <p>Each of the three parts is a list, because PHP allows commas in all of them. When the condition list has more than
 * one expression, the last one decides — the earlier ones are evaluated for their effects.
 */
public final class ForStatement extends PhpStatement {

    private final List<PhpExpression> initialisers;
    private final List<PhpExpression> conditions;
    private final List<PhpExpression> steps;
    private final PhpStatement body;

    public ForStatement(
            Location location,
            List<PhpExpression> initialisers,
            List<PhpExpression> conditions,
            List<PhpExpression> steps,
            PhpStatement body) {
        super(location);
        this.initialisers = List.copyOf(initialisers);
        this.conditions = List.copyOf(conditions);
        this.steps = List.copyOf(steps);
        this.body = body;
    }

    @Override
    public Signal execute(Env env) {
        LoopGuard guard = new LoopGuard(env, location());
        for (PhpExpression initialiser : initialisers) {
            initialiser.eval(env);
        }
        while (shouldRun(env)) {
            guard.tick();
            LoopStep step = LoopStep.of(body.execute(env));
            if (step.finished()) {
                return step.propagate();
            }
            for (PhpExpression each : steps) {
                each.eval(env);
            }
        }
        return null;
    }

    /** An empty condition list means "forever", which is what {@code for (;;)} asks for. */
    private boolean shouldRun(Env env) {
        boolean run = true;
        for (PhpExpression condition : conditions) {
            run = condition.eval(env).isTruthy();
        }
        return run;
    }
}
