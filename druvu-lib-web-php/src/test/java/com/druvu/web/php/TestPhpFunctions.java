package com.druvu.web.php;

import com.druvu.web.php.internal.PhpEngineConfig;
import com.druvu.web.php.internal.PhpProcessingException;
import com.druvu.web.php.internal.builtin.Builtins;
import com.druvu.web.php.internal.parse.PhpParser;
import com.druvu.web.php.internal.runtime.Env;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Output escaping and the function library, against what PHP 8.5.9 printed.
 *
 * <p>The escaping tests are the ones worth reading twice: they are where the engine deliberately stops matching PHP.
 */
public class TestPhpFunctions {

    // ---------------------------------------------------------------- escaping

    @Test
    public void testEchoEscapesByDefault() {
        Assert.assertEquals(
                render("<?php $bio = \"<script>alert('x')</script>\"; echo $bio;"),
                "&lt;script&gt;alert(&#039;x&#039;)&lt;/script&gt;",
                "the whole point: forgetting to escape is safe here");
    }

    @Test
    public void testRawIsTheOptOut() {
        Assert.assertEquals(render("<?php echo raw(\"<b>bold</b>\");"), "<b>bold</b>");
    }

    @Test
    public void testTheShortEchoTagEscapesToo() {
        Assert.assertEquals(render("<?php $x = \"a&b\"; ?><?= $x ?>"), "a&amp;b");
    }

    @Test
    public void testPrintEscapesToo() {
        Assert.assertEquals(render("<?php print \"a<b\";"), "a&lt;b");
    }

    @Test
    public void testTheMarkupOfTheTemplateItselfIsNeverEscaped() {
        Assert.assertEquals(render("<p class=\"x\">a &amp; b</p>"), "<p class=\"x\">a &amp; b</p>");
    }

    @Test
    public void testAttributeValuesAreSafeBecauseBothQuoteKindsAreEscaped() {
        Assert.assertEquals(
                render("<?php $v = \"\\\" onmouseover=\\\"evil()\"; ?><a title=\"<?= $v ?>\">x</a>"),
                "<a title=\"&quot; onmouseover=&quot;evil()\">x</a>");
    }

    @Test
    public void testAlreadyEscapedTextIsNotEscapedTwice() {
        Assert.assertEquals(render("<?php echo htmlspecialchars(\"a&b\");"), "a&amp;b");
    }

    @Test
    public void testSafetyDoesNotSurviveConcatenation() {
        Assert.assertEquals(
                render("<?php echo raw(\"<b>\") . \"<i>\";"),
                "&lt;b&gt;&lt;i&gt;",
                "joining a safe string to an unsafe one gives an unsafe one");
    }

    @Test
    public void testAHostCanTurnEscapingOff() {
        PhpEngineConfig stockPhp = PhpEngineConfig.DEFAULTS.withoutEscaping();
        Env env = new Env(stockPhp, null, Builtins.registry());
        PhpParser.parse("<?php echo \"<b>\";", "/t.php", stockPhp).render(env);
        Assert.assertEquals(env.output(), "<b>");
    }

    // --------------------------------------------------------------- functions

