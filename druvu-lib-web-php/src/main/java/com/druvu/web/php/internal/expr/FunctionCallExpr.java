package com.druvu.web.php.internal.expr;

import java.util.List;

import com.druvu.web.php.internal.PhpContext;

/**
 * Expression node representing a function call.
 * Example: {@code webjar('jquery/jquery.min.js')}
 *
 * @author Deniss Larka
 */
public class FunctionCallExpr extends PhpExpr {

	private final String functionName;
	private final List<PhpExpr> arguments;

	public FunctionCallExpr(String functionName, List<PhpExpr> arguments) {
		this.functionName = functionName;
		this.arguments = arguments;
	}

	@Override
	public String evaluate(PhpContext context) {
		PhpFunction fn = context.getFunctionRegistry().get(functionName);
		return fn.call(context, arguments);
	}

	@Override
	public String toString() {
		return "FunctionCallExpr{" + functionName + "(" + arguments + ")}";
	}
}
