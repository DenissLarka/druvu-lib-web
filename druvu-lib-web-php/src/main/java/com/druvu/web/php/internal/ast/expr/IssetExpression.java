package com.druvu.web.php.internal.ast.expr;

import com.druvu.web.php.internal.Location;
import com.druvu.web.php.internal.ast.PhpExpression;
import com.druvu.web.php.internal.runtime.Env;
import com.druvu.web.php.internal.value.PhpBool;
import com.druvu.web.php.internal.value.PhpValue;
import java.util.List;

/** {@code isset($a, $b)}: true when every operand is set and none of them is null. */
public final class IssetExpression extends PhpExpression {

    private final List<PhpExpression> operands;

    public IssetExpression(Location location, List<PhpExpression> operands) {
        super(location);
        this.operands = List.copyOf(operands);
    }

    @Override
    public PhpValue eval(Env env) {
        for (PhpExpression operand : operands) {
            if (!operand.isSet(env)) {
                return PhpBool.FALSE;
            }
        }
        return PhpBool.TRUE;
    }
}
