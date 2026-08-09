package com.druvu.web.php.internal.ast.expr;

import com.druvu.web.php.internal.Location;
import com.druvu.web.php.internal.ast.PhpExpression;
import com.druvu.web.php.internal.runtime.Env;
import com.druvu.web.php.internal.runtime.Output;
import com.druvu.web.php.internal.value.PhpInt;
import com.druvu.web.php.internal.value.PhpValue;

/**
 * {@code print $x}.
 *
 * <p>Almost {@code echo}, with two differences: it takes exactly one operand, and it is an expression whose value is 1,
 * which is what lets {@code $ok or print "failed"} work.
 */
public final class PrintExpression extends PhpExpression {

    private final PhpExpression value;

    public PrintExpression(Location location, PhpExpression value) {
        super(location);
        this.value = value;
    }

    @Override
    public PhpValue eval(Env env) {
        Output.write(env, value.eval(env));
        return PhpInt.of(1L);
    }
}
