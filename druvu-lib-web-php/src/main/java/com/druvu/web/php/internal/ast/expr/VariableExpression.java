package com.druvu.web.php.internal.ast.expr;

import com.druvu.web.php.internal.Location;
import com.druvu.web.php.internal.PhpProcessingException;
import com.druvu.web.php.internal.ast.PhpExpression;
import com.druvu.web.php.internal.runtime.Env;
import com.druvu.web.php.internal.value.PhpArray;
import com.druvu.web.php.internal.value.PhpNull;
import com.druvu.web.php.internal.value.PhpString;
import com.druvu.web.php.internal.value.PhpValue;
import java.util.Objects;

/** {@code $name}. */
public final class VariableExpression extends PhpExpression {

    private final String name;

    public VariableExpression(Location location, String name) {
        super(location);
        this.name = Objects.requireNonNull(name, "name");
    }

    public String name() {
        return name;
    }

    @Override
    public PhpValue eval(Env env) {
        PhpValue value = env.getVariable(name);
        if (value == null) {
            env.warn(location(), "Undefined variable $" + name);
            return PhpNull.NULL;
        }
        return value;
    }

    @Override
    public PhpValue evalQuietly(Env env) {
        PhpValue value = env.getVariable(name);
        return value == null ? PhpNull.NULL : value;
    }

    @Override
    public boolean isSet(Env env) {
        return env.isDefined(name) && !(env.getVariable(name) instanceof PhpNull);
    }

    @Override
    public void unset(Env env) {
        env.unsetVariable(name);
    }

    @Override
    public PhpExpression toAssignment(PhpExpression value) {
        return new VariableAssignment(location(), name, value);
    }

    /** Writing an index into an unset variable creates the array, which is how PHP grows one. */
    @Override
    public PhpArray resolveContainer(Env env) {
        PhpValue existing = env.getVariable(name);
        if (existing instanceof PhpArray array) {
            return array;
        }
        if (existing != null && !(existing instanceof PhpNull) && !isEmptyString(existing)) {
            throw new PhpProcessingException(
                    location() + ": cannot use a value of type " + existing.typeName() + " as an array");
        }
        PhpArray created = PhpArray.empty();
        env.setVariable(name, created);
        return created;
    }

    private static boolean isEmptyString(PhpValue value) {
        return value instanceof PhpString text && text.value().isEmpty();
    }
}
