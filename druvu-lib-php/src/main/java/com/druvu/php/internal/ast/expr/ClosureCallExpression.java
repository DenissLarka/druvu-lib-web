package com.druvu.php.internal.ast.expr;

import com.druvu.php.Location;
import com.druvu.php.PhpProcessingException;
import com.druvu.php.internal.ast.PhpExpression;
import com.druvu.php.internal.runtime.Env;
import com.druvu.php.internal.value.PhpClosure;
import com.druvu.php.internal.value.PhpValue;
import java.util.ArrayList;
import java.util.List;

/** {@code $f(1)} — calling whatever a variable holds, which in this dialect can only ever be an arrow function. */
public final class ClosureCallExpression extends PhpExpression {

    private final PhpExpression callee;
    private final List<PhpExpression> arguments;

    public ClosureCallExpression(Location location, PhpExpression callee, List<PhpExpression> arguments) {
        super(location);
        this.callee = callee;
        this.arguments = List.copyOf(arguments);
    }

    @Override
    public PhpValue eval(Env env) {
        PhpValue target = callee.eval(env);
        if (!(target instanceof PhpClosure closure)) {
            throw new PhpProcessingException(
                    location() + ": a value of type " + target.typeName() + " is not callable");
        }
        List<PhpValue> values = new ArrayList<>(arguments.size());
        for (PhpExpression argument : arguments) {
            values.add(argument.eval(env).copyForAssignment());
        }
        return closure.call(values);
    }
}
