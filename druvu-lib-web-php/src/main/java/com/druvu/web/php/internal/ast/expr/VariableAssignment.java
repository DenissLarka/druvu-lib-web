package com.druvu.web.php.internal.ast.expr;

import com.druvu.web.php.internal.Location;
import com.druvu.web.php.internal.ast.PhpExpression;
import com.druvu.web.php.internal.runtime.Env;
import com.druvu.web.php.internal.value.PhpValue;

/** {@code $name = ...}, which is an expression: it has the assigned value. */
public final class VariableAssignment extends PhpExpression {

    private final String name;
    private final PhpExpression value;

    VariableAssignment(Location location, String name, PhpExpression value) {
        super(location);
        this.name = name;
        this.value = value;
    }

    @Override
    public PhpValue eval(Env env) {
        PhpValue assigned = value.eval(env).copyForAssignment();
        env.setVariable(name, assigned);
        return assigned;
    }
}
