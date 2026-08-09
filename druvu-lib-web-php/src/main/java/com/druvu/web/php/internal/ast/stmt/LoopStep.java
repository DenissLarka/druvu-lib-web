package com.druvu.web.php.internal.ast.stmt;

import com.druvu.web.php.internal.ast.Signal;

/**
 * What a loop does after one turn of its body.
 *
 * <p>Every loop reacts to {@code break} and {@code continue} the same way, and this is that reaction written once: a
 * level is absorbed here, and whatever is left over travels outward to the loop above.
 *
 * @param finished whether this loop stops now
 * @param propagate what the loop reports to its own caller, or null to simply fall through
 */
record LoopStep(boolean finished, Signal propagate) {

    private static final LoopStep KEEP_GOING = new LoopStep(false, null);

    static LoopStep of(Signal signal) {
        if (signal == null) {
            return KEEP_GOING;
        }
        if (signal instanceof Signal.Continue skip) {
            Signal outer = skip.outer();
            return outer == null ? KEEP_GOING : new LoopStep(true, outer);
        }
        if (signal instanceof Signal.Break stop) {
            return new LoopStep(true, stop.outer());
        }
        // A return leaves every loop it is inside.
        return new LoopStep(true, signal);
    }
}
