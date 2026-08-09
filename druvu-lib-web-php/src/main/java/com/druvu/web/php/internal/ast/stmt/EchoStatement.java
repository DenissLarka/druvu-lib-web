package com.druvu.web.php.internal.ast.stmt;

import com.druvu.web.php.internal.Location;
import com.druvu.web.php.internal.ast.PhpExpression;
import com.druvu.web.php.internal.ast.PhpStatement;
import com.druvu.web.php.internal.ast.Signal;
import com.druvu.web.php.internal.runtime.Env;
import com.druvu.web.php.internal.runtime.Output;
import java.util.List;

/**
 * {@code echo $a, $b;} and the {@code <?= ... ?>} tag, which is the same thing spelled shorter.
 *
 * <p>What it writes is escaped unless the value says it is already safe - see {@link Output}.
 */
public final class EchoStatement extends PhpStatement {

    private final List<PhpExpression> values;

    public EchoStatement(Location location, List<PhpExpression> values) {
        super(location);
        this.values = List.copyOf(values);
    }

    @Override
    public Signal execute(Env env) {
        for (PhpExpression value : values) {
            Output.write(env, value.eval(env));
        }
        return null;
    }
}
