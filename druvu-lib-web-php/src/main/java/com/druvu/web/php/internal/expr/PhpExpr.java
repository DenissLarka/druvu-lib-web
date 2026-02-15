package com.druvu.web.php.internal.expr;

import com.druvu.web.php.internal.PhpContext;

/**
 * Abstract base class for PHP expressions.
 * Each expression type implements the evaluate() method to return its value.
 */
public abstract class PhpExpr {

	/**
	 * Evaluates this expression in the given context.
	 *
	 * @param context the PHP execution context
	 * @return the evaluated result as a string
	 */
	public abstract String evaluate(PhpContext context);

	/**
	 * Returns true if this expression is a literal constant.
	 *
	 * @return true if literal
	 */
	public boolean isLiteral() {
		return false;
	}
}
