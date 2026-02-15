package com.druvu.web.php.internal.expr;

import java.util.HashMap;
import java.util.Map;

import com.druvu.web.php.internal.PhpProcessingException;

/**
 * Registry holding named PHP functions.
 * Functions are registered at init time and looked up during expression evaluation.
 *
 * @author Deniss Larka
 */
public class PhpFunctionRegistry {

	private final Map<String, PhpFunction> functions = new HashMap<>();

	/**
	 * Registers a function with the given name.
	 *
	 * @param name     the function name
	 * @param function the function implementation
	 */
	public void register(String name, PhpFunction function) {
		functions.put(name, function);
	}

	/**
	 * Looks up a function by name.
	 *
	 * @param name the function name
	 * @return the function
	 * @throws PhpProcessingException if the function is not registered
	 */
	public PhpFunction get(String name) {
		PhpFunction fn = functions.get(name);
		if (fn == null) {
			throw new PhpProcessingException("Unknown function: " + name + "()");
		}
		return fn;
	}

	/**
	 * Checks if a function with the given name is registered.
	 *
	 * @param name the function name
	 * @return true if registered
	 */
	public boolean has(String name) {
		return functions.containsKey(name);
	}
}
