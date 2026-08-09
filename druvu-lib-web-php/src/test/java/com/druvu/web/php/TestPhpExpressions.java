package com.druvu.web.php;

import com.druvu.web.php.internal.PhpEngineConfig;
import com.druvu.web.php.internal.PhpProcessingException;
import com.druvu.web.php.internal.PhpSyntaxException;
import com.druvu.web.php.internal.builtin.Builtins;
import com.druvu.web.php.internal.lex.PhpLexer;
import com.druvu.web.php.internal.lex.Token;
import com.druvu.web.php.internal.parse.PhpParser;
import com.druvu.web.php.internal.runtime.Env;
import com.druvu.web.php.internal.value.ArrayKey;
import com.druvu.web.php.internal.value.PhpArray;
import com.druvu.web.php.internal.value.PhpNull;
import com.druvu.web.php.internal.value.PhpValue;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * The expression layer.
 *
 * <p>Every expectation in the tables below was taken from PHP 8.5.9 rather than from the manual — the whole file is a
 * differential test that happens to run without PHP installed.
 */
public class TestPhpExpressions {

    @DataProvider(name = "expressions")
    public Object[][] expressions() {
        return new Object[][] {
            // precedence, including the PHP 8 rule that puts concatenation below + and -
            {"\"x\" . 1 + 2", "string", "x3"},
            {"1 + 2 . \"x\"", "string", "3x"},
            {"-2 ** 2", "integer", "-4"},
            {"2 ** 3 ** 2", "integer", "512"},
            {"2 ** -1", "double", "0.5"},
            {"!0 * 3", "integer", "3"},
            {"true ? 1 : 2", "integer", "1"},
            {"null ?: \"fallback\"", "string", "fallback"},
            {"null ?? \"a\" ?? \"b\"", "string", "a"},
            {"(1 + 2) * 3", "integer", "9"},

            // arithmetic keeps integers integral where PHP does
            {"10 / 2", "integer", "5"},
            {"10 / 3", "double", "3.3333333333333"},
            {"10 / 4", "double", "2.5"},
            {"7 % 3", "integer", "1"},
            {"-7 % 3", "integer", "-1"},
            {"7 % -3", "integer", "1"},
            {"2 ** 3", "integer", "8"},
            {"\"5\" + 1", "integer", "6"},
            {"\"5.5\" + 1", "double", "6.5"},
            {"\"5abc\" + 1", "integer", "6"},
            {"true + true", "integer", "2"},
            {"null + 1", "integer", "1"},
            {"1 . 2", "string", "12"},
            {"-\"5\"", "integer", "-5"},
            {"+\"5.5\"", "double", "5.5"},

            // comparison
            {"1 <=> 2", "integer", "-1"},
            {"\"10\" == \"1e1\"", "boolean", "1"},
            {"0 == \"foo\"", "boolean", ""},
            {"1 === 1.0", "boolean", ""},
            {"\"abc\" < \"abd\"", "boolean", "1"},

            // logical, including the short-circuit that keeps the right side from running
            {"false && (1 / 0)", "boolean", ""},
            {"true || (1 / 0)", "boolean", "1"},
            {"true xor true", "boolean", ""},

            // isset and empty never complain about what is not there
            {"isset($undefined)", "boolean", ""},
            {"empty(\"0\")", "boolean", "1"},
            {"empty(\"0.0\")", "boolean", ""},
            {"empty([])", "boolean", "1"},
            {"$undefined ?? \"fallback\"", "string", "fallback"},

            // casts
            {"(int) \"0x1A\"", "integer", "0"},
            {"(int) \"12abc\"", "integer", "12"},
            {"(bool) \"0\"", "boolean", ""},
            {"(string) 1.0", "string", "1"},
            {"(float) \"2.5\"", "double", "2.5"},

            // match compares strictly
            {"match (1) { 1 => \"a\", default => \"b\" }", "string", "a"},
            {"match (\"1\") { 1 => \"a\", default => \"b\" }", "string", "b"},
            {"match (2) { 1, 2 => \"ab\", default => \"c\" }", "string", "ab"},

            // string offsets read one character, counting in code points
            {"\"hello\"[0]", "string", "h"},
            {"\"hello\"[-1]", "string", "o"},
            {"\"héllo\"[1]", "string", "é"},

            // arrays
            {"[1, 2, 3][1]", "integer", "2"},
            {"[\"a\" => 1][\"a\"]", "integer", "1"},
            {"array(1, 2)[0]", "integer", "1"},
            {"[[1, 2], [3]][0][1]", "integer", "2"},
        };
    }

