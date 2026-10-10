package com.druvu.php.internal.ast.expr;

import com.druvu.php.Location;
import com.druvu.php.internal.ast.PhpExpression;
import com.druvu.php.internal.runtime.Env;
import com.druvu.php.internal.value.PhpBool;
import com.druvu.php.internal.value.PhpValue;

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
