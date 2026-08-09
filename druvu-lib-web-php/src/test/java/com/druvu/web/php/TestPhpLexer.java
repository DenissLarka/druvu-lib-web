package com.druvu.web.php;

import com.druvu.web.php.internal.Location;
import com.druvu.web.php.internal.PhpEngineConfig;
import com.druvu.web.php.internal.PhpSyntaxException;
import com.druvu.web.php.internal.ast.stmt.TextStatement;
import com.druvu.web.php.internal.lex.PhpLexer;
import com.druvu.web.php.internal.lex.Symbol;
import com.druvu.web.php.internal.lex.Token;
import com.druvu.web.php.internal.lex.TokenType;
import com.druvu.web.php.internal.runtime.Env;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * The lexer, examined token by token.
 *
 * <p>This is the layer where template engines rot — a closing tag inside a string, a comment that a closing tag ends,
 * the newline a closing tag eats — so the boundary cases are tested against the tokens rather than against rendered
 * output, where a mistake would show up as a subtly wrong page instead of a wrong token.
 */
public class TestPhpLexer {

    // ------------------------------------------------------------------- tags

    @Test
    public void testPlainHtmlIsOneToken() {
        List<Token> tokens = lex("<h1>hello</h1>");
        Assert.assertEquals(types(tokens), List.of(TokenType.INLINE_HTML, TokenType.EOF));
        Assert.assertEquals(tokens.get(0).text(), "<h1>hello</h1>");
    }

    @Test
    public void testEmptyTemplateIsJustTheEnd() {
        Assert.assertEquals(types(lex("")), List.of(TokenType.EOF));
    }

    @DataProvider(name = "openTags")
    public Object[][] openTags() {
        return new Object[][] {
            {"<?php 1; ?>", TokenType.OPEN_TAG},
            {"<?PHP 1; ?>", TokenType.OPEN_TAG},
            {"<?Php\n1; ?>", TokenType.OPEN_TAG},
            {"<?= 1 ?>", TokenType.OPEN_TAG_ECHO},
            {"<?=1?>", TokenType.OPEN_TAG_ECHO},
        };
    }

    @Test(dataProvider = "openTags")
    public void testOpenTagForms(String source, TokenType expected) {
        Assert.assertEquals(lex(source).get(0).type(), expected);
    }

    @Test
    public void testBareShortTagIsTextByDefault() {
        List<Token> tokens = lex("<? echo 1; ?>");
        Assert.assertEquals(types(tokens), List.of(TokenType.INLINE_HTML, TokenType.EOF));
    }

    @Test
    public void testAnXmlDeclarationSurvivesAsText() {
        List<Token> tokens = lex("<?xml version=\"1.0\"?><page/>");
        Assert.assertEquals(types(tokens), List.of(TokenType.INLINE_HTML, TokenType.EOF));
        Assert.assertEquals(tokens.get(0).text(), "<?xml version=\"1.0\"?><page/>");
    }

    @Test
    public void testBareShortTagOpensWhenTheHostAsksForIt() {
        PhpEngineConfig shortTags = new PhpEngineConfig(true, true, 10, 1000L, false);
        List<Token> tokens = new PhpLexer("<? 1; ?>", "/t.php", shortTags).tokenize();
        Assert.assertEquals(tokens.get(0).type(), TokenType.OPEN_TAG);
    }

    @Test
    public void testPhpMustBeFollowedByWhitespace() {
        Assert.assertEquals(types(lex("<?phpecho 1;")), List.of(TokenType.INLINE_HTML, TokenType.EOF));
    }

    @Test
    public void testOpenTagAtTheVeryEndIsLegal() {
        Assert.assertEquals(types(lex("<?php")), List.of(TokenType.OPEN_TAG, TokenType.EOF));
    }

    @Test
    public void testAnOmittedClosingTagIsLegal() {
        Assert.assertEquals(
                types(lex("<?php $a;")),
                List.of(TokenType.OPEN_TAG, TokenType.VARIABLE, TokenType.SYMBOL, TokenType.EOF));
    }

    // ------------------------------------------------ the newline after ?>

    @DataProvider(name = "closingTagWhitespace")
    public Object[][] closingTagWhitespace() {
        return new Object[][] {
            {"<?php 1; ?>\nrest", "rest"},
            {"<?php 1; ?>\r\nrest", "rest"},
            {"<?php 1; ?>\rrest", "rest"},
            {"<?php 1; ?>\n\nrest", "\nrest"},
            {"<?php 1; ?>  \nrest", "  \nrest"},
            {"<?php 1; ?>rest", "rest"},
        };
    }

