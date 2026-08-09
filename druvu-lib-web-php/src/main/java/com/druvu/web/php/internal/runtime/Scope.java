package com.druvu.web.php.internal.runtime;

import com.druvu.web.php.internal.value.PhpValue;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The variables the executing template can see.
 *
 * <p>A template has one scope, and an included template shares its includer's — that sharing is the whole mechanism by
 * which a layout hands data to a partial. Only a closure body gets a scope of its own.
 *
 * <p>An unset variable is absent from the map rather than present with a null value, which is what lets {@code isset}
 * and an explicit {@code null} stay distinguishable.
 *
 * @author Deniss Larka
 */
public final class Scope {

    private final Map<String, PhpValue> variables = new HashMap<>();

    /** The value of a variable, or null when it is not set. */
    public PhpValue get(String name) {
        return variables.get(name);
    }

    public boolean isDefined(String name) {
        return variables.containsKey(name);
    }

    public void set(String name, PhpValue value) {
        variables.put(
                Objects.requireNonNull(name, "name"),
                Objects.requireNonNull(value, "value; an unset variable is absent, a null one holds PhpNull.NULL"));
    }

    public void unset(String name) {
        variables.remove(name);
    }

    /** The names currently set, in no particular order. Unmodifiable. */
    public Set<String> names() {
        return Collections.unmodifiableSet(variables.keySet());
    }

    public int size() {
        return variables.size();
    }
}
