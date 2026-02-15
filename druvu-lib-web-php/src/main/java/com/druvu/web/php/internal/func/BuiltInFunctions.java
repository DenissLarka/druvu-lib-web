package com.druvu.web.php.internal.func;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;

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

	private static final String WEBJARS_PREFIX = "META-INF/resources/webjars/";
	private static final String RESOURCES_PREFIX = "META-INF/resources/";

	/** Maps filename (e.g. "w2ui-2.0.min.css") to relative path (e.g. "webjars/w2ui/2.0.0/w2ui-2.0.min.css"). */
	private static final Map<String, String> WEBJAR_INDEX = buildWebJarIndex();

	public static void registerAll(PhpFunctionRegistry registry) {
		registry.register("webjar", BuiltInFunctions::webjar);
		registry.register("context", BuiltInFunctions::context);
		registry.register("link", BuiltInFunctions::link);
	}

	private static String webjar(PhpContext ctx, List<PhpExpr> args) {
		requireArgs("webjar", args, 1);
		String path = args.getFirst().evaluate(ctx);
		String resolvedPath = WEBJAR_INDEX.get(path);
		if (resolvedPath == null) {
			throw new PhpProcessingException("WebJar resource not found: " + path);
		}
		return ctx.getRequest().getContextPath() + "/" + resolvedPath;
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

	private static Map<String, String> buildWebJarIndex() {
		Map<String, String> index = new HashMap<>();
		try {
			ClassLoader cl = BuiltInFunctions.class.getClassLoader();
			Enumeration<URL> urls = cl.getResources("META-INF/resources/webjars");
			while (urls.hasMoreElements()) {
				URL url = urls.nextElement();
				if ("jar".equals(url.getProtocol())) {
					String urlStr = url.toExternalForm();
					String jarPath = urlStr.substring("jar:file:".length(), urlStr.indexOf("!/"));
					scanJar(jarPath, index);
				}
			}
		} catch (IOException ignored) {
			// webjar() calls will fail individually with a clear error
		}
		return index;
	}

	private static void scanJar(String jarPath, Map<String, String> index) throws IOException {
		try (JarFile jar = new JarFile(jarPath)) {
			jar.stream()
				.filter(e -> !e.isDirectory() && e.getName().startsWith(WEBJARS_PREFIX))
				.forEach(e -> {
					String name = e.getName();
					String relativePath = name.substring(RESOURCES_PREFIX.length());
					String fileName = name.substring(name.lastIndexOf('/') + 1);
					index.putIfAbsent(fileName, relativePath);
				});
		}
	}
}
