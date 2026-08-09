package com.druvu.web.core.handlers.attr;

import jakarta.servlet.ServletContext;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @author : Deniss Larka <br>
 *     on 09 June 2024
 */
public class Attributes {

    private final AttributesBackend backend;

    public static GlobalAttributesImpl from(ServletContext context) {
        return new GlobalAttributesImpl(new AttributesBackend.ServletContextBackend(context));
    }

    /**
     * This class stays open ({@link GlobalAttributesImpl} extends it), so the constructor must not throw: a throw after
     * {@code Object.<init>} leaves a partially built instance that a malicious subclass can resurrect from its
     * finalizer (SEI CERT OBJ-11, SpotBugs CT_CONSTRUCTOR_THROW). Subclasses validate in the {@code super(...)}
     * argument expression instead, which runs first.
     *
     * @param backend the attribute backend, already validated by the caller
     */
    public Attributes(AttributesBackend backend) {
        this.backend = backend;
    }

    public <C> C get(String key) {
        if (backend.getAttribute(key) == null) {
            throw new IllegalStateException("No attribute found:" + key);
        }
        return (C) backend.getAttribute(key);
    }

    public boolean has(String key) {
        return backend.getAttribute(key) != null;
    }

    protected <K, V> Map<K, V> map(String keyName) {
        return Map.copyOf(Objects.requireNonNull(get(keyName), noValueForKeyError(keyName)));
    }

    protected <K> Set<K> set(String keyName) {
        return Set.copyOf(Objects.requireNonNull(get(keyName), noValueForKeyError(keyName)));
    }

    protected String string(String keyName) {
        return Objects.requireNonNull(get(keyName), noValueForKeyError(keyName));
    }

    private static String noValueForKeyError(String keyName) {
        return String.format("No %s in context", keyName);
    }
}
