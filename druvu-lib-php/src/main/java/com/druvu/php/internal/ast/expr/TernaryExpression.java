package com.druvu.php.internal.ast.expr;

import com.druvu.php.Location;
import com.druvu.php.internal.ast.PhpExpression;
import com.druvu.php.internal.runtime.Env;
import com.druvu.php.internal.value.PhpValue;

/** {@code $a ? $b : $c}, and with no middle operand the Elvis form {@code $a ?: $c}. */
public final class TernaryExpression extends PhpExpression {

    private final PhpExpression condition;
    private final PhpExpression whenTrue;
    private final PhpExpression whenFalse;

    /** @param whenTrue null for the Elvis form, where the condition is its own result */
    public TernaryExpression(
            Location location, PhpExpression condition, PhpExpression whenTrue, PhpExpression whenFalse) {
        super(location);
        this.condition = condition;
        this.whenTrue = whenTrue;
        this.whenFalse = whenFalse;
    }

    @Override
    public PhpValue eval(Env env) {
        PhpValue tested = condition.eval(env);
        if (!tested.isTruthy()) {
            return whenFalse.eval(env);
        }
        return whenTrue == null ? tested : whenTrue.eval(env);
    }
}