    @Test(dataProvider = "expressions")
    public void testExpression(String source, String expectedType, String expectedValue) {
        PhpValue value = evaluate(source);
        Assert.assertEquals(value.typeName(), expectedType, "type of " + source);
        Assert.assertEquals(value.toStr(), expectedValue, "value of " + source);
    }

    @DataProvider(name = "refusals")
    public Object[][] refusals() {
        return new Object[][] {
            {"1 / 0", "Division by zero"},
            {"1 % 0", "Modulo by zero"},
            {"\"abc\" + 1", "Unsupported operand types: string + integer"},
            {"[] + 1", "Unsupported operand types: array + integer"},
            {"match (9) { 1 => \"a\" }", "unhandled match case 9"},
        };
    }

    @Test(dataProvider = "refusals")
    public void testRefusedAtRuntime(String source, String expectedInMessage) {
        PhpProcessingException failure = Assert.expectThrows(PhpProcessingException.class, () -> evaluate(source));
        Assert.assertTrue(
                failure.getMessage().contains(expectedInMessage),
                "expected '" + expectedInMessage + "' in: " + failure.getMessage());
    }

    @DataProvider(name = "parseRefusals")
    public Object[][] parseRefusals() {
        return new Object[][] {
            {"1 ? 2 : 3 ? 4 : 5", "unparenthesised"},
            {"1 < 2 < 3", "comparison operators do not chain"},
            {"1 == 2 == 3", "equality operators do not chain"},
            {"1 = 2", "Cannot assign"},
            {"foo", "Undefined constant"},
        };
    }

    @Test(dataProvider = "parseRefusals")
    public void testRefusedAtParseTime(String source, String expectedInMessage) {
        PhpSyntaxException failure = Assert.expectThrows(PhpSyntaxException.class, () -> evaluate(source));
        Assert.assertTrue(
                failure.getMessage().contains(expectedInMessage),
                "expected '" + expectedInMessage + "' in: " + failure.getMessage());
    }

    // ------------------------------------------------------------ statefulness

    @DataProvider(name = "sequences")
    public Object[][] sequences() {
        return new Object[][] {
            {new String[] {"$a = 1", "$a += 2"}, "3"},
            {new String[] {"$a = null", "$a ??= 5"}, "5"},
            {new String[] {"$a = 1", "$a ??= 5"}, "1"},
            {new String[] {"$s = \"a\"", "$s .= \"b\""}, "ab"},
            {new String[] {"$a = $b = 7", "$a + $b"}, "14"},

            // ++ on null gives 1; -- on null does nothing at all
            {new String[] {"$n = null", "++$n"}, "1"},
            {new String[] {"$n = null", "--$n"}, ""},
            {new String[] {"$i = 5", "$i++"}, "5"},
            {new String[] {"$i = 5", "$i++", "$i"}, "6"},
            {new String[] {"$i = 5", "++$i"}, "6"},

            // assignment binds tighter than the word operators, so $x keeps true
            {new String[] {"$x = true and false", "$x"}, "1"},
            {new String[] {"$y = true && false", "$y"}, ""},

            // an arrow function captures by value, at the moment it is written
            {new String[] {"$m = 3", "$f = fn($x) => $x * $m", "$m = 99", "$f(2)"}, "6"},
            {new String[] {"$f = fn($a, $b) => $a . $b", "$f(\"x\", \"y\")"}, "xy"},
        };
    }

    @Test(dataProvider = "sequences")
    public void testSequence(String[] sources, String expectedValue) {
        Assert.assertEquals(evaluateAll(sources).toStr(), expectedValue, String.join(" ; ", sources));
    }

    @Test
    public void testArraysAreCopiedOnAssignment() {
        Env env = newEnv();
        evaluate("$a = [1]", env);
        evaluate("$b = $a", env);
        evaluate("$b[] = 2", env);
        Assert.assertEquals(((PhpArray) evaluate("$a", env)).size(), 1, "the original must not see the append");
        Assert.assertEquals(((PhpArray) evaluate("$b", env)).size(), 2);
    }

