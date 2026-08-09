package com.druvu.web.php.internal;

/**
 * Engine-wide policy for the PHP template dialect. One place to look up what the engine does and does not do, so the
 * answer never has to be reconstructed from scattered constants.
 *
 * <p>The dialect targets PHP 8.x semantics with one deliberate divergence: output is HTML-escaped by default and raw
 * output is the explicit opt-out.
 *
 * @param escapeOutput whether {@code echo}, {@code print} and {@code <?=} escape their output
 * @param shortOpenTag whether the bare {@code <?} open tag is recognised
 * @param maxIncludeDepth how deeply templates may include one another
 * @param maxLoopIterations safety valve: how many loop iterations a single render may run
 * @param debugFunctions whether the dump/inspection functions are callable
 * @author Deniss Larka
 */
public record PhpEngineConfig(
        boolean escapeOutput,
        boolean shortOpenTag,
        int maxIncludeDepth,
        long maxLoopIterations,
        boolean debugFunctions) {

    /** Include depth of the pre-rewrite engine, kept so existing templates behave the same. */
    public static final int DEFAULT_MAX_INCLUDE_DEPTH = 100;

    /** Large enough that no honest layout hits it, small enough that a runaway loop cannot pin the server. */
    public static final long DEFAULT_MAX_LOOP_ITERATIONS = 1_000_000L;

    /** The policy every template runs under unless the host says otherwise. */
    public static final PhpEngineConfig DEFAULTS =
            new PhpEngineConfig(true, false, DEFAULT_MAX_INCLUDE_DEPTH, DEFAULT_MAX_LOOP_ITERATIONS, false);

    public PhpEngineConfig {
        if (maxIncludeDepth < 1) {
            throw new IllegalArgumentException("maxIncludeDepth must be at least 1, got " + maxIncludeDepth);
        }
        if (maxLoopIterations < 1) {
            throw new IllegalArgumentException("maxLoopIterations must be at least 1, got " + maxLoopIterations);
        }
    }

    /** The same policy with output escaping turned off, for hosts that want stock PHP behaviour. */
    public PhpEngineConfig withoutEscaping() {
        return new PhpEngineConfig(false, shortOpenTag, maxIncludeDepth, maxLoopIterations, debugFunctions);
    }

    /** The same policy with the dump/inspection functions callable. */
    public PhpEngineConfig withDebugFunctions() {
        return new PhpEngineConfig(escapeOutput, shortOpenTag, maxIncludeDepth, maxLoopIterations, true);
    }
}
