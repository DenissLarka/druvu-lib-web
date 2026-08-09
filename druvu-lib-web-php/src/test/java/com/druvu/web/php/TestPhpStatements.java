package com.druvu.web.php;

import com.druvu.web.php.internal.PhpEngineConfig;
import com.druvu.web.php.internal.PhpProcessingException;
import com.druvu.web.php.internal.PhpSyntaxException;
import com.druvu.web.php.internal.builtin.Builtins;
import com.druvu.web.php.internal.parse.PhpParser;
import com.druvu.web.php.internal.runtime.Env;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Whole templates in, rendered pages out.
 *
 * <p>Every expected string below is what PHP 8.5.9 printed for the same template, captured rather than imagined. The
 * echoed values are deliberately escape-neutral — letters and digits — so these stay valid once output escaping lands
 * and the engine's own behaviour departs from PHP's on purpose.
 */
public class TestPhpStatements {

    @DataProvider(name = "templates")
    public Object[][] templates() {
        return new Object[][] {
            {"if, in the colon form the layout is written in", """
                <ul>
                <?php $n = 2; ?>
                <?php if ($n > 5): ?>
                  <li>big</li>
                <?php elseif ($n > 1): ?>
                  <li>medium</li>
                <?php else: ?>
                  <li>small</li>
                <?php endif; ?>
                </ul>
                """, "<ul>\n  <li>medium</li>\n</ul>\n"},
            {"if, braced, with else if in two words and a bare body", """
                <?php
                $n = 0;
                if ($n) { echo "yes"; } else if ($n === 0) { echo "zero"; } else { echo "no"; }
                echo "|";
                if (1) echo "bare";
                """, "zero|bare"},
            {
                "foreach over keys and values, and over a bare list",
                """
                <?php $rows = ["a" => 1, "b" => 2, 7 => 3]; ?>
                <table>
                <?php foreach ($rows as $key => $value): ?>
                  <tr><td><?= $key ?></td><td><?= $value ?></td></tr>
                <?php endforeach; ?>
                </table>
                <?php foreach ([10, 20] as $v) { echo $v, ";"; } ?>
                """,
                "<table>\n  <tr><td>a</td><td>1</td></tr>\n  <tr><td>b</td><td>2</td></tr>\n"
                        + "  <tr><td>7</td><td>3</td></tr>\n</table>\n10;20;"
            },
            {"for with comma lists, while, do-while, and the loop jumps", """
                <?php
                for ($i = 0, $j = 10; $i < 3; $i++, $j--) { echo $i, "-", $j, ";"; }
                echo "|";
                $k = 0;
                while ($k < 3): echo $k; $k++; endwhile;
                echo "|";
                $m = 5;
                do { echo $m; $m++; } while ($m < 3);
                echo "|";
                for ($i = 0; $i < 5; $i++) { if ($i == 1) continue; if ($i == 3) break; echo $i; }
                """, "0-10;1-9;2-8;|012|5|02"},
            {"switch falls through until something breaks", """
                <?php
                foreach ([1, 2, 3, 9] as $v) {
                  switch ($v) {
                    case 1:
                      echo "one";
                    case 2:
                      echo "two";
                      break;
                    case 3:
                      echo "three";
                      break;
                    default:
                      echo "other";
                  }
                  echo "|";
                }
                """, "onetwo|two|three|other|"},
            {"break and continue unwind the number of loops they are given", """
                <?php
                for ($i = 0; $i < 3; $i++) {
                  for ($j = 0; $j < 3; $j++) {
                    if ($j == 1) continue 2;
                    if ($i == 2) break 2;
                    echo $i, $j, ";";
                  }
                }
                echo "|";
                foreach ([1,2] as $a) { foreach ([3,4] as $b) { echo $a, $b; break 2; } }
                """, "00;10;|13"},
            {"echo takes a list, print is an expression, unset removes both kinds of thing", """
                <?php
                echo "a", "b", 1 + 2;
                echo "|";
                print "p";
                echo "|";
                $x = 1; $arr = ["k" => 1, "j" => 2];
                unset($x, $arr["k"]);
                echo isset($x) ? "set" : "unset";
                echo isset($arr["k"]) ? "still" : "gone";
                echo $arr["j"];
                """, "ab3|p|unsetgone2"},
            {"switch in the colon form", """
                <?php $v = 2; ?>
                <?php switch ($v): ?>
                <?php case 1: ?>one
                <?php break; ?>
                <?php case 2: ?>two
                <?php break; ?>
                <?php default: ?>other
                <?php endswitch; ?>
                done
                """, "two\ndone\n"},
            {"match and an arrow function carrying it, and an array grown out of nothing", """
                <?php
                $grade = fn($n) => match (true) { $n >= 90 => "A", $n >= 80 => "B", default => "C" };
                foreach ([95, 85, 20] as $score) { echo $grade($score); }
                echo "|";
                $rows = [];
                $rows[] = "x";
                $rows["deep"]["er"] = "y";
                echo $rows[0], $rows["deep"]["er"];
                """, "ABC|xy"},
        };
    }

