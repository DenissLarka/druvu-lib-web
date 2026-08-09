package com.druvu.web.php.internal.value;

import com.druvu.web.php.internal.PhpProcessingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Something the host handed the template: a record, or anything with getters.
 *
 * <p>Read-only, deliberately and completely. A template can ask an object for a property and can ask nothing else of it
 * — there is no {@code new}, no method call, no assignment. A layout that could reach back into the application's
 * objects and change them would be a layout that has to be read as code.
 *
 * <p>Record components are found first, then {@code getX()} and {@code isX()}, then a public field. The host's class
 * must be public and its package exported, because that is what reflection across a module boundary needs; when it is
 * not, the template author gets told so rather than getting a null.
 *
 * @author Deniss Larka
 */
public final class PhpObject extends PhpValue {

    private final Object host;

    private PhpObject(Object host) {
        this.host = host;
    }

    public static PhpObject of(Object host) {
        return new PhpObject(Objects.requireNonNull(host, "host"));
    }

    public Object host() {
        return host;
    }

    /** The named property, or null when the object has no such thing. */
    public Object property(String name) {
        Method reader = readerFor(name);
        if (reader == null) {
            return null;
        }
        try {
            return reader.invoke(host);
        } catch (IllegalAccessException notReachable) {
            throw new PhpProcessingException("Cannot read $" + name + " from "
                    + host.getClass().getName() + ": its package is not open to the template engine");
        } catch (InvocationTargetException failed) {
            throw new PhpProcessingException(
                    "Reading $" + name + " from " + host.getClass().getName() + " failed", failed.getCause());
        }
    }

    public boolean hasProperty(String name) {
        return readerFor(name) != null;
    }

    /** A record component, then {@code getX()}, then {@code isX()} — the three ways a host might have spelled it. */
    private Method readerFor(String name) {
        for (String candidate : new String[] {name, getterFor("get", name), getterFor("is", name)}) {
            Method method = methodNamed(candidate);
            if (method != null) {
                return method;
            }
        }
        return null;
    }

    private Method methodNamed(String candidate) {
        try {
            Method method = host.getClass().getMethod(candidate);
            boolean reads = method.getParameterCount() == 0 && method.getReturnType() != void.class;
            return reads ? method : null;
        } catch (NoSuchMethodException noSuchSpelling) {
            return null;
        }
    }

    private static String getterFor(String prefix, String name) {
        return prefix + Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    @Override
    public String typeName() {
        return "object";
    }

    @Override
    public boolean isTruthy() {
        return true;
    }

    @Override
    public String toStr() {
        throw new PhpProcessingException(
                "Object of class " + host.getClass().getSimpleName() + " could not be converted to string");
    }

    @Override
    public long toInt() {
        return 1L;
    }

    @Override
    public double toFloat() {
        return 1.0;
    }

    @Override
    public ArrayKey toKey() {
        throw new PhpProcessingException("Illegal offset type: an object cannot be used as an array key");
    }

    @Override
    public String toString() {
        return "object(" + host.getClass().getSimpleName() + ")";
    }
}
