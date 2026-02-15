package com.druvu.web.php.internal;

/**
 * Represents a token in PHP content.
 * Tokens can be plain text, PHP echo tags, or PHP code blocks.
 */
public class PhpToken {

	/**
	 * Type of PHP token
	 */
	public enum Type {
		/**
		 * Plain text content (HTML, etc.)
		 */
		TEXT,

		/**
		 * PHP echo tag: <?php echo ... ?>
		 */
		PHP_ECHO,

		/**
		 * Short echo tag: <?= ... ?>
		 */
		PHP_ECHO_SHORT,

		/**
		 * Generic PHP code block: <?php ... ?>
		 */
		PHP_CODE,

		/**
		 * PHP include statement: {@code <?php include 'file.php'; ?>}
		 */
		PHP_INCLUDE,

		/**
		 * PHP require statement: {@code <?php require 'file.php'; ?>}
		 */
		PHP_REQUIRE,

		/**
		 * PHP include_once statement: {@code <?php include_once 'file.php'; ?>}
		 */
		PHP_INCLUDE_ONCE,

		/**
		 * PHP require_once statement: {@code <?php require_once 'file.php'; ?>}
		 */
		PHP_REQUIRE_ONCE
	}

	private final Type type;
	private final String content;
	private final int startPosition;
	private final int endPosition;

	public PhpToken(Type type, String content, int startPosition, int endPosition) {
		this.type = type;
		this.content = content;
		this.startPosition = startPosition;
		this.endPosition = endPosition;
	}

	public Type getType() {
		return type;
	}

	public String getContent() {
		return content;
	}

	public int getStartPosition() {
		return startPosition;
	}

	public int getEndPosition() {
		return endPosition;
	}

	@Override
	public String toString() {
		return "PhpToken{type=" + type + ", content='" + content + "', pos=" + startPosition + "-" + endPosition + '}';
	}
}
