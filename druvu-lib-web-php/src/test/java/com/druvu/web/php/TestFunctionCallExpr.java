package com.druvu.web.php;

import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.druvu.web.php.internal.PhpContext;
import com.druvu.web.php.internal.PhpProcessingException;
import com.druvu.web.php.internal.expr.FunctionCallExpr;
import com.druvu.web.php.internal.expr.LiteralStringExpr;
import com.druvu.web.php.internal.expr.PhpFunctionRegistry;

public class TestFunctionCallExpr {

	@Test
	public void testEvaluateCallsFunction() {
		PhpFunctionRegistry registry = new PhpFunctionRegistry();
		registry.register("echo", (ctx, args) -> args.getFirst().evaluate(ctx));
		PhpContext context = new PhpContext(null, null, "/test.php", p -> null, registry);

		FunctionCallExpr expr = new FunctionCallExpr("echo", List.of(new LiteralStringExpr("hello")));
		Assert.assertEquals(expr.evaluate(context), "hello");
	}

	@Test
	public void testEvaluateNoArgs() {
		PhpFunctionRegistry registry = new PhpFunctionRegistry();
		registry.register("constant", (ctx, args) -> "42");
		PhpContext context = new PhpContext(null, null, "/test.php", p -> null, registry);

		FunctionCallExpr expr = new FunctionCallExpr("constant", List.of());
		Assert.assertEquals(expr.evaluate(context), "42");
	}

	@Test(expectedExceptions = PhpProcessingException.class)
	public void testEvaluateUnknownFunction() {
		PhpFunctionRegistry registry = new PhpFunctionRegistry();
		PhpContext context = new PhpContext(null, null, "/test.php", p -> null, registry);

		FunctionCallExpr expr = new FunctionCallExpr("unknown", List.of());
		expr.evaluate(context);
	}

	@Test
	public void testIsLiteralReturnsFalse() {
		FunctionCallExpr expr = new FunctionCallExpr("fn", List.of());
		Assert.assertFalse(expr.isLiteral());
	}

	@Test
	public void testToString() {
		FunctionCallExpr expr = new FunctionCallExpr("test", List.of(new LiteralStringExpr("arg")));
		String str = expr.toString();
		Assert.assertTrue(str.contains("test"));
		Assert.assertTrue(str.contains("arg"));
	}
}