    @Test(dataProvider = "closingTagWhitespace")
    public void testClosingTagEatsExactlyOneLineEnding(String source, String expectedText) {
        List<Token> tokens = lex(source);
        Token html = tokens.get(tokens.size() - 2);
        Assert.assertEquals(html.type(), TokenType.INLINE_HTML);
        Assert.assertEquals(html.text(), expectedText);
    }

    // --------------------------------------------- ?> that must not close

    @Test
    public void testClosingTagInsideASingleQuotedStringDoesNotCloseTheBlock() {
        List<Token> tokens = lex("<?php 'a?>b'; ?>tail");
        Assert.assertEquals(
                types(tokens),
                List.of(
                        TokenType.OPEN_TAG,
                        TokenType.STRING_LITERAL,
                        TokenType.SYMBOL,
                        TokenType.CLOSE_TAG,
                        TokenType.INLINE_HTML,
                        TokenType.EOF));
        Assert.assertEquals(tokens.get(1).value().toStr(), "a?>b");
    }

    @Test
    public void testClosingTagInsideADoubleQuotedStringDoesNotCloseTheBlock() {
        List<Token> tokens = lex("<?php \"a?>b\"; ?>");
        Assert.assertEquals(tokens.get(1).type(), TokenType.STRING_LITERAL);
        Assert.assertEquals(tokens.get(1).value().toStr(), "a?>b");
    }

    @Test
    public void testClosingTagEndsALineComment() {
        List<Token> tokens = lex("<?php // a comment ?>tail");
        Assert.assertEquals(
                types(tokens), List.of(TokenType.OPEN_TAG, TokenType.CLOSE_TAG, TokenType.INLINE_HTML, TokenType.EOF));
        Assert.assertEquals(tokens.get(2).text(), "tail");
    }

    // --------------------------------------------------------------- comments

    @DataProvider(name = "comments")
    public Object[][] comments() {
        return new Object[][] {
            {"<?php // gone\n$a;"},
            {"<?php # gone\n$a;"},
            {"<?php /* gone */ $a;"},
            {"<?php /* gone\nover two lines */ $a;"},
        };
    }

    @Test(dataProvider = "comments")
    public void testCommentsProduceNoTokens(String source) {
        Assert.assertEquals(
                types(lex(source)), List.of(TokenType.OPEN_TAG, TokenType.VARIABLE, TokenType.SYMBOL, TokenType.EOF));
    }

    @Test
    public void testAttributesAreRefusedRatherThanReadAsAComment() {
        assertRejects("<?php #[Attr] $a;", "attributes");
    }

    @Test
    public void testUnterminatedBlockCommentIsRefused() {
        assertRejects("<?php /* never ends", "Unterminated block comment");
    }

    // --------------------------------------------------------------- literals

    @DataProvider(name = "numbers")
    public Object[][] numbers() {
        return new Object[][] {
            {"1", TokenType.INT_LITERAL, "1"},
            {"0", TokenType.INT_LITERAL, "0"},
            {"42", TokenType.INT_LITERAL, "42"},
            {"1_000_000", TokenType.INT_LITERAL, "1000000"},
            {"1.5", TokenType.FLOAT_LITERAL, "1.5"},
            {"1.0", TokenType.FLOAT_LITERAL, "1"},
            {"1e3", TokenType.FLOAT_LITERAL, "1000"},
            {"1E3", TokenType.FLOAT_LITERAL, "1000"},
            {"1.5e-3", TokenType.FLOAT_LITERAL, "0.0015"},
            {"2.5e+2", TokenType.FLOAT_LITERAL, "250"},
            {"99999999999999999999", TokenType.FLOAT_LITERAL, "1.0E+20"},
        };
    }

    @Test(dataProvider = "numbers")
    public void testNumberLiterals(String written, TokenType expectedType, String expectedValue) {
        Token token = lex("<?php " + written).get(1);
        Assert.assertEquals(token.type(), expectedType, "type of " + written);
        Assert.assertEquals(token.value().toStr(), expectedValue, "value of " + written);
    }

