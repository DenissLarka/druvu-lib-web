package com.druvu.web.php.internal.ast.expr;

import com.druvu.web.php.internal.Location;
import com.druvu.web.php.internal.ast.PhpExpression;
import com.druvu.web.php.internal.runtime.Env;
import com.druvu.web.php.internal.runtime.HostValues;
import com.druvu.web.php.internal.value.PhpNull;
import com.druvu.web.php.internal.value.PhpObject;
import com.druvu.web.php.internal.value.PhpValue;

/**
 * {@code $order->total} and {@code $order?->total}.
 *
 * <p>Reading only. There is nothing to assign to and no method to call: the host decides what a template can see by
 * choosing what it hands over, and this is the whole of what the template can do with it.
 */
public final class PropertyExpression extends PhpExpression {

    private final PhpExpression target;
    private final String name;
    private final boolean nullSafe;

    public PropertyExpression(Location location, PhpExpression target, String name, boolean nullSafe) {
        super(location);
        this.target = target;
        this.name = name;
        this.nullSafe = nullSafe;
    }

    @Override
    public PhpValue eval(Env env) {
        return read(env, true);
    }

    @Override
    public PhpValue evalQuietly(Env env) {
        return read(env, false);
    }

    @Override
    public boolean isSet(Env env) {
        return !(read(env, false) instanceof PhpNull);
    }

    private PhpValue read(Env env, boolean reporting) {
        PhpValue owner = target.evalQuietly(env);
        if (owner instanceof PhpNull) {
            if (!nullSafe && reporting) {
                env.warn(location(), "Reading $" + name + " on null");
            }
            return PhpNull.NULL;
        }
        if (!(owner instanceof PhpObject object)) {
            if (reporting) {
                env.warn(location(), "Reading $" + name + " on a value of type " + owner.typeName());
            }
            return PhpNull.NULL;
        }
        if (!object.hasProperty(name)) {
            if (reporting) {
                env.warn(location(), "Undefined property $" + name + " on " + object);
            }
            return PhpNull.NULL;
        }
        return HostValues.of(object.property(name));
    }
}
