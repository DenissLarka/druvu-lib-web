package com.druvu.web.php.internal.ast;

import com.druvu.web.php.internal.Location;
import com.druvu.web.php.internal.runtime.Env;

/**
 * One executable statement of a parsed template.
 *
 * <p>The tree executes directly: a statement does its work against the {@link Env} and reports what should happen next.
 * There is no compile step and no bytecode backend, which keeps the thing a reader has to follow — parse tree in,
 * output out — down to one hop.
 *
 * <p>Every statement knows where it came from, so any error it raises can name a file and a line rather than an offset
 * into a fragment.
 *
 * @author Deniss Larka
 */
public abstract class PhpStatement {

    private final Location location;

    /**
     * Assigns without validating on purpose: the class is extensible, and a constructor that can throw in a non-final
     * class is a finalizer-attack surface (SpotBugs {@code CT_CONSTRUCTOR_THROW}).
     */
    protected PhpStatement(Location location) {
        this.location = location;
    }

    /** Where this statement starts in its template. */
    public final Location location() {
        return location;
    }

    /**
     * Executes this statement.
     *
     * @param env the state of the render in progress
     * @return null to fall through to the next statement, or the signal that interrupts it
     */
    public abstract Signal execute(Env env);
}
