package com.druvu.web.php.internal.ast.expr;

import com.druvu.web.php.internal.Location;
import com.druvu.web.php.internal.ast.PhpExpression;
import com.druvu.web.php.internal.runtime.Env;
import com.druvu.web.php.internal.runtime.PhpFunction;
import com.druvu.web.php.internal.value.PhpValue;
import java.util.ArrayList;
import java.util.List;

/** {@code strlen($s)} — a call to one of the engine's own functions, by name. */
public final class FunctionCallExpression extends PhpExpression {

    private final String name;
    private final List<PhpExpression> arguments;

    public FunctionCallExpression(Location location, String name, List<PhpExpression> arguments) {
        super(location);
        this.name = name;
        this.arguments = List.copyOf(arguments);
    }

    public String name() {
        return name;
    }

    @Override
    public PhpValue eval(Env env) {
        PhpFunction function = env.findFunction(name);
        if (function == null) {
            throw new com.druvu.web.php.internal.PhpProcessingException(
                    location() + ": call to undefined function " + name + "()");
        }
        List<PhpValue> values = new ArrayList<>(arguments.size());
        for (PhpExpression argument : arguments) {
            values.add(argument.eval(env).copyForAssignment());
        }

        PhpValue result = function.call(env, values);
        if (env.functionWritesBackFirstArgument(name) && !arguments.isEmpty()) {
            writeBack(env, values.get(0));
        }
        return result;
    }

    /**
     * The sorts rearrange the array they were given, so what they rearranged goes back where it came from.
     *
     * <p>Arguments reach a function by value, which is what makes every other function in the library unable to
     * surprise a template author. These few are the exception PHP's own {@code sort($items)} requires, and confining
     * the write-back to this method is what keeps it from becoming a general reference mechanism.
     */
    private void writeBack(Env env, PhpValue sorted) {
        PhpExpression target = arguments.get(0).toAssignment(new LiteralExpression(location(), sorted));
        if (target == null) {
            throw new com.druvu.web.php.internal.PhpProcessingException(
                    location() + ": " + name + "() must be given a variable it can sort");
        }
        target.eval(env);
    }
}