    @Test(dataProvider = "templates")
    public void testTemplateRendersAsPhpDoes(String what, String template, String expected) {
        Assert.assertEquals(render(template), expected, what);
    }

    // --------------------------------------------------------- other behaviour

    @Test
    public void testMarkupInsideALoopBodyIsRepeated() {
        String rendered = render("<?php foreach ([1, 2] as $n): ?><b><?= $n ?></b><?php endforeach; ?>");
        Assert.assertEquals(rendered, "<b>1</b><b>2</b>");
    }

    @Test
    public void testAClosingTagSuppliesTheMissingSemicolon() {
        Assert.assertEquals(render("<?php echo \"x\" ?>tail"), "xtail");
    }

    @Test
    public void testReturnEndsTheTemplateAndCarriesAValue() {
        Env env = new Env(PhpEngineConfig.DEFAULTS, null, Builtins.registry());
        var template = PhpParser.parse("<?php echo \"a\"; return 7; ?>never", "/t.php", PhpEngineConfig.DEFAULTS);
        Assert.assertEquals(template.render(env).toStr(), "7");
        Assert.assertEquals(env.output(), "a");
    }

    @Test
    public void testATemplateWithNoReturnYieldsOne() {
        Env env = new Env(PhpEngineConfig.DEFAULTS, null, Builtins.registry());
        Assert.assertEquals(
                PhpParser.parse("plain", "/t.php", PhpEngineConfig.DEFAULTS)
                        .render(env)
                        .toStr(),
                "1");
    }

    @Test
    public void testForeachOverSomethingThatIsNotAnArrayIsReportedAndSkipped() {
        Env env = new Env(PhpEngineConfig.DEFAULTS, null, Builtins.registry());
        PhpParser.parse("<?php foreach (5 as $v) { echo $v; }", "/t.php", PhpEngineConfig.DEFAULTS)
                .render(env);
        Assert.assertEquals(env.output(), "");
        Assert.assertEquals(env.diagnostics().size(), 1);
        Assert.assertTrue(env.diagnostics().get(0).message().contains("foreach() needs an array"));
    }

    @Test
    public void testARunawayLoopIsStopped() {
        PhpEngineConfig shortLeash = new PhpEngineConfig(true, false, 10, 50L, false);
        Env env = new Env(shortLeash, null, Builtins.registry());
        PhpProcessingException failure = Assert.expectThrows(
                PhpProcessingException.class,
                () -> PhpParser.parse("<?php while (true) { echo \"x\"; }", "/t.php", shortLeash)
                        .render(env));
        Assert.assertTrue(failure.getMessage().contains("ran more than 50 times"), failure.getMessage());
    }

    @Test
    public void testBreakOutsideALoopIsRefused() {
        Env env = new Env(PhpEngineConfig.DEFAULTS, null, Builtins.registry());
        PhpProcessingException failure = Assert.expectThrows(
                PhpProcessingException.class,
                () -> PhpParser.parse("<?php break;", "/t.php", PhpEngineConfig.DEFAULTS)
                        .render(env));
        Assert.assertTrue(failure.getMessage().contains("outside a loop"), failure.getMessage());
    }

    @DataProvider(name = "refusedTemplates")
    public Object[][] refusedTemplates() {
        return new Object[][] {
            {"<?php foreach ([1] as &$v) {}", "no references"},
            {"<?php if (1): echo \"a\";", "Expected 'endif'"},
            {"<?php $a = 1 $b = 2;", "Expected ';'"},
            {"<?php while (1) { echo 1;", "Expected '}'"},
            {"<?php switch (1) { echo 1; }", "'}' to close the switch"},
        };
    }

    @Test(dataProvider = "refusedTemplates")
    public void testMalformedTemplatesAreRefused(String template, String expectedInMessage) {
        PhpSyntaxException failure = Assert.expectThrows(PhpSyntaxException.class, () -> render(template));
        Assert.assertTrue(
                failure.getMessage().contains(expectedInMessage),
                "expected '" + expectedInMessage + "' in: " + failure.getMessage());
    }

    @Test
    public void testAnOmittedFinalSemicolonAtTheEndOfTheFileIsFine() {
        Assert.assertEquals(render("<?php echo \"x\""), "x");
    }

    // ---------------------------------------------------------------- helpers

    private static String render(String template) {
        Env env = new Env(PhpEngineConfig.DEFAULTS, null, Builtins.registry());
        PhpParser.parse(template, "/test.php", PhpEngineConfig.DEFAULTS).render(env);
        return env.output();
    }
}
