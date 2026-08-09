package com.druvu.web.php.internal.ast;

import com.druvu.web.php.internal.value.PhpValue;
import java.util.Objects;

/**
 * What a statement reports back when it does not simply fall through to the next one.
 *
 * <p>A null signal means "carry on". Break and continue are sentinels rather than exceptions, because {@code break 2;}
 * has to be answered one loop at a time: each enclosing loop absorbs a level and passes the rest outward. An exception
 * carrying the same count would work but would put control flow that happens on every other template render onto the
 * exception path.
 *
 * @author Deniss Larka
 */
public sealed interface Signal {

    /** {@code break $levels;} */
    record Break(int levels) implements Signal {

        public Break {
            if (levels < 1) {
                throw new IllegalArgumentException("break level must be at least 1, got " + levels);
            }
        }

        /** What propagates once the innermost loop has absorbed a level, or null when the break stops there. */
        public Signal outer() {
            return levels == 1 ? null : new Break(levels - 1);
        }
    }

    /** {@code continue $levels;} */
    record Continue(int levels) implements Signal {

        public Continue {
            if (levels < 1) {
                throw new IllegalArgumentException("continue level must be at least 1, got " + levels);
            }
        }

        /** What propagates once the innermost loop has absorbed a level, or null when the continue stops there. */
        public Signal outer() {
            return levels == 1 ? null : new Continue(levels - 1);
        }
    }

    /**
     * {@code return $value;} at the top level of a template, which ends it and gives the includer's {@code include}
     * expression its value.
     */
    record Return(PhpValue value) implements Signal {

        public Return {
            Objects.requireNonNull(value, "value");
        }
    }
}