    @Test
    public void testANumberFollowedByEIsNotAnExponent() {
        Assert.assertEquals(
                types(lex("<?php 1e")),
                List.of(TokenType.OPEN_TAG, TokenType.INT_LITERAL, TokenType.IDENTIFIER, TokenType.EOF));
    }

    @DataProvider(name = "refusedNumbers")
    public Object[][] refusedNumbers() {
        return new Object[][] {{"0x1A"}, {"0b1010"}, {"0o17"}, {"0755"}};
    }

    @Test(dataProvider = "refusedNumbers")
    public void testOnlyDecimalNumbersAreAccepted(String written) {
        assertRejects("<?php " + written, "Only decimal numbers");
    }

    @DataProvider(name = "doubleQuotedEscapes")
    public Object[][] doubleQuotedEscapes() {
        return new Object[][] {
            {"\\n", "\n"},
            {"\\t", "\t"},
            {"\\r", "\r"},
            {"\\f", "\f"},
            {"\\v", String.valueOf((char) 0x0B)},
            {"\\e", String.valueOf((char) 0x1B)},
            {"\\\\", "\\"},
            {"\\\"", "\""},
            {"\\$", "$"},
            {"\\x41", "A"},
            {"\\x4", String.valueOf((char) 0x04)},
            {"\\xZZ", "\\xZZ"},
            {"\\101", "A"},
            {"\\0", "\0"},
            {"\\u{48}", "H"},
            {"\\u{1F600}", "\uD83D\uDE00"},
            {"\\q", "\\q"},
        };
    }

    @Test(dataProvider = "doubleQuotedEscapes")
    public void testDoubleQuotedEscapes(String written, String expected) {
        Token token = lex("<?php \"" + written + "\"").get(1);
        Assert.assertEquals(token.value().toStr(), expected, "escape " + written);
    }

    @Test
    public void testSingleQuotedStringsKnowOnlyTwoEscapes() {
        Assert.assertEquals(lex("<?php '\\\\'").get(1).value().toStr(), "\\");
        Assert.assertEquals(lex("<?php '\\''").get(1).value().toStr(), "'");
        Assert.assertEquals(lex("<?php '\\n'").get(1).value().toStr(), "\\n", "a single-quoted \\n is two characters");
        Assert.assertEquals(lex("<?php '$a'").get(1).value().toStr(), "$a", "and it never interpolates");
    }

    @Test
    public void testMalformedUnicodeEscapeIsRefused() {
        assertRejects("<?php \"\\u{}\"", "Malformed");
        assertRejects("<?php \"\\u{41\"", "Malformed");
    }

    @Test
    public void testUnterminatedStringIsRefused() {
        assertRejects("<?php 'never ends", "Unterminated string");
    }

    @Test
    public void testAnInterpolatedStringBecomesARunOfTokens() {
        Assert.assertEquals(
                types(lex("<?php \"hello $name\"")),
                List.of(
                        TokenType.OPEN_TAG,
                        TokenType.INTERPOLATION_START,
                        TokenType.STRING_LITERAL,
                        TokenType.VARIABLE,
                        TokenType.INTERPOLATION_END,
                        TokenType.EOF));
    }

    @Test
    public void testAStringWithNothingInterpolatedStaysOneToken() {
        Assert.assertEquals(
                types(lex("<?php \"hello\"")), List.of(TokenType.OPEN_TAG, TokenType.STRING_LITERAL, TokenType.EOF));
    }

    @Test
    public void testBracesAroundAnExpressionAreLexedAsCode() {
        Assert.assertEquals(
                types(lex("<?php \"{$user['name']}\"")),
                List.of(
                        TokenType.OPEN_TAG,
                        TokenType.INTERPOLATION_START,
                        TokenType.VARIABLE,
                        TokenType.SYMBOL,
                        TokenType.STRING_LITERAL,
                        TokenType.SYMBOL,
                        TokenType.INTERPOLATION_END,
                        TokenType.EOF));
    }

    // ------------------------------------------------- names and symbols

    @Test
    public void testVariablesDropTheDollarFromTheirName() {
        Token token = lex("<?php $userName").get(1);
        Assert.assertEquals(token.type(), TokenType.VARIABLE);
        Assert.assertEquals(token.text(), "userName");
    }

    @Test
    public void testKeywordsAreMatchedWithoutRegardToCase() {
        Token token = lex("<?php FOREACH").get(1);
        Assert.assertTrue(token.isIdentifier("foreach"));
        Assert.assertFalse(token.isIdentifier("for"));
    }

