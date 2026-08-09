package com.druvu.web.php.internal.builtin;

import com.druvu.web.php.internal.PhpProcessingException;
import com.druvu.web.php.internal.runtime.FunctionRegistry;
import com.druvu.web.php.internal.value.PhpArithmetic;
import com.druvu.web.php.internal.value.PhpFloat;
import com.druvu.web.php.internal.value.PhpInt;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The arithmetic a layout needs: rounding a price, working out how many pages a list fills.
 *
 * <p>{@code floor} and {@code ceil} answer with floats, as PHP's do — which is why the paging idiom is written
 * {@code (int) ceil($total / $perPage)} and not without the cast.
 *
 * @author Deniss Larka
 */
final class MathFunctions {

    private MathFunctions() {}

    static void registerInto(FunctionRegistry registry) {
        Functions.define(
                registry,
                "abs",
                1,
                (env, a) -> a.at(0) instanceof PhpInt whole
                        ? PhpInt.of(Math.abs(whole.value()))
                        : PhpFloat.of(Math.abs(a.number(0))));
        Functions.define(registry, "floor", 1, (env, a) -> PhpFloat.of(Math.floor(a.number(0))));
        Functions.define(registry, "ceil", 1, (env, a) -> PhpFloat.of(Math.ceil(a.number(0))));
        Functions.define(
                registry,
                "round",
                1,
                3,
                (env, a) -> PhpFloat.of(round(a.number(0), (int) a.integerOr(1, 0), (int) a.integerOr(2, 1))));
        Functions.define(registry, "sqrt", 1, (env, a) -> PhpFloat.of(Math.sqrt(a.number(0))));
        Functions.define(registry, "pow", 2, (env, a) -> PhpArithmetic.power(a.at(0), a.at(1)));
        Functions.define(registry, "fmod", 2, (env, a) -> PhpFloat.of(a.number(0) % a.number(1)));
        Functions.define(registry, "intdiv", 2, (env, a) -> {
            if (a.integer(1) == 0) {
                throw new PhpProcessingException("intdiv(): Division by zero");
            }
            return PhpInt.of(a.integer(0) / a.integer(1));
        });
    }

    /**
     * PHP's four rounding modes, numbered 1 to 4 as its {@code PHP_ROUND_HALF_*} constants are.
     *
     * <p>{@link BigDecimal#valueOf(double)} rather than the constructor on purpose: it takes the number as it was
     * written rather than the binary approximation behind it, which is why {@code round(1.955, 2)} answers 1.96 here as
     * it does in PHP instead of 1.95.
     */
    private static double round(double value, int precision, int mode) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return value;
        }
        RoundingMode rounding =
                switch (mode) {
                    case 2 -> RoundingMode.HALF_DOWN;
                    case 3 -> RoundingMode.HALF_EVEN;
                    case 4 -> RoundingMode.UP;
                    default -> RoundingMode.HALF_UP;
                };
        return BigDecimal.valueOf(value).setScale(precision, rounding).doubleValue();
    }
}