    @DataProvider(name = "functions")
    public Object[][] functions() {
        return new Object[][] {
            // strings, counted in characters
            {"strlen(\"héllo\")", "5"},
            {"mb_strlen(\"héllo\")", "5"},
            {"substr(\"abcdef\", -3, 2)", "de"},
            {"substr(\"abcdef\", 1, -1)", "bcde"},
            {"strpos(\"hello\", \"ll\")", "2"},
            {"str_contains(\"hello\", \"ell\") ? \"y\" : \"n\"", "y"},
            {"str_replace(\"a\", \"b\", \"banana\")", "bbnbnb"},
            {"str_repeat(\"ab\", 3)", "ababab"},
            {"trim(\"  x  \")", "x"},
            {"ucwords(\"hello big world\")", "Hello Big World"},
            {"ucfirst(\"hello\")", "Hello"},
            {"implode(\", \", [\"a\", \"b\"])", "a, b"},
            {"str_pad(\"x\", 7, \"-\", STR_PAD_BOTH)", "---x---"},
            {"wordwrap(\"The quick brown fox\", 10, \"|\", true)", "The quick|brown fox"},
            {"number_format(1234567.891, 2)", "1,234,567.89"},
            {"number_format(1234.5)", "1,235"},

            // sprintf
            {"sprintf(\"%05.2f\", 3.14159)", "03.14"},
            {"sprintf(\"%-6s|\", \"ab\")", "ab    |"},
            {"sprintf('%1$s %2$s %1$s', 'a', 'b')", "a b a"},
            {"sprintf(\"%+d %+d\", 5, -5)", "+5 -5"},
            {"sprintf(\"%x %X %o %b\", 255, 255, 8, 5)", "ff FF 10 101"},

            // arrays
            {"count([1, 2, 3])", "3"},
            {"implode(\",\", array_keys([\"a\" => 1, \"b\" => 2]))", "a,b"},
            {"implode(\",\", array_values([\"a\" => 1, \"b\" => 2]))", "1,2"},
            {"in_array(2, [1, 2]) ? \"y\" : \"n\"", "y"},
            {"in_array(\"2\", [1, 2], true) ? \"y\" : \"n\"", "n"},
            {"implode(\",\", range(1, 5, 2))", "1,3,5"},
            {"implode(\",\", range(\"a\", \"e\"))", "a,b,c,d,e"},
            {"implode(\",\", array_slice([1, 2, 3, 4], -2))", "3,4"},
            {"implode(\",\", array_reverse([1, 2, 3]))", "3,2,1"},
            {"array_sum([1, 2, 3])", "6"},
            {"array_product([2, 3])", "6"},
            {"min([3, 1, 2])", "1"},
            {"max(3, 9, 2)", "9"},
            {"array_key_first([\"a\" => 1, \"b\" => 2])", "a"},
            {"array_key_last([\"a\" => 1, \"b\" => 2])", "b"},
            {"implode(\",\", array_unique([1, 2, 2, 3]))", "1,2,3"},
            {"implode(\",\", array_map(fn($n) => $n * 2, [1, 2, 3]))", "2,4,6"},
            {"implode(\",\", array_filter([1, 2, 3, 4], fn($n) => $n % 2 == 0))", "2,4"},

            // types
            {"gettype(1.0)", "double"},
            {"get_debug_type(1.0)", "float"},
            {"is_array([]) ? \"y\" : \"n\"", "y"},
            {"is_numeric(\"12.5\") ? \"y\" : \"n\"", "y"},
            {"is_numeric(\"12abc\") ? \"y\" : \"n\"", "n"},
            {"intval(\"42abc\")", "42"},

            // maths
            {"round(2.5) . \" \" . round(3.5) . \" \" . round(-2.5)", "3 4 -3"},
            {"round(1.955, 2)", "1.96"},
            {"ceil(4.1) . \" \" . floor(-4.1)", "5 -5"},
            {"abs(-7)", "7"},
            {"intdiv(7, 2)", "3"},
            {"(int) ceil(10 / 3)", "4"},

            // dates, always in UTC
            {"date(\"Y-m-d H:i:s D N w z t L\", 1700000000)", "2023-11-14 22:13:20 Tue 2 2 317 30 0"},
            {"date(\"c\", 1700000000)", "2023-11-14T22:13:20+00:00"},
            {"date(\"r\", 1700000000)", "Tue, 14 Nov 2023 22:13:20 +0000"},

            // constants exist
            {"PHP_EOL === \"\\n\" ? \"y\" : \"n\"", "y"},
            {"PHP_INT_MAX", "9223372036854775807"},
        };
    }

    @Test(dataProvider = "functions")
    public void testFunction(String expression, String expected) {
        Assert.assertEquals(render("<?php echo " + expression + ";"), expected, expression);
    }

