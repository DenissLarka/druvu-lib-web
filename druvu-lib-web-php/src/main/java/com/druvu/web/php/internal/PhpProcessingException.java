package com.druvu.web.php.internal;

/**
 * Runtime exception thrown when PHP processing fails.
 * Changed to RuntimeException to avoid checked exception handling throughout the processing pipeline.
 *
 * @author Deniss Larka
 */
public class PhpProcessingException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public PhpProcessingException(String message) {
		super(message);
	}

	public PhpProcessingException(String message, Throwable cause) {
		super(message, cause);
	}
}
