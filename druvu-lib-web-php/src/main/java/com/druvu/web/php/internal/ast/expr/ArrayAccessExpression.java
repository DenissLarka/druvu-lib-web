package com.druvu.web.php.internal.ast.expr;

import com.druvu.web.php.internal.Location;
import com.druvu.web.php.internal.PhpSyntaxException;
import com.druvu.web.php.internal.ast.PhpExpression;
import com.druvu.web.php.internal.runtime.Env;
import com.druvu.web.php.internal.value.ArrayKey;
import com.druvu.web.php.internal.value.PhpArray;
import com.druvu.web.php.internal.value.PhpNull;
import com.druvu.web.php.internal.value.PhpString;
import com.druvu.web.php.internal.value.PhpValue;

/**
 * {@code $a['k']}, {@code $a[0]} and — with no index at all — the {@code $a[]} that only makes sense on the left of an
 * assignment.
 *
 * <p>Reading an index of a string yields one character. PHP counts those in bytes; this counts in code points, which is
 * the same decision already taken for {@code strlen} and friends and the one a layout author expects.
 */
public final class ArrayAccessExpression extends PhpExpression {

    private final PhpExpression target;
    private final PhpExpression index;

    public ArrayAccessExpression(Location location, PhpExpression target, PhpExpression index) {
        super(location);
        this.target = target;
        this.index = index;
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

    @Override
    public void unset(Env env) {
        if (index != null && target.evalQuietly(env) instanceof PhpArray array) {
            array.remove(index.eval(env).toKey());
        }
    }

    @Override
    public PhpExpression toAssignment(PhpExpression value) {
        return index == null
                ? new ArrayAppendAssignment(location(), target, value)
                : new ArrayElementAssignment(location(), target, index, value);
    }

    @Override
    public PhpArray resolveContainer(Env env) {
        PhpArray parent = target.resolveContainer(env);
        ArrayKey key = index == null
                ? ArrayKey.of(parent.nextIndex())
                : index.eval(env).toKey();
        if (parent.get(key) instanceof PhpArray existing) {
            return existing;
        }
        PhpArray created = PhpArray.empty();
        parent.put(key, created);
        return created;
    }

    private PhpValue read(Env env, boolean reporting) {
        if (index == null) {
            throw new PhpSyntaxException(location(), "Cannot use [] for reading");
        }
        PhpValue container = target.evalQuietly(env);
        PhpValue key = index.eval(env);
        if (container instanceof PhpArray array) {
            PhpValue value = array.get(key.toKey());
            if (value == null) {
                warn(env, reporting, "Undefined array key " + describe(key));
                return PhpNull.NULL;
            }
            return value;
        }
        if (container instanceof PhpString text) {
            return characterAt(env, reporting, text, key);
        }
        warn(env, reporting, "Trying to access an array offset on a value of type " + container.typeName());
        return PhpNull.NULL;
    }

    private PhpValue characterAt(Env env, boolean reporting, PhpString text, PhpValue index) {
        String value = text.value();
        int length = value.codePointCount(0, value.length());
        long position = index.toInt();
        if (position < 0) {
            position += length;
        }
        if (position < 0 || position >= length) {
            warn(env, reporting, "Uninitialized string offset " + describe(index));
            return PhpString.of("");
        }
        int offset = value.offsetByCodePoints(0, (int) position);
        return PhpString.of(new String(Character.toChars(value.codePointAt(offset))));
    }

    private void warn(Env env, boolean reporting, String message) {
        if (reporting) {
            env.warn(location(), message);
        }
    }

    private static String describe(PhpValue key) {
        return key instanceof PhpString text ? "\"" + text.value() + "\"" : key.toStr();
    }
}
