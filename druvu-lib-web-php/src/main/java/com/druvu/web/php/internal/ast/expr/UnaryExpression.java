package com.druvu.web.php.internal.ast.expr;

import com.druvu.web.php.internal.Location;
import com.druvu.web.php.internal.ast.PhpExpression;
import com.druvu.web.php.internal.runtime.Env;
import com.druvu.web.php.internal.value.PhpArithmetic;
import com.druvu.web.php.internal.value.PhpBool;
import com.druvu.web.php.internal.value.PhpValue;

/** {@code !x}, {@code -x} and {@code +x}. Unary plus is not a no-op in PHP: it turns its operand into a number. */
public final class UnaryExpression extends PhpExpression {

    /** The three prefix operators the dialect has. */
    public enum Operator {
        NOT,
        NEGATE,
        IDENTITY
    }

    private final Operator operator;
    private final PhpExpression operand;

    public UnaryExpression(Location location, Operator operator, PhpExpression operand) {
        super(location);
        this.operator = operator;
        this.operand = operand;
    }

    @Override
    public PhpValue eval(Env env) {
        PhpValue value = operand.eval(env);
        return switch (operator) {
            case NOT -> PhpBool.of(!value.isTruthy());
            case NEGATE -> PhpArithmetic.negate(value);
            case IDENTITY -> PhpArithmetic.identity(value);
        };
    }
}
