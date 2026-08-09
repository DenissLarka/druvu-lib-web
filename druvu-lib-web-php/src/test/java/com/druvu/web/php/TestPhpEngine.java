package com.druvu.web.php;

import com.druvu.web.php.internal.PhpEngine;
import com.druvu.web.php.internal.PhpEngineConfig;
import com.druvu.web.php.internal.parse.CachingTemplateSource;
import com.druvu.web.php.internal.parse.ParsingTemplateSource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * The engine as an application uses it: one instance, many renders.
 *
 * <p>The thread-safety test is the one that matters. Everything the engine holds is finished being written before the
 * first request, and everything a render changes lives in its own environment — so this asserts what that buys, which
 * is that concurrent renders cannot see each other.
 */
public class TestPhpEngine {

    @Test
    public void testARenderProducesThePage() {
        PhpEngine engine = engineFor(Map.of("/page.php", "<h1><?= $title ?></h1>"));
        Assert.assertEquals(engine.render("/page.php", null, Map.of("title", "Home")), "<h1>Home</h1>");
    }

    @Test
    public void testAMissingTemplateIsNoPageRatherThanAnError() {
        Assert.assertNull(engineFor(Map.of()).render("/gone.php", null, Map.of()));
    }

    @Test
    public void testTwoRendersDoNotShareVariables() {
        PhpEngine engine = engineFor(Map.of("/page.php", "<?= $who ?? \"nobody\" ?>"));
        Assert.assertEquals(engine.render("/page.php", null, Map.of("who", "Ada")), "Ada");
        Assert.assertEquals(engine.render("/page.php", null, Map.of()), "nobody");
    }

    @Test
    public void testATemplateIsParsedOnceAndRenderedManyTimes() {
        Map<String, String> files = new HashMap<>(Map.of("/page.php", "<?= 1 + 1 ?>"));
        CachingTemplateSource cache =
                new CachingTemplateSource(new ParsingTemplateSource(files::get, PhpEngineConfig.DEFAULTS));

        Assert.assertNotNull(cache.find("/page.php"));
        Assert.assertSame(cache.find("/page.php"), cache.find("/page.php"), "the same tree comes back");
        Assert.assertEquals(cache.size(), 1);

        cache.clear();
        Assert.assertEquals(cache.size(), 0);
    }

    @Test
    public void testAMissingTemplateIsNotRemembered() {
        Map<String, String> files = new HashMap<>();
        CachingTemplateSource cache =
                new CachingTemplateSource(new ParsingTemplateSource(files::get, PhpEngineConfig.DEFAULTS));

        Assert.assertNull(cache.find("/later.php"));
        files.put("/later.php", "here");
        Assert.assertNotNull(cache.find("/later.php"), "adding the file should be enough to make it appear");
    }

    @Test
    public void testManyThreadsCanRenderTheSameTemplateAtOnce() throws InterruptedException, ExecutionException {
        PhpEngine engine = engineFor(Map.of("/page.php", """
                <?php $total = 0; ?>
                <?php foreach (range(1, 50) as $n): ?><?php $total = $total + $n; ?><?php endforeach; ?>
                <?= $who ?>:<?= $total ?>
                """));

        try (ExecutorService threads = Executors.newFixedThreadPool(8)) {
            var work = IntStream.range(0, 200)
                    .<Callable<String>>mapToObj(i -> () -> engine.render("/page.php", null, Map.of("who", "w" + i)))
                    .toList();
            var results = threads.invokeAll(work);
            for (int i = 0; i < results.size(); i++) {
                Future<String> result = results.get(i);
                Assert.assertEquals(result.get().strip(), "w" + i + ":1275", "render " + i + " saw another's state");
            }
        }
    }

    @Test
    public void testIncludesWorkThroughTheEngine() {
        PhpEngine engine = engineFor(Map.of(
                "/page.php", "<?php $title = \"Home\"; require \"parts/head.php\"; ?>body",
                "/parts/head.php", "<title><?= $title ?></title>"));
        Assert.assertEquals(engine.render("/page.php", null, Map.of()), "<title>Home</title>body");
    }

    @Test
    public void testTheEnginePutsTheHostModelAndTheEscapingTogether() {
        PhpEngine engine = engineFor(Map.of("/page.php", "<?= $note ?>|<?= raw($note) ?>"));
        Assert.assertEquals(engine.render("/page.php", null, Map.of("note", "<b>")), "&lt;b&gt;|<b>");
    }

    private static PhpEngine engineFor(Map<String, String> files) {
        Map<String, String> sources = new HashMap<>(files);
        return new PhpEngine(sources::get, PhpEngineConfig.DEFAULTS, true);
    }
}
