package com.druvu.web.php.internal.ast;

import com.druvu.web.php.internal.Location;
import com.druvu.web.php.internal.PhpProcessingException;
import com.druvu.web.php.internal.runtime.Env;
import com.druvu.web.php.internal.value.PhpArray;
import com.druvu.web.php.internal.value.PhpNull;
import com.druvu.web.php.internal.value.PhpValue;

/**
 * One expression in a parsed template: something that has a value.
 *
 * <p>Four questions can be asked of an expression, and only the first is always interesting. The other three are what
 * let PHP's more awkward constructs share one mechanism instead of each growing its own: reading without complaining
 * (what {@code ??} and {@code isset} need), turning into an assignment (what makes {@code $x}, {@code $a['k']} and
 * {@code $a[]} one feature rather than three), and naming a container to write into (what grows {@code $a['x']['y'] =
 * 1} out of nothing).
 *
 * @author Deniss Larka
 */
public abstract class PhpExpression {

    private final Location location;

    /**
     * Assigns without validating on purpose: the class is extensible, and a constructor that can throw in a non-final
     * class is a finalizer-attack surface (SpotBugs {@code CT_CONSTRUCTOR_THROW}).
     */
    protected PhpExpression(Location location) {
        this.location = location;
    }

    /** Where this expression starts in its template. */
    public final Location location() {
        return location;
    }

    /** The value of this expression. */
    public abstract PhpValue eval(Env env);

    /**
     * The value, without reporting anything that is not set. Probing for optional data is how templates are written,
     * not a mistake, so {@code ??} and {@code isset} go through here.
     */
    public PhpValue evalQuietly(Env env) {
        return eval(env);
    }

    /** Whether this names something that is set and is not null — the question {@code isset} asks. */
    public boolean isSet(Env env) {
        return !(evalQuietly(env) instanceof PhpNull);
    }

    /** The expression that stores {@code value} where this one points, or null when this cannot be assigned to. */
    public PhpExpression toAssignment(PhpExpression value) {
        return null;
    }

    /** Removes whatever this expression names, for {@code unset}. */
    public void unset(Env env) {
        throw new PhpProcessingException(location() + ": this expression cannot be unset");
    }

    /** The array this expression names, creating it if it is not there yet, as PHP does when writing into one. */
    public PhpArray resolveContainer(Env env) {
        throw new PhpProcessingException(location() + ": this expression cannot hold array elements");
    }
}
