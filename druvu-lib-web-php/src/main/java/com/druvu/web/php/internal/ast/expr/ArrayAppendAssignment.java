package com.druvu.web.php.internal.ast.expr;

import com.druvu.web.php.internal.Location;
import com.druvu.web.php.internal.ast.PhpExpression;
import com.druvu.web.php.internal.runtime.Env;
import com.druvu.web.php.internal.value.PhpValue;

/** {@code $a[] = ...}, which puts the value at the array's next free index. */
public final class ArrayAppendAssignment extends PhpExpression {

    private final PhpExpression container;
    private final PhpExpression value;

    ArrayAppendAssignment(Location location, PhpExpression container, PhpExpression value) {
        super(location);
        this.container = container;
        this.value = value;
    }

    @Override
    public PhpValue eval(Env env) {
        PhpValue assigned = value.eval(env).copyForAssignment();
        container.resolveContainer(env).append(assigned);
        return assigned;
    }
}
