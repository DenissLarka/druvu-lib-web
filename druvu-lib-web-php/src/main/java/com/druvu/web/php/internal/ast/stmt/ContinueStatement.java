package com.druvu.web.php.internal.ast.stmt;

import com.druvu.web.php.internal.Location;
import com.druvu.web.php.internal.ast.PhpStatement;
import com.druvu.web.php.internal.ast.Signal;
import com.druvu.web.php.internal.runtime.Env;

/** {@code continue;} or {@code continue 2;}. */
public final class ContinueStatement extends PhpStatement {

    private final int levels;

    public ContinueStatement(Location location, int levels) {
        super(location);
        this.levels = levels;
    }

    @Override
    public Signal execute(Env env) {
        return new Signal.Continue(levels);
    }
}