    @DataProvider(name = "symbols")
    public Object[][] symbols() {
        return new Object[][] {
            {"<=>", Symbol.SPACESHIP},
            {"===", Symbol.IDENTICAL},
            {"!==", Symbol.NOT_IDENTICAL},
            {"??=", Symbol.COALESCE_ASSIGN},
            {"?->", Symbol.NULLSAFE_ARROW},
            {"<=", Symbol.LESS_OR_EQUAL},
            {"<>", Symbol.NOT_EQUAL_ALT},
            {"??", Symbol.COALESCE},
            {"?:", Symbol.ELVIS},
            {"**", Symbol.POWER},
            {"=>", Symbol.DOUBLE_ARROW},
            {"->", Symbol.ARROW},
            {".", Symbol.CONCAT},
            {"?", Symbol.QUESTION},
        };
    }

    @Test(dataProvider = "symbols")
    public void testTheLongestSymbolWins(String written, Symbol expected) {
        Token token = lex("<?php " + written).get(1);
        Assert.assertEquals(token.type(), TokenType.SYMBOL, "type of " + written);
        Assert.assertEquals(token.symbol(), expected, "symbol " + written);
    }

    @Test
    public void testExcludedOperatorsAreRefused() {
        assertRejects("<?php $a & $b", "Unexpected character");
        assertRejects("<?php @$a", "Unexpected character");
    }

    // -------------------------------------------------------------- positions

    @Test
    public void testTokensKnowTheirLineAndColumn() {
        List<Token> tokens = lex("line one\n<?php $a;\n$b;");

        Location html = tokens.get(0).location();
        Assert.assertEquals(html.line(), 1);
        Assert.assertEquals(html.column(), 1);

        Location openTag = tokens.get(1).location();
        Assert.assertEquals(openTag.line(), 2);
        Assert.assertEquals(openTag.column(), 1);

        Location firstVariable = tokens.get(2).location();
        Assert.assertEquals(firstVariable.line(), 2);
        Assert.assertEquals(firstVariable.column(), 7);

        Location secondVariable = tokens.get(4).location();
        Assert.assertEquals(secondVariable.line(), 3);
        Assert.assertEquals(secondVariable.column(), 1);
    }

    @Test
    public void testAnErrorNamesTheFileAndTheLine() {
        PhpSyntaxException failure = Assert.expectThrows(PhpSyntaxException.class, () -> lex("<?php\n\n  0x1A"));
        Assert.assertEquals(failure.location().line(), 3);
        Assert.assertEquals(failure.location().column(), 3);
        Assert.assertTrue(failure.getMessage().startsWith("/test.php:3:3: "), failure.getMessage());
    }

    // ------------------------------------------------------------- round trip

    /**
     * The literal text of a template, run back out through the statement that will carry it. Nothing between the tags
     * is evaluated yet, so what this really proves is that the lexer neither loses nor invents a character of markup —
     * including the one newline a closing tag is supposed to eat.
     */
    @Test
    public void testTheMarkupOfATemplateSurvivesUnchanged() {
        String template = "<ul>\n" + "<?php 1; ?>\n" + "  <li>one</li>\n" + "<?php 2; ?>\n" + "</ul>\n";

        Env env = new Env(PhpEngineConfig.DEFAULTS, null);
        for (Token token : lex(template)) {
            if (token.is(TokenType.INLINE_HTML)) {
                new TextStatement(token.location(), token.text()).execute(env);
            }
        }

        Assert.assertEquals(env.output(), "<ul>\n" + "  <li>one</li>\n" + "</ul>\n");
    }

    // ---------------------------------------------------------------- helpers

    private static List<Token> lex(String source) {
        return new PhpLexer(source, "/test.php", PhpEngineConfig.DEFAULTS).tokenize();
    }

    private static List<TokenType> types(List<Token> tokens) {
        return tokens.stream().map(Token::type).toList();
    }

    private static void assertRejects(String source, String expectedInMessage) {
        PhpSyntaxException failure = Assert.expectThrows(PhpSyntaxException.class, () -> lex(source));
        Assert.assertTrue(
                failure.getMessage().contains(expectedInMessage),
                "expected a message mentioning '" + expectedInMessage + "' but got: " + failure.getMessage());
    }
}
