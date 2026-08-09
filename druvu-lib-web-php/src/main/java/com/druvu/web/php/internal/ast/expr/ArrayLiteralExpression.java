package com.druvu.web.php.internal.ast.expr;

import com.druvu.web.php.internal.Location;
import com.druvu.web.php.internal.ast.PhpExpression;
import com.druvu.web.php.internal.runtime.Env;
import com.druvu.web.php.internal.value.PhpArray;
import com.druvu.web.php.internal.value.PhpValue;
import java.util.List;

/** {@code [1, 2]} and {@code ['a' => 1]}, in both the bracket and the {@code array(...)} spelling. */
public final class ArrayLiteralExpression extends PhpExpression {

    /** One entry. A null key means the value takes the array's next free index. */
    public record Entry(PhpExpression key, PhpExpression value) {}

    private final List<Entry> entries;

    public ArrayLiteralExpression(Location location, List<Entry> entries) {
        super(location);
        this.entries = List.copyOf(entries);
    }

    @Override
    public PhpValue eval(Env env) {
        PhpArray array = PhpArray.empty();
        for (Entry entry : entries) {
            PhpValue value = entry.value().eval(env).copyForAssignment();
            if (entry.key() == null) {
                array.append(value);
            } else {
                array.put(entry.key().eval(env).toKey(), value);
            }
        }
        return array;
    }
}