    @Test
    public void testWritingAnIndexGrowsTheArrayOutOfNothing() {
        Env env = newEnv();
        evaluate("$a[\"x\"][\"y\"] = 1", env);
        PhpArray outer = (PhpArray) evaluate("$a", env);
        PhpArray inner = (PhpArray) outer.get(ArrayKey.of("x"));
        Assert.assertEquals(inner.get(ArrayKey.of("y")).toStr(), "1");
    }

    @Test
    public void testAppendingToAnUndefinedVariableCreatesTheArray() {
        Env env = newEnv();
        evaluate("$a[] = \"x\"", env);
        Assert.assertEquals(
                ((PhpArray) evaluate("$a", env)).get(ArrayKey.of(0L)).toStr(), "x");
    }

    @Test
    public void testArrayUnionKeepsTheLeftHandKeys() {
        PhpArray union = (PhpArray) evaluate("[1, 2] + [3, 4, 5]");
        Assert.assertEquals(union.size(), 3);
        Assert.assertEquals(union.get(ArrayKey.of(0L)).toStr(), "1");
        Assert.assertEquals(union.get(ArrayKey.of(1L)).toStr(), "2");
        Assert.assertEquals(union.get(ArrayKey.of(2L)).toStr(), "5");
    }

    @Test
    public void testArrayLiteralIndexesFromTheHighestKeySoFar() {
        PhpArray array = (PhpArray) evaluate("[5 => \"a\", \"b\"]");
        Assert.assertTrue(array.containsKey(ArrayKey.of(5L)));
        Assert.assertEquals(array.get(ArrayKey.of(6L)).toStr(), "b");
    }

    // ------------------------------------------------------------ diagnostics

    @Test
    public void testAnUndefinedVariableIsReportedButDoesNotStopTheRender() {
        Env env = newEnv();
        Assert.assertSame(evaluate("$missing", env), PhpNull.NULL);
        Assert.assertEquals(env.diagnostics().size(), 1);
        Assert.assertTrue(env.diagnostics().get(0).message().contains("Undefined variable $missing"));
    }

    @Test
    public void testAMissingArrayKeyIsReported() {
        Env env = newEnv();
        evaluate("$a = []", env);
        evaluate("$a[\"nope\"]", env);
        Assert.assertEquals(env.diagnostics().size(), 1);
        Assert.assertTrue(env.diagnostics().get(0).message().contains("Undefined array key \"nope\""));
    }

    @Test
    public void testProbingWithCoalesceReportsNothing() {
        Env env = newEnv();
        evaluate("$a = []", env);
        evaluate("$a[\"nope\"] ?? \"fallback\"", env);
        evaluate("isset($nothing)", env);
        evaluate("empty($nothing)", env);
        Assert.assertEquals(env.diagnostics(), List.of(), "probing for optional data is not a mistake");
    }

    @Test
    public void testAStringOffsetPastTheEndIsEmptyAndReported() {
        Env env = newEnv();
        Assert.assertEquals(evaluate("\"hello\"[99]", env).toStr(), "");
        Assert.assertEquals(env.diagnostics().size(), 1);
        Assert.assertTrue(env.diagnostics().get(0).message().contains("Uninitialized string offset"));
    }

    // ---------------------------------------------------------------- helpers

    private static Env newEnv() {
        return new Env(PhpEngineConfig.DEFAULTS, null, Builtins.registry());
    }

    private static PhpValue evaluate(String source) {
        return evaluate(source, newEnv());
    }

    private static PhpValue evaluate(String source, Env env) {
        List<Token> tokens = new PhpLexer("<?php " + source, "/test.php", PhpEngineConfig.DEFAULTS).tokenize();
        return new PhpParser(tokens.subList(1, tokens.size())).parseExpression().eval(env);
    }

    private static PhpValue evaluateAll(String... sources) {
        Env env = newEnv();
        PhpValue last = PhpNull.NULL;
        for (String source : sources) {
            last = evaluate(source, env);
        }
        return last;
    }
}
