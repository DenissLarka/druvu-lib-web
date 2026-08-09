package com.druvu.web.php.internal.ast.stmt;

import com.druvu.web.php.internal.Location;
import com.druvu.web.php.internal.ast.PhpExpression;
import com.druvu.web.php.internal.ast.PhpStatement;
import com.druvu.web.php.internal.ast.Signal;
import com.druvu.web.php.internal.runtime.Env;
import com.druvu.web.php.internal.value.PhpComparison;
import com.druvu.web.php.internal.value.PhpValue;
import java.util.List;

/**
 * {@code switch}, with PHP's loose comparison and its fall-through.
 *
 * <p>Matching picks a starting point rather than a branch: once a case matches, every statement from there to the end
 * of the switch runs until something breaks out. {@code default} is only considered after every case has failed, no
 * matter where it was written.
 *
 * <p>A bare {@code continue} inside a switch acts as {@code break}, as it does in PHP — the switch counts as a level.
 */
public final class SwitchStatement extends PhpStatement {

    /** One case. A null test is the {@code default}. */
    public record Branch(PhpExpression test, List<PhpStatement> body) {

        public Branch {
            body = List.copyOf(body);
        }
    }

    private final PhpExpression subject;
    private final List<Branch> branches;

    public SwitchStatement(Location location, PhpExpression subject, List<Branch> branches) {
        super(location);
        this.subject = subject;
        this.branches = List.copyOf(branches);
    }

    @Override
    public Signal execute(Env env) {
        int start = firstMatch(env, subject.eval(env));
        if (start < 0) {
            return null;
        }
        for (int i = start; i < branches.size(); i++) {
            for (PhpStatement statement : branches.get(i).body()) {
                Signal signal = statement.execute(env);
                if (signal instanceof Signal.Break stop) {
                    return stop.outer();
                }
                if (signal instanceof Signal.Continue skip) {
                    return skip.outer();
                }
                if (signal != null) {
                    return signal;
                }
            }
        }
        return null;
    }

    /** The branch to start at: the first matching case, or the default, or nowhere. */
    private int firstMatch(Env env, PhpValue tested) {
        for (int i = 0; i < branches.size(); i++) {
            PhpExpression test = branches.get(i).test();
            if (test != null && PhpComparison.looseEquals(tested, test.eval(env))) {
                return i;
            }
        }
        for (int i = 0; i < branches.size(); i++) {
            if (branches.get(i).test() == null) {
                return i;
            }
        }
        return -1;
    }
}
