package com.druvu.web.php.internal.ast.expr;

import com.druvu.web.php.internal.Location;
import com.druvu.web.php.internal.ast.PhpExpression;
import com.druvu.web.php.internal.runtime.Env;
import com.druvu.web.php.internal.value.PhpBool;
import com.druvu.web.php.internal.value.PhpValue;

/**
 * {@code &&}, {@code ||} and their word forms, plus {@code xor}.
 *
 * <p>Separate from {@link BinaryExpression} because the answer often does not need the right-hand side at all, and a
 * template relies on that: {@code $user && $user['name']} is only safe because the second half never runs when the
 * first is false.
 */
public final class LogicalExpression extends PhpExpression {

    /** {@code and} and {@code &&} differ only in precedence, so they share an operator here. */
    public enum Operator {
        AND,
        OR,
        XOR
    }

    private final Operator operator;
    private final PhpExpression left;
    private final PhpExpression right;

    public LogicalExpression(Location location, Operator operator, PhpExpression left, PhpExpression right) {
        super(location);
        this.operator = operator;
        this.left = left;
        this.right = right;
    }

    @Override
    public PhpValue eval(Env env) {
        boolean first = left.eval(env).isTruthy();
        return switch (operator) {
            case AND -> PhpBool.of(first && right.eval(env).isTruthy());
            case OR -> PhpBool.of(first || right.eval(env).isTruthy());
            case XOR -> PhpBool.of(first ^ right.eval(env).isTruthy());
        };
    }
}
