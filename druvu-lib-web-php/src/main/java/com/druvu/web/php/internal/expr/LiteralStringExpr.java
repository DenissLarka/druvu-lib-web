package com.druvu.web.php.internal.expr;

import com.druvu.web.php.internal.PhpContext;

/**
 * Represents a PHP string literal expression
 * Examples: 'Hello World', "Hello World"
 */
public class LiteralStringExpr extends PhpExpr {

	private final String value;

	public LiteralStringExpr(String value) {
		this.value = value;
	}

	@Override
	public String evaluate(PhpContext context) {
		return value;
	}

	@Override
	public boolean isLiteral() {
		return true;
	}

	@Override
	public String toString() {
		return "LiteralStringExpr{\"" + value + "\"}";
	}
}
