package com.druvu.web.php;

import com.druvu.web.php.internal.PhpEngineConfig;
import com.druvu.web.php.internal.PhpProcessingException;
import com.druvu.web.php.internal.builtin.Builtins;
import com.druvu.web.php.internal.parse.ParsingTemplateSource;
import com.druvu.web.php.internal.parse.PhpParser;
import com.druvu.web.php.internal.runtime.Env;
import com.druvu.web.php.internal.runtime.TemplatePaths;
import com.druvu.web.php.internal.runtime.TemplateSource;
import java.util.HashMap;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Composition: a page including a partial, which is the only way a layout is ever built.
 *
 * <p>Two of these cover bugs the survey found in the engine's previous version — a plain {@code include} poisoning a
 * later {@code include_once}, and a missing partial rendering as nothing at all — and one covers a hole that was never
 * closed: an include path walking out of the template root.
 */
public class TestPhpIncludes {

    @Test
    public void testAnIncludedTemplateSeesTheIncluderVariables() {
        Assert.assertEquals(
                render(Map.of(
                        "/page.php", "<?php $title = \"Home\"; include \"partial.php\";",
                        "/partial.php", "<h1><?= $title ?></h1>")),
                "<h1>Home</h1>",
                "sharing the scope is how a page hands data to a partial");
    }

    @Test
    public void testAnIncludedTemplateCanChangeTheIncluderVariables() {
        Assert.assertEquals(
                render(Map.of(
                        "/page.php", "<?php $n = 1; include \"bump.php\"; echo $n;",
                        "/bump.php", "<?php $n = $n + 1;")),
                "2");
    }

    @Test
    public void testARelativePathResolvesAgainstTheIncludingFile() {
        Assert.assertEquals(
                render(Map.of(
                        "/page.php", "<?php include \"parts/head.php\";",
                        "/parts/head.php", "<?php include \"foot.php\";",
                        "/parts/foot.php", "deep")),
                "deep");
    }

    @Test
    public void testIncludeOnceRunsOnlyTheFirstTime() {
        Assert.assertEquals(
                render(Map.of(
                        "/page.php", "<?php include_once \"p.php\"; include_once \"p.php\";",
                        "/p.php", "x")),
                "x");
    }

    /** The bug the naming of the bookkeeping exists to prevent: a plain include must not consume the "once". */
    @Test
    public void testAPlainIncludeDoesNotSpendTheOnce() {
        Assert.assertEquals(
                render(Map.of(
                        "/page.php", "<?php include \"p.php\"; include_once \"p.php\";",
                        "/p.php", "x")),
                "xx",
                "a plain include must not stop a later include_once from running");
    }

    @Test
    public void testAMissingIncludeIsReportedAndCarriesOn() {
        Map<String, String> files = Map.of("/page.php", "<?php include \"gone.php\"; echo \"after\";");
        Env env = newEnv(files);
        renderInto(env, files);
        Assert.assertEquals(env.output(), "after");
        Assert.assertEquals(env.diagnostics().size(), 1);
        Assert.assertTrue(env.diagnostics().get(0).message().contains("include(): failed to open 'gone.php'"));
    }

    @Test
    public void testAMissingRequireStopsTheRender() {
        PhpProcessingException failure = Assert.expectThrows(
                PhpProcessingException.class, () -> render(Map.of("/page.php", "<?php require \"gone.php\";")));
        Assert.assertTrue(failure.getMessage().contains("require(): failed to open"), failure.getMessage());
    }

    @Test
    public void testAnIncludeIsAnExpressionCarryingWhatTheTemplateReturned() {
        Assert.assertEquals(
                render(Map.of(
                        "/page.php", "<?php $config = include \"c.php\"; echo $config[\"who\"];",
                        "/c.php", "<?php return [\"who\" => \"druvu\"];")),
                "druvu");
    }

    @Test
    public void testAFailedIncludeEvaluatesToFalse() {
        Assert.assertEquals(
                render(Map.of("/page.php", "<?php echo (include \"gone.php\") === false ? \"false\" : \"other\";")),
                "false");
    }

    @Test
    public void testIncludeDepthIsLimited() {
        PhpEngineConfig shallow = new PhpEngineConfig(true, false, 3, 1000L, false);
        PhpProcessingException failure = Assert.expectThrows(
                PhpProcessingException.class,
                () -> render(Map.of("/page.php", "<?php include \"page.php\";"), shallow));
        Assert.assertTrue(failure.getMessage().contains("Maximum include depth"), failure.getMessage());
    }

    // ------------------------------------------------------------- containment

    @DataProvider(name = "paths")
    public Object[][] paths() {
        return new Object[][] {
            {"partial.php", "/page.php", "/partial.php"},
            {"parts/head.php", "/page.php", "/parts/head.php"},
            {"/abs.php", "/deep/page.php", "/abs.php"},
            {"../up.php", "/deep/page.php", "/up.php"},
            {"./same.php", "/deep/page.php", "/deep/same.php"},
            {"a/../b.php", "/page.php", "/b.php"},
        };
    }

    @Test(dataProvider = "paths")
    public void testPathResolution(String requested, String from, String expected) {
        Assert.assertEquals(TemplatePaths.resolve(requested, from), expected);
    }

    @DataProvider(name = "escapingPaths")
    public Object[][] escapingPaths() {
        return new Object[][] {{"../../etc/passwd", "/page.php"}, {"/../secret", "/page.php"}, {"..", "/page.php"}};
    }

    @Test(dataProvider = "escapingPaths")
    public void testAPathThatClimbsOutOfTheRootIsRefused(String requested, String from) {
        PhpProcessingException failure =
                Assert.expectThrows(PhpProcessingException.class, () -> TemplatePaths.resolve(requested, from));
        Assert.assertTrue(failure.getMessage().contains("climbs above the template root"), failure.getMessage());
    }

    // ---------------------------------------------------------------- helpers

    private static String render(Map<String, String> files) {
        return render(files, PhpEngineConfig.DEFAULTS);
    }

    private static String render(Map<String, String> files, PhpEngineConfig config) {
        Env env = newEnv(files, config);
        renderInto(env, files, config);
        return env.output();
    }

    private static Env newEnv(Map<String, String> files) {
        return newEnv(files, PhpEngineConfig.DEFAULTS);
    }

    private static Env newEnv(Map<String, String> files, PhpEngineConfig config) {
        Map<String, String> sources = new HashMap<>(files);
        TemplateSource templates = new ParsingTemplateSource(sources::get, config);
        return new Env(config, null, Builtins.registry(), templates, "/page.php");
    }

    private static void renderInto(Env env, Map<String, String> files) {
        renderInto(env, files, PhpEngineConfig.DEFAULTS);
    }

    private static void renderInto(Env env, Map<String, String> files, PhpEngineConfig config) {
        PhpParser.parse(files.get("/page.php"), "/page.php", config).render(env);
    }
}
