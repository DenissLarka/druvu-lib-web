package com.druvu.web.php.internal.expr;

import com.druvu.web.php.internal.PhpContext;

/**
 * Represents a PHP string concatenation expression using the "." operator.
 * Example: 'Hello' . ' ' . 'World'
 */
public class ConcatExpr extends PhpExpr {

	private final PhpExpr left;
	private final PhpExpr right;

	public ConcatExpr(PhpExpr left, PhpExpr right) {
		this.left = left;
		this.right = right;
	}

	@Override
	public String evaluate(PhpContext context) {
		return left.evaluate(context) + right.evaluate(context);
	}

	@Override
	public boolean isLiteral() {
		return left.isLiteral() && right.isLiteral();
	}

	@Override
	public String toString() {
		return "ConcatExpr{" + left + " . " + right + "}";
	}
}
