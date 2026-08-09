package com.druvu.web.php.internal.ast.expr;

import com.druvu.web.php.internal.Location;
import com.druvu.web.php.internal.ast.PhpExpression;
import com.druvu.web.php.internal.runtime.Env;
import com.druvu.web.php.internal.value.PhpValue;

/** Any two-operand operator that evaluates both sides: arithmetic, concatenation, comparison. */
public final class BinaryExpression extends PhpExpression {

    private final BinaryOperator operator;
    private final PhpExpression left;
    private final PhpExpression right;

    public BinaryExpression(Location location, BinaryOperator operator, PhpExpression left, PhpExpression right) {
        super(location);
        this.operator = operator;
        this.left = left;
        this.right = right;
    }

    @Override
    public PhpValue eval(Env env) {
        return operator.apply(left.eval(env), right.eval(env));
    }
}
