package com.druvu.web.php.internal;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import com.druvu.web.php.internal.expr.PhpExpr;
import com.druvu.web.php.internal.expr.PhpExpressionParser;

/**
 * PHP processor that uses a tokenization approach.
 * <p>
 * This implementation:
 * - Uses PhpTokenizer for parsing
 * - Uses PhpExpr for evaluation
 * - Supports multiple PHP tag types: <?php echo ... ?> and <?= ... ?>
 * - Supports include/require/include_once/require_once statements
 */
public class TokenBasedPhpProcessor {

	/**
	 * Processes PHP content by tokenizing and evaluating PHP tags.
	 *
	 * @param content the PHP/HTML content to process
	 * @param context the PHP execution context
	 * @return processed content with PHP tags replaced by their output
	 * @throws PhpProcessingException if processing fails (unchecked)
	 */
	public String process(String content, PhpContext context) {
		PhpTokenizer tokenizer = new PhpTokenizer(content);
		List<PhpToken> tokens = tokenizer.tokenize();

		StringBuilder result = new StringBuilder();

		for (PhpToken token : tokens) {
			switch (token.getType()) {
				case TEXT:
					// Plain text - output as-is
					result.append(token.getContent());
					break;

				case PHP_ECHO:
				case PHP_ECHO_SHORT:
					// Echo tag - evaluate expression and append a result
					String evaluated = evaluateExpression(token.getContent(), context);
					result.append(evaluated);
					break;

				case PHP_INCLUDE:
				case PHP_REQUIRE:
				case PHP_INCLUDE_ONCE:
				case PHP_REQUIRE_ONCE:
					// Include/require - load and process included file
					String included = processInclude(token, context);
					result.append(included);
					break;

				case PHP_CODE:
					// Generic PHP code - not implemented yet
					throw new PhpProcessingException(
							"Generic PHP code blocks are not yet supported at position " +
									token.getStartPosition()
					);

				default:
					throw new PhpProcessingException(
							"Unknown token type: " + token.getType()
					);
			}
		}

		return result.toString();
	}

	/**
	 * Processes an include or require token by loading and recursively processing the included file.
	 *
	 * @param token   the include/require token
	 * @param context the PHP execution context
	 * @return the processed content of the included file
	 * @throws PhpProcessingException if require fails to load (unchecked)
	 */
	private String processInclude(PhpToken token, PhpContext context) {
		boolean isRequire = token.getType() == PhpToken.Type.PHP_REQUIRE
				|| token.getType() == PhpToken.Type.PHP_REQUIRE_ONCE;
		boolean isOnce = token.getType() == PhpToken.Type.PHP_INCLUDE_ONCE
				|| token.getType() == PhpToken.Type.PHP_REQUIRE_ONCE;

		// Evaluate the path expression
		String includePath = evaluateExpression(token.getContent(), context);

		// Resolve relative to current file's directory
		String resolvedPath = resolvePath(includePath, context.getCurrentPath());

		// Check once semantics - skip if already included
		if (isOnce && context.getIncludedFiles().contains(resolvedPath)) {
			return "";
		}

		// Load the included file
		String fileContent;
		try {
			fileContent = context.getResourceLoader().load(resolvedPath);
		} catch (IOException e) {
			if (isRequire) {
				throw new PhpProcessingException(
						"require: Failed to open required '" + includePath + "' (resolved: " + resolvedPath + ")", e);
			}
			return "";
		}

		if (fileContent == null) {
			if (isRequire) {
				throw new PhpProcessingException(
						"require: Failed to open required '" + includePath + "' (resolved: " + resolvedPath + ")");
			}
			return "";
		}

		// Mark as included (for once semantics)
		context.getIncludedFiles().add(resolvedPath);

		// Recursively process the included file
		PhpContext includeContext = context.forInclude(resolvedPath);
		return process(fileContent, includeContext);
	}

	/**
	 * Resolves an include path relative to the current file's directory.
	 * Absolute paths (starting with /) are used as-is.
	 * Relative paths are resolved against the directory of the current file.
	 *
	 * @param includePath the path from the include/require statement
	 * @param currentPath the path of the file containing the include/require
	 * @return the resolved absolute path
	 */
	private String resolvePath(String includePath, String currentPath) {
		if (includePath.startsWith("/")) {
			return normalizePath(includePath);
		}

		// Resolve relative to current file's directory
		String currentDir = "/";
		if (currentPath != null) {
			int lastSlash = currentPath.lastIndexOf('/');
			if (lastSlash >= 0) {
				currentDir = currentPath.substring(0, lastSlash + 1);
			}
		}

		return normalizePath(currentDir + includePath);
	}

	/**
	 * Normalizes a path by resolving . and .. segments.
	 */
	private String normalizePath(String path) {
		String normalized = Path.of(path).normalize().toString();
		// Ensure forward slashes (for Windows compatibility)
		normalized = normalized.replace('\\', '/');
		if (!normalized.startsWith("/")) {
			normalized = "/" + normalized;
		}
		return normalized;
	}

	/**
	 * Evaluates a PHP expression using the expression parser.
	 *
	 * @param expression the PHP expression to evaluate
	 * @param context    the PHP execution context
	 * @return the evaluated result as a string
	 * @throws PhpProcessingException if expression evaluation fails (unchecked)
	 */
	private String evaluateExpression(String expression, PhpContext context) {
		PhpExpressionParser parser = new PhpExpressionParser(expression);
		PhpExpr expr = parser.parse();
		return expr.evaluate(context);
	}
}
