package com.druvu.web.php.internal.ast.expr;

import com.druvu.web.php.internal.Location;
import com.druvu.web.php.internal.ast.PhpExpression;
import com.druvu.web.php.internal.runtime.Env;
import com.druvu.web.php.internal.value.PhpBool;
import com.druvu.web.php.internal.value.PhpValue;

/** {@code empty($a)}: true when the operand is missing or falsy. Never complains about what is not there. */
public final class EmptyExpression extends PhpExpression {

    private final PhpExpression operand;

    public EmptyExpression(Location location, PhpExpression operand) {
        super(location);
        this.operand = operand;
    }

    @Override
    public PhpValue eval(Env env) {
        return PhpBool.of(!operand.evalQuietly(env).isTruthy());
    }
}
