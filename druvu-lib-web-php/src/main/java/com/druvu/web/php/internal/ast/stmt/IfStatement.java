package com.druvu.web.php.internal.ast.stmt;

import com.druvu.web.php.internal.Location;
import com.druvu.web.php.internal.ast.PhpExpression;
import com.druvu.web.php.internal.ast.PhpStatement;
import com.druvu.web.php.internal.ast.Signal;
import com.druvu.web.php.internal.runtime.Env;

/** {@code if}, in both the braced form and the {@code if (…): … endif;} form that layouts are written in. */
public final class IfStatement extends PhpStatement {

    private final PhpExpression condition;
    private final PhpStatement whenTrue;
    private final PhpStatement whenFalse;

    /** @param whenFalse the else or elseif branch, or null when there is none */
    public IfStatement(Location location, PhpExpression condition, PhpStatement whenTrue, PhpStatement whenFalse) {
        super(location);
        this.condition = condition;
        this.whenTrue = whenTrue;
        this.whenFalse = whenFalse;
    }

    @Override
    public Signal execute(Env env) {
        if (condition.eval(env).isTruthy()) {
            return whenTrue.execute(env);
        }
        return whenFalse == null ? null : whenFalse.execute(env);
    }
}
