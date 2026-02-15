package com.druvu.web.php;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.druvu.web.php.internal.PhpProcessingException;
import com.druvu.web.php.internal.expr.PhpFunction;
import com.druvu.web.php.internal.expr.PhpFunctionRegistry;

public class TestPhpFunctionRegistry {

	@Test
	public void testRegisterAndGet() {
		PhpFunctionRegistry registry = new PhpFunctionRegistry();
		PhpFunction fn = (ctx, args) -> "result";
		registry.register("test", fn);

		Assert.assertSame(registry.get("test"), fn);
	}

	@Test
	public void testHas() {
		PhpFunctionRegistry registry = new PhpFunctionRegistry();
		registry.register("exists", (ctx, args) -> "");

		Assert.assertTrue(registry.has("exists"));
		Assert.assertFalse(registry.has("missing"));
	}

	@Test(expectedExceptions = PhpProcessingException.class, expectedExceptionsMessageRegExp = ".*Unknown function: missing\\(\\).*")
	public void testGetUnknownFunctionThrows() {
		PhpFunctionRegistry registry = new PhpFunctionRegistry();
		registry.get("missing");
	}

	@Test
	public void testOverwriteFunction() {
		PhpFunctionRegistry registry = new PhpFunctionRegistry();
		registry.register("fn", (ctx, args) -> "first");
		registry.register("fn", (ctx, args) -> "second");

		Assert.assertEquals(registry.get("fn").call(null, null), "second");
	}
}
