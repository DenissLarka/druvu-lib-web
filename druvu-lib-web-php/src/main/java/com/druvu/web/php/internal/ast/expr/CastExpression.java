package com.druvu.web.php.internal.ast.expr;

import com.druvu.web.php.internal.Location;
import com.druvu.web.php.internal.ast.PhpExpression;
import com.druvu.web.php.internal.runtime.Env;
import com.druvu.web.php.internal.value.PhpArray;
import com.druvu.web.php.internal.value.PhpFloat;
import com.druvu.web.php.internal.value.PhpInt;
import com.druvu.web.php.internal.value.PhpNull;
import com.druvu.web.php.internal.value.PhpString;
import com.druvu.web.php.internal.value.PhpValue;

/** {@code (int)}, {@code (float)}, {@code (string)}, {@code (bool)} and {@code (array)}. */
public final class CastExpression extends PhpExpression {

    /** The five casts, with every spelling PHP accepts for each. */
    public enum Target {
        INT("int", "integer"),
        FLOAT("float", "double"),
        STRING("string"),
        BOOL("bool", "boolean"),
        ARRAY("array");

        private final String[] spellings;

        Target(String... spellings) {
            this.spellings = spellings;
        }

        /** The cast this word names, or null when it names none. */
        public static Target named(String word) {
            for (Target target : values()) {
                for (String spelling : target.spellings) {
                    if (spelling.equalsIgnoreCase(word)) {
                        return target;
                    }
                }
            }
            return null;
        }
    }

    private final Target target;
    private final PhpExpression operand;

    public CastExpression(Location location, Target target, PhpExpression operand) {
        super(location);
        this.target = target;
        this.operand = operand;
    }

    @Override
    public PhpValue eval(Env env) {
        PhpValue value = operand.eval(env);
        return switch (target) {
            case INT -> PhpInt.of(value.toInt());
            case FLOAT -> PhpFloat.of(value.toFloat());
            case STRING -> PhpString.of(value.toStr());
            case BOOL -> value.toBool();
            case ARRAY -> toArray(value);
        };
    }

    /** PHP wraps a scalar in a one-element array and turns null into an empty one. */
    private static PhpValue toArray(PhpValue value) {
        if (value instanceof PhpArray) {
            return value;
        }
        return value instanceof PhpNull ? PhpArray.empty() : PhpArray.ofValues(value);
    }
}