    @Test
    public void testJsonEncodeMatchesPhpDefaults() {
        Assert.assertEquals(
                render("<?php echo raw(json_encode([\"a\" => 1, \"b\" => \"<x>\"]));"), "{\"a\":1,\"b\":\"<x>\"}");
        Assert.assertEquals(render("<?php echo raw(json_encode([1, 2, 3]));"), "[1,2,3]");
        Assert.assertEquals(
                render("<?php echo raw(json_encode([\"u\" => \"a/b\"]));"),
                "{\"u\":\"a\\/b\"}",
                "slashes escape by default");
        Assert.assertEquals(
                render("<?php echo raw(json_encode([\"u\" => \"a/b\"], JSON_UNESCAPED_SLASHES));"), "{\"u\":\"a/b\"}");
        Assert.assertEquals(
                render("<?php echo raw(json_encode([\"b\" => \"<x>\"], JSON_HEX_TAG));"),
                "{\"b\":\"\\u003Cx\\u003E\"}");
    }

    // ------------------------------------------------------- the sorts, in place

    @DataProvider(name = "sorts")
    public Object[][] sorts() {
        return new Object[][] {
            {"$a = [3, 1, 2]; sort($a);", "1,2,3"},
            {"$a = [3, 1, 2]; rsort($a);", "3,2,1"},
            {"$a = [\"b\" => 2, \"a\" => 1]; ksort($a);", "1,2"},
            {"$a = [\"b\" => 2, \"a\" => 1]; asort($a);", "1,2"},
            {"$a = [3, 1, 2]; usort($a, fn($x, $y) => $y <=> $x);", "3,2,1"},
        };
    }

    @Test(dataProvider = "sorts")
    public void testASortRearrangesTheVariableItWasGiven(String setup, String expected) {
        Assert.assertEquals(render("<?php " + setup + " echo implode(\",\", $a);"), expected, setup);
    }

    @Test
    public void testKsortKeepsItsKeys() {
        Assert.assertEquals(
                render("<?php $a = [\"b\" => 2, \"a\" => 1]; ksort($a); echo implode(\",\", array_keys($a));"), "a,b");
    }

    // ------------------------------------------------------------------ refusals

    @Test
    public void testTheDebugFunctionsAreOffUnlessTheHostAsks() {
        PhpProcessingException failure =
                Assert.expectThrows(PhpProcessingException.class, () -> render("<?php print_r([1]);"));
        Assert.assertTrue(failure.getMessage().contains("debug functions on"), failure.getMessage());
    }

    @Test
    public void testTheDebugFunctionsWorkWhenTheHostAsks() {
        PhpEngineConfig debugging = PhpEngineConfig.DEFAULTS.withDebugFunctions();
        Env env = new Env(debugging, null, Builtins.registry());
        PhpParser.parse("<?php echo raw(print_r([1], true));", "/t.php", debugging)
                .render(env);
        Assert.assertTrue(env.output().contains("0 => 1"), env.output());
    }

    @Test
    public void testAnUnknownFunctionIsNamed() {
        PhpProcessingException failure =
                Assert.expectThrows(PhpProcessingException.class, () -> render("<?php echo nosuchthing(1);"));
        Assert.assertTrue(failure.getMessage().contains("undefined function nosuchthing()"), failure.getMessage());
    }

    @Test
    public void testTheWrongNumberOfArgumentsIsNamed() {
        PhpProcessingException failure =
                Assert.expectThrows(PhpProcessingException.class, () -> render("<?php echo strlen();"));
        Assert.assertTrue(failure.getMessage().contains("strlen() expects 1 argument"), failure.getMessage());
    }

    private static String render(String template) {
        Env env = new Env(PhpEngineConfig.DEFAULTS, null, Builtins.registry());
        PhpParser.parse(template, "/test.php", PhpEngineConfig.DEFAULTS).render(env);
        return env.output();
    }
}
