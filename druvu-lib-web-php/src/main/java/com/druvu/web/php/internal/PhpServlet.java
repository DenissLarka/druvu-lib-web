package com.druvu.web.php.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;

import com.druvu.web.php.internal.expr.PhpFunctionRegistry;
import com.druvu.web.php.internal.func.BuiltInFunctions;

/**
 * PHP servlet that processes PHP files using a tokenization approach.
 *
 * <p>
 * Supports:
 * - Standard echo tags: {@code <?php echo ... ?>}
 * - Short echo tags: {@code <?= ... ?>}
 * - Expression evaluation with string literals and concatenation
 *
 * @author Deniss Larka
 */
public class PhpServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private final TokenBasedPhpProcessor processor;
	private final PhpFunctionRegistry functionRegistry;

	/**
	 * Creates a new PhpServlet with the default token-based processor and built-in functions.
	 */
	public PhpServlet() {
		this(new TokenBasedPhpProcessor());
	}

	/**
	 * Creates a new PhpServlet with a custom processor.
	 * This constructor allows for dependency injection and custom processing logic.
	 *
	 * @param processor the PHP processor to use
	 */
	public PhpServlet(TokenBasedPhpProcessor processor) {
		this.processor = processor;
		this.functionRegistry = new PhpFunctionRegistry();
		BuiltInFunctions.registerAll(functionRegistry);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		processPhp(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
		processPhp(req, resp);
	}

	@SneakyThrows
	private void processPhp(HttpServletRequest req, HttpServletResponse resp) {
		String path = req.getPathInfo();
		if (path == null || path.isEmpty()) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		// Load PHP file content from webapp resources
		String content = loadPhpFile(req, path);
		if (content == null) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND, "PHP file not found: " + path);
			return;
		}

		try {
			// Create resource loader from servlet context
			PhpContext.ResourceLoader resourceLoader = p -> loadPhpFile(req, p);

			// Create context for processors
			PhpContext context = new PhpContext(req, resp, path, resourceLoader, functionRegistry);

			// Process content using a token-based processor
			String processedContent = processor.process(content, context);

			resp.setContentType("text/html");
			resp.setCharacterEncoding("UTF-8");

			try (PrintWriter writer = resp.getWriter()) {
				writer.write(processedContent);
			}
		} catch (PhpProcessingException e) {
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error processing PHP: " + e.getMessage());
		}
	}

	/**
	 * Loads PHP file content from the webapp resources.
	 */
	private String loadPhpFile(HttpServletRequest req, String path) throws IOException {
		InputStream is = req.getServletContext().getResourceAsStream(path);
		if (is == null) {
			return null;
		}
		try (is) {
			return new String(is.readAllBytes(), StandardCharsets.UTF_8);
		}
	}
}
