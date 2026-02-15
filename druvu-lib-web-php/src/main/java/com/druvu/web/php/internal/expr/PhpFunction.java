package com.druvu.web.php.internal.expr;

import java.util.List;

import com.druvu.web.php.internal.PhpContext;

/**
 * Functional interface for PHP functions callable from expressions.
 * Functions receive the context and a list of unevaluated argument expressions.
 *
 * @author Deniss Larka
 */
@FunctionalInterface
public interface PhpFunction {

	/**
	 * Calls this function with the given context and arguments.
	 *
	 * @param context the PHP execution context
	 * @param args    the argument expressions (unevaluated)
	 * @return the function result as a string
	 */
	String call(PhpContext context, List<PhpExpr> args);
}
