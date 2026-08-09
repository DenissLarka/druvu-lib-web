package com.druvu.web.php.internal.ast.expr;

import com.druvu.web.php.internal.Location;
import com.druvu.web.php.internal.PhpProcessingException;
import com.druvu.web.php.internal.ast.PhpExpression;
import com.druvu.web.php.internal.runtime.Env;
import com.druvu.web.php.internal.value.PhpComparison;
import com.druvu.web.php.internal.value.PhpValue;
import java.util.List;

/**
 * {@code match ($x) { 1, 2 => 'a', default => 'b' }}.
 *
 * <p>Comparison is strict, so {@code match ("1")} does not meet a {@code 1} arm — the reason to reach for it over
 * {@code switch} in the first place. There is no fall-through, and an unmatched subject with no default is an error
 * rather than a silent null.
 */
public final class MatchExpression extends PhpExpression {

    /** One arm: the values it answers to, and what it produces. */
    public record Arm(List<PhpExpression> conditions, PhpExpression result) {

        public Arm {
            conditions = List.copyOf(conditions);
        }
    }

    private final PhpExpression subject;
    private final List<Arm> arms;
    private final PhpExpression fallback;

    /** @param fallback the {@code default} arm, or null when the match has none */
    public MatchExpression(Location location, PhpExpression subject, List<Arm> arms, PhpExpression fallback) {
        super(location);
        this.subject = subject;
        this.arms = List.copyOf(arms);
        this.fallback = fallback;
    }

    @Override
    public PhpValue eval(Env env) {
        PhpValue tested = subject.eval(env);
        for (Arm arm : arms) {
            for (PhpExpression condition : arm.conditions()) {
                if (PhpComparison.identical(tested, condition.eval(env))) {
                    return arm.result().eval(env);
                }
            }
        }
        if (fallback != null) {
            return fallback.eval(env);
        }
        throw new PhpProcessingException(location() + ": unhandled match case " + tested.toStr());
    }
}
