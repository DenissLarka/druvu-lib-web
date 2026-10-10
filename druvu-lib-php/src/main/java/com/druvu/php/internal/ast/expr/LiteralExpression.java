package com.druvu.php.internal.ast.expr;

import com.druvu.php.Location;
import com.druvu.php.internal.ast.PhpExpression;
import com.druvu.php.internal.runtime.Env;
import com.druvu.php.internal.value.PhpValue;
import java.util.Objects;

/** A value written straight into the template: a number, a quoted string, {@code true}, {@code false}, {@code null}. */
public final class LiteralExpression extends PhpExpression {

    private final PhpValue value;

    public LiteralExpression(Location location, PhpValue value) {
        super(location);
        this.value = Objects.requireNonNull(value, "value");
    }

    @Override
    public PhpValue eval(Env env) {
        return value;
    }
}
