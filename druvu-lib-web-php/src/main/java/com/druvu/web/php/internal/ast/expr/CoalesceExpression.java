package com.druvu.web.php.internal.ast.expr;

import com.druvu.web.php.internal.Location;
import com.druvu.web.php.internal.ast.PhpExpression;
import com.druvu.web.php.internal.runtime.Env;
import com.druvu.web.php.internal.value.PhpNull;
import com.druvu.web.php.internal.value.PhpValue;

/**
 * {@code $a ?? $b}.
 *
 * <p>The left side is read quietly: asking whether optional data is there is the point of the operator, so an undefined
 * variable or a missing key is not worth mentioning.
 */
public final class CoalesceExpression extends PhpExpression {

    private final PhpExpression left;
    private final PhpExpression right;

    public CoalesceExpression(Location location, PhpExpression left, PhpExpression right) {
        super(location);
        this.left = left;
        this.right = right;
    }

    @Override
    public PhpValue eval(Env env) {
        PhpValue value = left.evalQuietly(env);
        return value instanceof PhpNull ? right.eval(env) : value;
    }

    @Override
    public PhpValue evalQuietly(Env env) {
        return eval(env);
    }
}
