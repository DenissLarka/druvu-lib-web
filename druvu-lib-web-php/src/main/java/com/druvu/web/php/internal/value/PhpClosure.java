package com.druvu.web.php.internal.value;

import com.druvu.web.php.internal.PhpProcessingException;
import java.util.List;
import java.util.Objects;

/**
 * An arrow function, once evaluated.
 *
 * <p>PHP calls this an object of class Closure, and it behaves like one: always truthy, never convertible to a string,
 * equal only to itself.
 *
 * <p>What the closure actually does is supplied as a {@link Body} rather than as a syntax tree, so the value layer
 * stays unaware that a syntax tree exists at all.
 *
 * @author Deniss Larka
 */
public final class PhpClosure extends PhpValue {

    /** What a closure does when called. */
    @FunctionalInterface
    public interface Body {
        PhpValue call(List<PhpValue> arguments);
    }

    private final Body body;

    private PhpClosure(Body body) {
        this.body = body;
    }

    public static PhpClosure of(Body body) {
        return new PhpClosure(Objects.requireNonNull(body, "body"));
    }

    public PhpValue call(List<PhpValue> arguments) {
        return body.call(arguments);
    }

    /** PHP reports a closure as an object, because that is what it is. */
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
        throw new PhpProcessingException("Object of class Closure could not be converted to string");
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
        throw new PhpProcessingException("Illegal offset type: a closure cannot be used as an array key");
    }

    @Override
    public String toString() {
        return "closure";
    }
}
