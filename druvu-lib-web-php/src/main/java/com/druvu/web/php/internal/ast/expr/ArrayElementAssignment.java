package com.druvu.web.php.internal.ast.expr;

import com.druvu.web.php.internal.Location;
import com.druvu.web.php.internal.ast.PhpExpression;
import com.druvu.web.php.internal.runtime.Env;
import com.druvu.web.php.internal.value.PhpValue;

/** {@code $a['k'] = ...}, growing {@code $a} and everything above it if it is not there yet. */
public final class ArrayElementAssignment extends PhpExpression {

    private final PhpExpression container;
    private final PhpExpression index;
    private final PhpExpression value;

    ArrayElementAssignment(Location location, PhpExpression container, PhpExpression index, PhpExpression value) {
        super(location);
        this.container = container;
        this.index = index;
        this.value = value;
    }

    @Override
    public PhpValue eval(Env env) {
        PhpValue assigned = value.eval(env).copyForAssignment();
        container.resolveContainer(env).put(index.eval(env).toKey(), assigned);
        return assigned;
    }
}
