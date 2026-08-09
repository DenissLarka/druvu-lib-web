package com.druvu.web.php.internal.ast.stmt;

import com.druvu.web.php.internal.Location;
import com.druvu.web.php.internal.ast.PhpExpression;
import com.druvu.web.php.internal.ast.PhpStatement;
import com.druvu.web.php.internal.ast.Signal;
import com.druvu.web.php.internal.runtime.Env;

/** An expression written for what it does rather than for its value: an assignment, a call. */
public final class ExpressionStatement extends PhpStatement {

    private final PhpExpression expression;

    public ExpressionStatement(Location location, PhpExpression expression) {
        super(location);
        this.expression = expression;
    }

    @Override
    public Signal execute(Env env) {
        expression.eval(env);
        return null;
    }
}
