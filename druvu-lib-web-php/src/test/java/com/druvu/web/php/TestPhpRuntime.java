package com.druvu.web.php;

import com.druvu.web.php.internal.PhpEngineConfig;
import com.druvu.web.php.internal.PhpProcessingException;
import com.druvu.web.php.internal.runtime.Env;
import com.druvu.web.php.internal.runtime.Scope;
import com.druvu.web.php.internal.value.PhpInt;
import com.druvu.web.php.internal.value.PhpNull;
import com.druvu.web.php.internal.value.PhpString;
import org.testng.Assert;
import org.testng.annotations.Test;

/** The state one render carries: its variables, its output, and its include bookkeeping. */
public class TestPhpRuntime {

    @Test
    public void testScopeDistinguishesUnsetFromNull() {
        Scope scope = new Scope();
        Assert.assertFalse(scope.isDefined("x"));
        Assert.assertNull(scope.get("x"));

        scope.set("x", PhpNull.NULL);
        Assert.assertTrue(scope.isDefined("x"), "a variable holding null is still set");
        Assert.assertSame(scope.get("x"), PhpNull.NULL);

        scope.unset("x");
        Assert.assertFalse(scope.isDefined("x"));
    }

    @Test
    public void testPushedScopeIsFreshAndTheOldOneSurvives() {
        Env env = newEnv();
        env.setVariable("outer", PhpInt.of(1L));

        Scope previous = env.pushScope();
        Assert.assertFalse(env.isDefined("outer"), "a pushed scope starts empty");
        env.setVariable("inner", PhpInt.of(2L));

        env.popScope(previous);
        Assert.assertTrue(env.isDefined("outer"));
        Assert.assertFalse(env.isDefined("inner"));
    }

    @Test
    public void testOutputAccumulatesInOrder() {
        Env env = newEnv();
        env.write("<p>");
        env.write("hello");
        env.write("</p>");
        Assert.assertEquals(env.output(), "<p>hello</p>");
    }

    @Test
    public void testIncludeDepthIsLimited() {
        Env env = new Env(new PhpEngineConfig(true, false, 2, 1000L, false), null);
        env.enterInclude();
        env.enterInclude();
        Assert.assertEquals(env.includeDepth(), 2);
        Assert.assertThrows(PhpProcessingException.class, env::enterInclude);
    }

    @Test
    public void testLeavingAnIncludeFreesTheDepthAgain() {
        Env env = new Env(new PhpEngineConfig(true, false, 1, 1000L, false), null);
        env.enterInclude();
        env.leaveInclude();
        env.enterInclude();
        Assert.assertEquals(env.includeDepth(), 1);
    }

    @Test
    public void testOnceBookkeepingOnlyAnswersForOnceIncludes() {
        Env env = newEnv();
        Assert.assertFalse(env.wasIncludedOnce("/layout.php"));
        Assert.assertTrue(env.markIncludedOnce("/layout.php"), "the first _once include runs");
        Assert.assertFalse(env.markIncludedOnce("/layout.php"), "the second is skipped");
        Assert.assertTrue(env.wasIncludedOnce("/layout.php"));
    }

    @Test
    public void testConfigDefaultsAreTheDocumentedPolicy() {
        PhpEngineConfig config = PhpEngineConfig.DEFAULTS;
        Assert.assertTrue(config.escapeOutput(), "output is escaped by default");
        Assert.assertFalse(config.shortOpenTag());
        Assert.assertFalse(config.debugFunctions());
        Assert.assertEquals(config.maxIncludeDepth(), PhpEngineConfig.DEFAULT_MAX_INCLUDE_DEPTH);
        Assert.assertEquals(config.maxLoopIterations(), PhpEngineConfig.DEFAULT_MAX_LOOP_ITERATIONS);
        Assert.assertFalse(config.withoutEscaping().escapeOutput());
        Assert.assertTrue(config.withDebugFunctions().debugFunctions());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testConfigRejectsAnImpossibleIncludeDepth() {
        new PhpEngineConfig(true, false, 0, 1000L, false);
    }

    @Test
    public void testEnvHasNoRequestOutsideAContainer() {
        Env env = newEnv();
        Assert.assertNull(env.request());
        env.setVariable("title", PhpString.of("druvu"));
        Assert.assertEquals(env.getVariable("title").toStr(), "druvu");
        env.unsetVariable("title");
        Assert.assertNull(env.getVariable("title"));
    }

    private static Env newEnv() {
        return new Env(PhpEngineConfig.DEFAULTS, null);
    }
}
