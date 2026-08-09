package com.druvu.web.php.internal.ast.stmt;

import com.druvu.web.php.internal.Location;
import com.druvu.web.php.internal.ast.PhpExpression;
import com.druvu.web.php.internal.ast.PhpStatement;
import com.druvu.web.php.internal.ast.Signal;
import com.druvu.web.php.internal.runtime.Env;
import java.util.List;

/** {@code unset($a, $b['k'])}. */
public final class UnsetStatement extends PhpStatement {

    private final List<PhpExpression> targets;

    public UnsetStatement(Location location, List<PhpExpression> targets) {
        super(location);
        this.targets = List.copyOf(targets);
    }

    @Override
    public Signal execute(Env env) {
        for (PhpExpression target : targets) {
            target.unset(env);
        }
        return null;
    }
}
