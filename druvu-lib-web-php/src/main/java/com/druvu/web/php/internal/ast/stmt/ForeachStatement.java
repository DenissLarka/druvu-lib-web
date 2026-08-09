package com.druvu.web.php.internal.ast.stmt;

import com.druvu.web.php.internal.Location;
import com.druvu.web.php.internal.ast.PhpExpression;
import com.druvu.web.php.internal.ast.PhpStatement;
import com.druvu.web.php.internal.ast.Signal;
import com.druvu.web.php.internal.runtime.Env;
import com.druvu.web.php.internal.value.ArrayKey;
import com.druvu.web.php.internal.value.PhpArray;
import com.druvu.web.php.internal.value.PhpInt;
import com.druvu.web.php.internal.value.PhpString;
import com.druvu.web.php.internal.value.PhpValue;
import java.util.Map;

/**
 * {@code foreach ($items as $item)} and {@code foreach ($items as $key => $item)}.
 *
 * <p>By value only. The array is copied before the walk begins, which is both what PHP does and what makes a body that
 * changes the array safe to run.
 */
public final class ForeachStatement extends PhpStatement {

    private final PhpExpression subject;
    private final String keyName;
    private final String valueName;
    private final PhpStatement body;

    /** @param keyName the {@code $key} of the {@code $key => $value} form, or null when there is none */
    public ForeachStatement(
            Location location, PhpExpression subject, String keyName, String valueName, PhpStatement body) {
        super(location);
        this.subject = subject;
        this.keyName = keyName;
        this.valueName = valueName;
        this.body = body;
    }

    @Override
    public Signal execute(Env env) {
        PhpValue value = subject.eval(env);
        if (!(value instanceof PhpArray array)) {
            env.warn(location(), "foreach() needs an array but was given " + value.typeName());
            return null;
        }

        LoopGuard guard = new LoopGuard(env, location());
        for (Map.Entry<ArrayKey, PhpValue> entry : array.copy().entries().entrySet()) {
            guard.tick();
            if (keyName != null) {
                env.setVariable(keyName, asValue(entry.getKey()));
            }
            env.setVariable(valueName, entry.getValue().copyForAssignment());

            LoopStep step = LoopStep.of(body.execute(env));
            if (step.finished()) {
                return step.propagate();
            }
        }
        return null;
    }

    private static PhpValue asValue(ArrayKey key) {
        return key instanceof ArrayKey.IntKey index ? PhpInt.of(index.value()) : PhpString.of(text(key));
    }

    private static String text(ArrayKey key) {
        return ((ArrayKey.StringKey) key).value();
    }
}
