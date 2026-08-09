package com.druvu.web.php;

import com.druvu.web.php.internal.Location;
import com.druvu.web.php.internal.PhpEngineConfig;
import com.druvu.web.php.internal.ast.PhpStatement;
import com.druvu.web.php.internal.ast.Signal;
import com.druvu.web.php.internal.ast.stmt.TextStatement;
import com.druvu.web.php.internal.runtime.Env;
import com.druvu.web.php.internal.value.PhpInt;
import org.testng.Assert;
import org.testng.annotations.Test;

/** How {@code break 2;} unwinds one loop at a time, and that a statement can write and report at all. */
public class TestSignal {

    @Test
    public void testASingleLevelBreakStopsAtTheInnermostLoop() {
        Assert.assertNull(new Signal.Break(1).outer());
        Assert.assertNull(new Signal.Continue(1).outer());
    }

    @Test
    public void testAMultiLevelBreakLosesOneLevelPerLoop() {
        Signal.Break three = new Signal.Break(3);
        Assert.assertEquals(three.outer(), new Signal.Break(2));
        Assert.assertEquals(((Signal.Break) three.outer()).outer(), new Signal.Break(1));
        Assert.assertNull(((Signal.Break) ((Signal.Break) three.outer()).outer()).outer());
    }

    @Test
    public void testContinueUnwindsTheSameWay() {
        Assert.assertEquals(new Signal.Continue(2).outer(), new Signal.Continue(1));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testBreakLevelsStartAtOne() {
        new Signal.Break(0);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testContinueLevelsStartAtOne() {
        new Signal.Continue(-1);
    }

    @Test
    public void testReturnCarriesItsValue() {
        Assert.assertEquals(new Signal.Return(PhpInt.of(7L)).value(), PhpInt.of(7L));
    }

    @Test
    public void testAStatementWritesAndFallsThrough() {
        Env env = new Env(PhpEngineConfig.DEFAULTS, null);
        PhpStatement statement = new TextStatement(Location.UNKNOWN, "<h1>");

        Assert.assertNull(statement.execute(env), "null means carry on with the next statement");
        Assert.assertEquals(env.output(), "<h1>");
        Assert.assertEquals(statement.location(), Location.UNKNOWN);
    }

    @Test
    public void testLiteralTextIsNeverEscaped() {
        Env env = new Env(PhpEngineConfig.DEFAULTS, null);
        new TextStatement(Location.UNKNOWN, "<p class=\"x\">a & b</p>").execute(env);
        Assert.assertEquals(env.output(), "<p class=\"x\">a & b</p>", "the page's own markup is not a value");
    }
}
