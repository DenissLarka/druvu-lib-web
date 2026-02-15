package com.druvu.web.php;

import java.util.List;
import java.util.Locale;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.druvu.web.php.internal.PhpContext;
import com.druvu.web.php.internal.PhpProcessingException;
import com.druvu.web.php.internal.expr.PhpExpr;
import com.druvu.web.php.internal.expr.PhpExpressionParser;
import com.druvu.web.php.internal.expr.PhpFunctionRegistry;

public class TestPhpExpressionParserFunctions {

	private PhpFunctionRegistry registry;
	private PhpContext context;

	@BeforeMethod
	public void setup() {
		registry = new PhpFunctionRegistry();
		registry.register("greet", (ctx, args) -> "hello");
		registry.register("upper", (ctx, args) -> args.getFirst().evaluate(ctx).toUpperCase(Locale.ENGLISH));
		registry.register("join", (ctx, args) -> {
			StringBuilder sb = new StringBuilder();
			for (PhpExpr arg : args) {
				sb.append(arg.evaluate(ctx));
			}
			return sb.toString();
		});
		context = new PhpContext(null, null, "/test.php", p -> null, registry);
	}

	@Test
	public void testNoArgFunction() {
		PhpExpr expr = new PhpExpressionParser("greet()").parse();
		Assert.assertEquals(expr.evaluate(context), "hello");
	}

	@Test
	public void testSingleArgFunction() {
		PhpExpr expr = new PhpExpressionParser("upper('world')").parse();
		Assert.assertEquals(expr.evaluate(context), "WORLD");
	}

	@Test
	public void testMultiArgFunction() {
		PhpExpr expr = new PhpExpressionParser("join('a', 'b', 'c')").parse();
		Assert.assertEquals(expr.evaluate(context), "abc");
	}

	@Test
	public void testFunctionInConcatenation() {
		PhpExpr expr = new PhpExpressionParser("'prefix-' . greet() . '-suffix'").parse();
		Assert.assertEquals(expr.evaluate(context), "prefix-hello-suffix");
	}

	@Test
	public void testFunctionWithConcatArg() {
		PhpExpr expr = new PhpExpressionParser("upper('hello' . ' world')").parse();
		Assert.assertEquals(expr.evaluate(context), "HELLO WORLD");
	}

	@Test
	public void testFunctionWithWhitespace() {
		PhpExpr expr = new PhpExpressionParser("  greet(  )  ").parse();
		Assert.assertEquals(expr.evaluate(context), "hello");
	}

	@Test
	public void testFunctionWithUnderscoreName() {
		registry.register("my_func", (ctx, args) -> "works");
		PhpExpr expr = new PhpExpressionParser("my_func()").parse();
		Assert.assertEquals(expr.evaluate(context), "works");
	}

	@Test(expectedExceptions = PhpProcessingException.class, expectedExceptionsMessageRegExp = ".*Variables not yet supported.*")
	public void testBareIdentifierThrows() {
		new PhpExpressionParser("myVar").parse();
	}

	@Test(expectedExceptions = PhpProcessingException.class, expectedExceptionsMessageRegExp = ".*Expected '\\)'.*")
	public void testUnclosedFunctionCallThrows() {
		new PhpExpressionParser("greet(").parse();
	}
}
