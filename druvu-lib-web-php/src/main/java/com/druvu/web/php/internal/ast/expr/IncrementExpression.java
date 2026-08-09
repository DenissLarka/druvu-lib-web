package com.druvu.web.php.internal.ast.expr;

import com.druvu.web.php.internal.Location;
import com.druvu.web.php.internal.PhpProcessingException;
import com.druvu.web.php.internal.ast.PhpExpression;
import com.druvu.web.php.internal.runtime.Env;
import com.druvu.web.php.internal.value.NumericStrings;
import com.druvu.web.php.internal.value.PhpArithmetic;
import com.druvu.web.php.internal.value.PhpInt;
import com.druvu.web.php.internal.value.PhpNull;
import com.druvu.web.php.internal.value.PhpString;
import com.druvu.web.php.internal.value.PhpValue;

/**
 * {@code ++$i} and {@code $i++}, and their decrementing twins.
 *
 * <p>Two of PHP's oddities are kept because loops depend on them: {@code ++} on null gives 1, while {@code --} on null
 * does nothing at all. A third is not: {@code ++} on a non-numeric string yields {@code "b"} from {@code "a"} in PHP, a
 * feature PHP itself deprecated, and here it is refused rather than quietly surprising anyone.
 */
public final class IncrementExpression extends PhpExpression {

    private final PhpExpression target;
    private final boolean increasing;
    private final boolean prefix;

    public IncrementExpression(Location location, PhpExpression target, boolean increasing, boolean prefix) {
        super(location);
        this.target = target;
        this.increasing = increasing;
        this.prefix = prefix;
    }

    @Override
    public PhpValue eval(Env env) {
        PhpValue before = target.eval(env);
        PhpValue after = step(before);

        PhpExpression assignment = target.toAssignment(new LiteralExpression(location(), after));
        if (assignment == null) {
            throw new PhpProcessingException(location() + ": cannot increment or decrement this expression");
        }
        assignment.eval(env);

        return prefix ? after : before;
    }

    private PhpValue step(PhpValue value) {
        if (value instanceof PhpNull) {
            return increasing ? PhpInt.of(1L) : PhpNull.NULL;
        }
        if (value instanceof PhpString text && !NumericStrings.startsWithNumber(text.value())) {
            throw new PhpProcessingException(
                    location() + ": cannot increment or decrement the non-numeric string \"" + text.value() + "\"");
        }
        return increasing ? PhpArithmetic.add(value, PhpInt.of(1L)) : PhpArithmetic.subtract(value, PhpInt.of(1L));
    }
}
