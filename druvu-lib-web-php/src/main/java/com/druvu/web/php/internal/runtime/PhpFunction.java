package com.druvu.web.php.internal.runtime;

import com.druvu.web.php.internal.value.PhpValue;
import java.util.List;

/**
 * A function callable from a template. Arguments arrive already evaluated and in order; there are no named arguments
 * and no references.
 *
 * @author Deniss Larka
 */
@FunctionalInterface
public interface PhpFunction {

    PhpValue call(Env env, List<PhpValue> arguments);
}
