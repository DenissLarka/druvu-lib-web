package com.druvu.web.php.internal;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.druvu.web.php.internal.expr.PhpFunctionRegistry;

/**
 * Context object that holds request and response objects for PHP processing.
 * This allows tag processors to access servlet context when needed.
 *
 * @author Deniss Larka
 */
public class PhpContext {

	/**
	 * Loads a resource file by path.
	 */
	@FunctionalInterface
	public interface ResourceLoader {

		/**
		 * Loads the content of a resource at the given path.
		 *
		 * @param path the resource path
		 * @return the file content, or null if not found
		 * @throws IOException if an I/O error occurs
		 */
		String load(String path) throws IOException;
	}

	private static final int MAX_INCLUDE_DEPTH = 100;

	private final HttpServletRequest request;
	private final HttpServletResponse response;
	private final String currentPath;
	private final Set<String> includedFiles;
	private final ResourceLoader resourceLoader;
	private final PhpFunctionRegistry functionRegistry;
	private final int includeDepth;

	/**
	 * Creates a new PhpContext for the top-level PHP file being processed.
	 *
	 * @param request          the HTTP request
	 * @param response         the HTTP response
	 * @param currentPath      the path of the PHP file being processed
	 * @param resourceLoader   the loader for resolving included files
	 * @param functionRegistry the registry of available PHP functions
	 */
	public PhpContext(HttpServletRequest request, HttpServletResponse response,
					  String currentPath, ResourceLoader resourceLoader,
					  PhpFunctionRegistry functionRegistry) {
		this.request = request;
		this.response = response;
		this.currentPath = currentPath;
		this.includedFiles = new HashSet<>();
		this.resourceLoader = resourceLoader;
		this.functionRegistry = functionRegistry;
		this.includeDepth = 0;
	}

	private PhpContext(HttpServletRequest request, HttpServletResponse response,
					   String currentPath, Set<String> includedFiles,
					   ResourceLoader resourceLoader, PhpFunctionRegistry functionRegistry,
					   int includeDepth) {
		this.request = request;
		this.response = response;
		this.currentPath = currentPath;
		this.includedFiles = includedFiles;
		this.resourceLoader = resourceLoader;
		this.functionRegistry = functionRegistry;
		this.includeDepth = includeDepth;
	}

	/**
	 * Creates a derived context for processing an included file.
	 * Shares the same includedFiles set and resourceLoader, but tracks
	 * a new currentPath and incremented include depth.
	 *
	 * @param includedPath the path of the included file
	 * @return a new context for the included file
	 * @throws PhpProcessingException if the maximum include depth is exceeded
	 */
	public PhpContext forInclude(String includedPath) {
		int newDepth = includeDepth + 1;
		if (newDepth > MAX_INCLUDE_DEPTH) {
			throw new PhpProcessingException(
					"Maximum include depth exceeded (" + MAX_INCLUDE_DEPTH + ")");
		}
		return new PhpContext(request, response, includedPath, includedFiles, resourceLoader, functionRegistry, newDepth);
	}

	public HttpServletRequest getRequest() {
		return request;
	}

	public HttpServletResponse getResponse() {
		return response;
	}

	public String getCurrentPath() {
		return currentPath;
	}

	public Set<String> getIncludedFiles() {
		return includedFiles;
	}

	public ResourceLoader getResourceLoader() {
		return resourceLoader;
	}

	public PhpFunctionRegistry getFunctionRegistry() {
		return functionRegistry;
	}

	public int getIncludeDepth() {
		return includeDepth;
	}
}
