package com.druvu.web.php.internal.func;

import java.util.List;

import org.webjars.WebJarAssetLocator;

import com.druvu.web.php.internal.PhpContext;
import com.druvu.web.php.internal.PhpProcessingException;
import com.druvu.web.php.internal.expr.PhpExpr;
import com.druvu.web.php.internal.expr.PhpFunctionRegistry;

/**
 * Built-in PHP functions: webjar(), context(), link().
 *
 * @author Deniss Larka
 */
public class BuiltInFunctions {

	private static final WebJarAssetLocator ASSET_LOCATOR = new WebJarAssetLocator();

	public static void registerAll(PhpFunctionRegistry registry) {
		registry.register("webjar", BuiltInFunctions::webjar);
		registry.register("context", BuiltInFunctions::context);
		registry.register("link", BuiltInFunctions::link);
	}

	private static String webjar(PhpContext ctx, List<PhpExpr> args) {
		requireArgs("webjar", args, 1);
		String path = args.getFirst().evaluate(ctx);
		String fullPath = ASSET_LOCATOR.getFullPath(path);
		// Strip META-INF/resources/ prefix, prepend a context path
		String relativePath = fullPath.substring("META-INF/resources/".length());
		return ctx.getRequest().getContextPath() + "/" + relativePath;
	}

	@SuppressWarnings("PMD.UnusedFormalParameter")
	private static String context(PhpContext ctx, List<PhpExpr> args) {
		return ctx.getRequest().getContextPath();
	}

	private static String link(PhpContext ctx, List<PhpExpr> args) {
		requireArgs("link", args, 1);
		String target = args.getFirst().evaluate(ctx);
		return ctx.getRequest().getContextPath() + "/" + target;
	}

	private static void requireArgs(String name, List<PhpExpr> args, int expected) {
		if (args.size() != expected) {
			throw new PhpProcessingException(name + "() expects " + expected + " argument(s), got " + args.size());
		}
	}
}
