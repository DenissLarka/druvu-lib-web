package com.druvu.web.php;

import java.util.List;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.druvu.web.php.internal.PhpContext;
import com.druvu.web.php.internal.PhpProcessingException;
import com.druvu.web.php.internal.expr.LiteralStringExpr;
import com.druvu.web.php.internal.expr.PhpExpr;
import com.druvu.web.php.internal.expr.PhpExpressionParser;
import com.druvu.web.php.internal.expr.PhpFunctionRegistry;
import com.druvu.web.php.internal.func.BuiltInFunctions;

public class TestBuiltInFunctions {

	private PhpFunctionRegistry registry;

	@BeforeMethod
	public void setup() {
		registry = new PhpFunctionRegistry();
		BuiltInFunctions.registerAll(registry);
	}

	@Test
	public void testAllFunctionsRegistered() {
		Assert.assertTrue(registry.has("webjar"));
		Assert.assertTrue(registry.has("context"));
		Assert.assertTrue(registry.has("link"));
	}

	@Test
	public void testContext() {
		PhpContext ctx = createContext("/myapp");
		String result = registry.get("context").call(ctx, List.of());
		Assert.assertEquals(result, "/myapp");
	}

	@Test
	public void testContextEmpty() {
		PhpContext ctx = createContext("");
		String result = registry.get("context").call(ctx, List.of());
		Assert.assertEquals(result, "");
	}

	@Test
	public void testLink() {
		PhpContext ctx = createContext("/myapp");
		List<PhpExpr> args = List.of(new LiteralStringExpr("dashboard"));
		String result = registry.get("link").call(ctx, args);
		Assert.assertEquals(result, "/myapp/dashboard");
	}

	@Test(expectedExceptions = PhpProcessingException.class, expectedExceptionsMessageRegExp = ".*link\\(\\) expects 1 argument.*")
	public void testLinkNoArgs() {
		PhpContext ctx = createContext("/myapp");
		registry.get("link").call(ctx, List.of());
	}

	@Test
	public void testContextFunctionViaParser() {
		PhpContext ctx = createContext("/app");
		PhpExpr expr = new PhpExpressionParser("context() . '/static/style.css'").parse();
		Assert.assertEquals(expr.evaluate(ctx), "/app/static/style.css");
	}

	@Test
	public void testLinkFunctionViaParser() {
		PhpContext ctx = createContext("/app");
		PhpExpr expr = new PhpExpressionParser("link('dashboard')").parse();
		Assert.assertEquals(expr.evaluate(ctx), "/app/dashboard");
	}

	private PhpContext createContext(String contextPath) {
		// Minimal HttpServletRequest stub for getContextPath()
		var request = java.lang.reflect.Proxy.newProxyInstance(
				getClass().getClassLoader(),
				new Class[]{jakarta.servlet.http.HttpServletRequest.class},
				(proxy, method, args) -> {
					if ("getContextPath".equals(method.getName())) {
						return contextPath;
					}
					return null;
				}
		);
		return new PhpContext(
				(jakarta.servlet.http.HttpServletRequest) request,
				null, "/test.php", p -> null, registry
		);
	}
}
