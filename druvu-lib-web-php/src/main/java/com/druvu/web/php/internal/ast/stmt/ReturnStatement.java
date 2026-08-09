package com.druvu.web.php.internal.ast.stmt;

import com.druvu.web.php.internal.Location;
import com.druvu.web.php.internal.ast.PhpExpression;
import com.druvu.web.php.internal.ast.PhpStatement;
import com.druvu.web.php.internal.ast.Signal;
import com.druvu.web.php.internal.runtime.Env;
import com.druvu.web.php.internal.value.PhpInt;
import com.druvu.web.php.internal.value.PhpValue;

/**
 * {@code return $x;} at the top level of a template, which ends it there and hands the value back to whatever included
 * it. A bare {@code return;} yields 1, the value an include has when it says nothing.
 */
public final class ReturnStatement extends PhpStatement {

    private final PhpExpression value;

    /** @param value null for a bare {@code return;} */
    public ReturnStatement(Location location, PhpExpression value) {
        super(location);
        this.value = value;
    }

    @Override
    public Signal execute(Env env) {
        PhpValue returned = value == null ? PhpInt.of(1L) : value.eval(env);
        return new Signal.Return(returned);
    }
}
