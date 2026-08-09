package com.druvu.web.php.internal.ast.stmt;

import com.druvu.web.php.internal.Location;
import com.druvu.web.php.internal.ast.PhpStatement;
import com.druvu.web.php.internal.ast.Signal;
import com.druvu.web.php.internal.runtime.Env;

/** {@code break;} or {@code break 2;}. */
public final class BreakStatement extends PhpStatement {

    private final int levels;

    public BreakStatement(Location location, int levels) {
        super(location);
        this.levels = levels;
    }

    @Override
    public Signal execute(Env env) {
        return new Signal.Break(levels);
    }
}
