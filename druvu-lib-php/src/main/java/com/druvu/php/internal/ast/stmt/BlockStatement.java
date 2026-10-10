package com.druvu.php.internal.ast.stmt;

import com.druvu.php.Location;
import com.druvu.php.internal.ast.PhpStatement;
import com.druvu.php.internal.ast.Signal;
import com.druvu.php.internal.runtime.Env;
import java.util.List;

/** A run of statements executed in order, stopping at the first one that reports anything. */
public final class BlockStatement extends PhpStatement {

    private final List<PhpStatement> statements;

    public BlockStatement(Location location, List<PhpStatement> statements) {
        super(location);
        this.statements = List.copyOf(statements);
    }

    @Override
    public Signal execute(Env env) {
        for (PhpStatement statement : statements) {
            Signal signal = statement.execute(env);
            if (signal != null) {
                return signal;
            }
        }
        return null;
    }
}
