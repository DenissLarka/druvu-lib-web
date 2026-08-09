package com.druvu.web.php.internal.ast.expr;

import com.druvu.web.php.internal.Location;
import com.druvu.web.php.internal.ast.PhpExpression;
import com.druvu.web.php.internal.runtime.Env;
import com.druvu.web.php.internal.runtime.Scope;
import com.druvu.web.php.internal.value.PhpClosure;
import com.druvu.web.php.internal.value.PhpNull;
import com.druvu.web.php.internal.value.PhpValue;
import java.util.List;
import java.util.Map;

/**
 * {@code fn ($x) => $x * 2}.
 *
 * <p>The only kind of function a template can write. It captures what it can see at the moment it is created, by value:
 * changing a variable afterwards does not change what the closure computes. Its body is one expression, which is what
 * keeps it a value and not a second, weaker way to write a program inside a layout.
 */
public final class ArrowFunctionExpression extends PhpExpression {

    private final List<String> parameters;
    private final PhpExpression body;

    public ArrowFunctionExpression(Location location, List<String> parameters, PhpExpression body) {
        super(location);
        this.parameters = List.copyOf(parameters);
        this.body = body;
    }

    @Override
    public PhpValue eval(Env env) {
        Map<String, PhpValue> captured = env.captureVariables();
        return PhpClosure.of(arguments -> invoke(env, captured, arguments));
    }

    private PhpValue invoke(Env env, Map<String, PhpValue> captured, List<PhpValue> arguments) {
        Scope previous = env.pushScope();
        try {
            captured.forEach(env::setVariable);
            for (int i = 0; i < parameters.size(); i++) {
                PhpValue argument = i < arguments.size() ? arguments.get(i).copyForAssignment() : PhpNull.NULL;
                env.setVariable(parameters.get(i), argument);
            }
            return body.eval(env);
        } finally {
            env.popScope(previous);
        }
    }
}
