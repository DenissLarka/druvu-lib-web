package com.druvu.web.php;

import com.druvu.web.php.internal.PhpEngineConfig;
import com.druvu.web.php.internal.PhpSyntaxException;
import com.druvu.web.php.internal.builtin.Builtins;
import com.druvu.web.php.internal.parse.PhpParser;
import com.druvu.web.php.internal.runtime.Env;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Interpolation and heredocs, against what PHP 8.5.9 printed for the same source.
 *
 * <p>The engine lexes the pieces of an interpolated string in place and hands the parser a concatenation, so these also
 * check the thing that design buys: that {@code "$a"} is a string even when {@code $a} is not.
 */
public class TestPhpInterpolation {

    private static final String SETUP =
            "<?php $name = \"World\"; $arr = [\"key\" => \"K\", 0 => \"Z\", \"a\" => [\"b\" => \"AB\"]]; $i = 0; $n = 5; ";

    @DataProvider(name = "interpolations")
    public Object[][] interpolations() {
        return new Object[][] {
            {"\"$name\"", "World"},
            {"\"$name!\"", "World!"},
            {"\"a$name b\"", "aWorld b"},
            {"\"$arr[key]\"", "K"},
            {"\"$arr[0]\"", "Z"},
            {"\"$arr[$i]\"", "Z"},
            {"\"{$arr['key']}\"", "K"},
            {"\"{$arr['a']['b']}\"", "AB"},
            {"\"{$name}s\"", "Worlds"},

            // a dollar that begins nothing, and one that was escaped, are both just text
            {"\"\\$name\"", "$name"},
            {"\"$ x\"", "$ x"},
            {"\"cost: $5\"", "cost: $5"},
            {"\"{}\"", "{}"},
            {"\"a{b}c\"", "a{b}c"},

            // single quotes never interpolate
            {"'$name'", "$name"},

            // whole expressions are allowed inside the braces
            {"\"{$n} + {$n} = {$n} + {$n}\"", "5 + 5 = 5 + 5"},
            {"\"total: {$arr['key']}{$arr[0]}\"", "total: KZ"},
        };
    }

    @Test(dataProvider = "interpolations")
    public void testInterpolation(String expression, String expected) {
        Assert.assertEquals(render(SETUP + "echo " + expression + ";"), expected, expression);
    }

    @Test
    public void testAnInterpolatedNumberIsStillAString() {
        Assert.assertEquals(render("<?php $n = 5; echo \"$n\" === \"5\" ? \"string\" : \"not\";"), "string");
        Assert.assertEquals(render("<?php $n = 5; echo \"{$n}\" === \"5\" ? \"string\" : \"not\";"), "string");
    }

    @Test
    public void testHeredocInterpolatesAndShedsTheClosingIndentation() {
        // raw() because the body is markup and output is escaped by default; the point here is the indentation.
        String rendered = render("""
                <?php
                $name = "World";
                echo raw(<<<HTML
                    <p>$name</p>
                      indented
                    HTML);
                """);
        Assert.assertEquals(rendered, "<p>World</p>\n  indented");
    }

    @Test
    public void testNowdocKeepsEverythingLiteral() {
        String rendered = render("""
                <?php
                $name = "World";
                echo <<<'RAW'
                  $name stays
                  RAW;
                """);
        Assert.assertEquals(rendered, "$name stays");
    }

    @Test
    public void testHeredocWithNoIndentationKeepsItsLines() {
        String rendered = render("<?php echo <<<TXT\nline1\nline2\nTXT;\n");
        Assert.assertEquals(rendered, "line1\nline2");
    }

    @Test
    public void testHeredocCarriesEscapesAndNowdocDoesNot() {
        Assert.assertEquals(render("<?php echo <<<T\na\\tb\nT;\n"), "a\tb");
        Assert.assertEquals(render("<?php echo <<<'T'\na\\tb\nT;\n"), "a\\tb");
    }

    @Test
    public void testALabelInsideTheBodyDoesNotCloseTheHeredoc() {
        Assert.assertEquals(render("<?php echo <<<T\nTITLE\nT;\n"), "TITLE");
    }

    @DataProvider(name = "refused")
    public Object[][] refused() {
        return new Object[][] {
            {"<?php echo \"{$name\";", "Unterminated"},
            {"<?php echo <<<T\nnever ends\n", "Unterminated heredoc"},
            {"<?php echo <<<T text on the same line\nT;\n", "must begin on the line after"},
            {"<?php echo \"$arr[key\";", "Expected ']'"},
        };
    }

    @Test(dataProvider = "refused")
    public void testMalformedInterpolationIsRefused(String template, String expectedInMessage) {
        PhpSyntaxException failure = Assert.expectThrows(PhpSyntaxException.class, () -> render(template));
        Assert.assertTrue(
                failure.getMessage().contains(expectedInMessage),
                "expected '" + expectedInMessage + "' in: " + failure.getMessage());
    }

    @Test
    public void testInterpolationInsideALayout() {
        String rendered = render("""
                <?php $items = ["pen" => 2, "cup" => 5]; ?>
                <ul>
                <?php foreach ($items as $what => $count): ?>
                  <li>$count x $what</li>
                <?php endforeach; ?>
                </ul>
                """);
        Assert.assertEquals(rendered, "<ul>\n  <li>$count x $what</li>\n  <li>$count x $what</li>\n</ul>\n");
    }

    @Test
    public void testInterpolationOnlyHappensInsideStrings() {
        Assert.assertEquals(
                render("<?php $a = 1; ?>literal $a here"), "literal $a here", "markup is never interpolated");
    }

    private static String render(String template) {
        Env env = new Env(PhpEngineConfig.DEFAULTS, null, Builtins.registry());
        PhpParser.parse(template, "/test.php", PhpEngineConfig.DEFAULTS).render(env);
        return env.output();
    }
}
