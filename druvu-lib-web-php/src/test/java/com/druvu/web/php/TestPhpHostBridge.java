package com.druvu.web.php;

import com.druvu.web.php.internal.PhpEngineConfig;
import com.druvu.web.php.internal.builtin.Builtins;
import com.druvu.web.php.internal.parse.PhpParser;
import com.druvu.web.php.internal.runtime.Env;
import java.util.List;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * What the application hands the template, and what the template can do with it.
 *
 * <p>The interesting half is what it cannot do: reach through an object, change one, or call anything on it.
 */
public class TestPhpHostBridge {

    /** Stands in for whatever a handler would pass. Public, because reflection across a module needs it to be. */
    public record Order(String reference, int quantity, Customer customer, List<String> lines) {

        public Order {
            lines = List.copyOf(lines);
        }
    }

    public record Customer(String name, boolean preferred) {}

    /** The other shape a host might pass: an ordinary bean. */
    public static final class Legacy {

        public String getTitle() {
            return "Old";
        }

        public boolean isVisible() {
            return true;
        }
    }

    @Test
    public void testTheModelArrivesAsOrdinaryVariables() {
        Assert.assertEquals(
                render("<?php echo $title, \" \", $count;", Map.of("title", "Home", "count", 3)),
                "Home 3",
                "a handler passing 'title' makes $title, with nothing to learn");
    }

    @Test
    public void testAMapBecomesAnArrayAndAListBecomesAList() {
        Assert.assertEquals(
                render(
                        "<?php echo $user[\"name\"], \"|\", implode(\",\", $tags), \"|\", count($tags);",
                        Map.of("user", Map.of("name", "Ada"), "tags", List.of("a", "b"))),
                "Ada|a,b|2");
    }

    @Test
    public void testARecordIsReadThroughItsComponents() {
        Assert.assertEquals(
                render("<?php echo $order->reference, \" x\", $order->quantity;", Map.of("order", sample())), "A-1 x2");
    }

    @Test
    public void testNestedRecordsAndListsKeepWorking() {
        Assert.assertEquals(
                render(
                        "<?php echo $order->customer->name, \"|\", implode(\",\", $order->lines);",
                        Map.of("order", sample())),
                "Ada|pen,cup");
    }

    @Test
    public void testABeanIsReadThroughItsGetters() {
        Assert.assertEquals(
                render("<?php echo $it->title, ($it->visible ? \"+\" : \"-\");", Map.of("it", new Legacy())), "Old+");
    }

    @Test
    public void testTheNullSafeArrowStopsAtNull() {
        Assert.assertEquals(render("<?php echo $missing?->name ?? \"none\";", Map.of()), "none");
    }

    @Test
    public void testAnUnknownPropertyIsReportedAndReadsAsNull() {
        Env env = newEnv(Map.of("order", sample()));
        PhpParser.parse("<?php echo $order->nosuch ?? \"none\";", "/t.php", PhpEngineConfig.DEFAULTS)
                .render(env);
        Assert.assertEquals(env.output(), "none");
    }

    @Test
    public void testAHostObjectIsReadOnly() {
        Assert.assertThrows(
                RuntimeException.class, () -> render("<?php $order->reference = \"B-2\";", Map.of("order", sample())));
    }

    @Test
    public void testAnObjectCannotBePrintedByAccident() {
        Assert.assertThrows(RuntimeException.class, () -> render("<?php echo $order;", Map.of("order", sample())));
    }

    @Test
    public void testHostTextIsStillEscapedOnTheWayOut() {
        Assert.assertEquals(
                render("<?php echo $name;", Map.of("name", "<script>")),
                "&lt;script&gt;",
                "data from the application is data, not markup");
    }

    @Test
    public void testALayoutDrivenByTheModel() {
        String template = """
                <ul>
                <?php foreach ($order->lines as $line): ?>
                  <li><?= $line ?></li>
                <?php endforeach; ?>
                </ul>
                """;
        Assert.assertEquals(
                render(template, Map.of("order", sample())), "<ul>\n  <li>pen</li>\n  <li>cup</li>\n</ul>\n");
    }

    private static Order sample() {
        return new Order("A-1", 2, new Customer("Ada", true), List.of("pen", "cup"));
    }

    private static String render(String template, Map<String, ?> model) {
        Env env = newEnv(model);
        PhpParser.parse(template, "/test.php", PhpEngineConfig.DEFAULTS).render(env);
        return env.output();
    }

    private static Env newEnv(Map<String, ?> model) {
        Env env = new Env(PhpEngineConfig.DEFAULTS, null, Builtins.registry());
        env.bind(model);
        return env;
    }
}
