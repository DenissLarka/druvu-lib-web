package com.druvu.web.php.internal;

import java.util.ArrayList;
import java.util.List;

/**
 * Tokenizer for PHP content.
 * Parses content character-by-character to identify PHP tags and text content.
 */
public class PhpTokenizer {

	private final String content;
	private int position;
	private final int length;

	public PhpTokenizer(String content) {
		this.content = content;
		this.position = 0;
		this.length = content.length();
	}

	/**
	 * Tokenizes the entire content into a list of tokens.
	 *
	 * @return list of tokens representing the PHP content
	 */
	public List<PhpToken> tokenize() {
		List<PhpToken> tokens = new ArrayList<>();

		while (position < length) {
			PhpToken token = nextToken();
			if (token != null) {
				tokens.add(token);
			}
		}

		return tokens;
	}

	/**
	 * Reads the next token from the content.
	 *
	 * @return next token or null if the end of content
	 */
	private PhpToken nextToken() {
		if (position >= length) {
			return null;
		}

		StringBuilder text = new StringBuilder();
		int startPos = position;

		while (position < length) {
			char ch = content.charAt(position);

			if (ch == '<') {
				// Lookahead to check for PHP tags
				if (position + 1 < length) {
					char next = content.charAt(position + 1);

					// Check for <?= (short echo tag)
					if (next == '?' && position + 2 < length && content.charAt(position + 2) == '=') {
						// Return accumulated text first
						if (text.length() > 0) {
							return new PhpToken(PhpToken.Type.TEXT, text.toString(), startPos, position);
						}
						// Parse short echo tag
						return parseShortEchoTag();
					}

					// Check for <?php (full open tag)
					if (next == '?' && position + 5 < length
							&& (content.charAt(position + 2) == 'p' || content.charAt(position + 2) == 'P')
							&& (content.charAt(position + 3) == 'h' || content.charAt(position + 3) == 'H')
							&& (content.charAt(position + 4) == 'p' || content.charAt(position + 4) == 'P')
							&& (Character.isWhitespace(content.charAt(position + 5)) || content.charAt(position + 5) == '?')) {

						// Return accumulated text first
						if (text.length() > 0) {
							return new PhpToken(PhpToken.Type.TEXT, text.toString(), startPos, position);
						}
						// Parse PHP tag
						return parsePhpTag();
					}
				}

				// Not a PHP tag, append and continue
				text.append(ch);
				position++;
			} else {
				text.append(ch);
				position++;
			}
		}

		// Return the remaining text
		if (text.length() > 0) {
			return new PhpToken(PhpToken.Type.TEXT, text.toString(), startPos, position);
		}

		return null;
	}

	/**
	 * Parses a short echo tag: <?= expression ?>
	 */
	private PhpToken parseShortEchoTag() {
		int startPos = position;
		position += 3; // Skip <?=

		// Skip whitespace
		skipWhitespace();

		// Find closing ?>
		int contentStart = position;
		while (position < length - 1) {
			if (content.charAt(position) == '?' && content.charAt(position + 1) == '>') {
				String expression = content.substring(contentStart, position).trim();
				position += 2; // Skip ?>
				return new PhpToken(PhpToken.Type.PHP_ECHO_SHORT, expression, startPos, position);
			}
			position++;
		}

		throw new PhpProcessingException("Unclosed short echo tag at position " + startPos);
	}

	/**
	 * Parses a PHP tag: <?php ... ?>
	 * Determines if it's an echo, include/require, or generic PHP code
	 */
	private PhpToken parsePhpTag() {
		int startPos = position;
		position += 5; // Skip <?php

		// Skip whitespace
		skipWhitespace();

		// Check if it's an echo statement
		if (matchKeyword("echo")) {
			position += 4; // Skip "echo"
			return parseStatementExpression(startPos, PhpToken.Type.PHP_ECHO);
		}

		// Check include_once before include (longer match first)
		if (matchKeyword("include_once")) {
			position += 12;
			return parseStatementExpression(startPos, PhpToken.Type.PHP_INCLUDE_ONCE);
		}

		// Check include
		if (matchKeyword("include")) {
			position += 7;
			return parseStatementExpression(startPos, PhpToken.Type.PHP_INCLUDE);
		}

		// Check require_once before require (longer match first)
		if (matchKeyword("require_once")) {
			position += 12;
			return parseStatementExpression(startPos, PhpToken.Type.PHP_REQUIRE_ONCE);
		}

		// Check require
		if (matchKeyword("require")) {
			position += 7;
			return parseStatementExpression(startPos, PhpToken.Type.PHP_REQUIRE);
		}

		// Generic PHP code (not implemented yet, but tokenized for future use)
		int contentStart = position;
		while (position < length - 1) {
			if (content.charAt(position) == '?' && content.charAt(position + 1) == '>') {
				String code = content.substring(contentStart, position).trim();
				position += 2; // Skip ?>
				return new PhpToken(PhpToken.Type.PHP_CODE, code, startPos, position);
			}
			position++;
		}

		throw new PhpProcessingException("Unclosed PHP tag at position " + startPos);
	}

	/**
	 * Checks if the content at the current position matches a keyword,
	 * followed by a non-identifier character (whitespace, parenthesis, quote).
	 */
	private boolean matchKeyword(String keyword) {
		int len = keyword.length();
		if (position + len > length) {
			return false;
		}
		if (!content.substring(position, position + len).equalsIgnoreCase(keyword)) {
			return false;
		}
		if (position + len >= length) {
			return true;
		}
		char next = content.charAt(position + len);
		return Character.isWhitespace(next) || next == '(' || next == '\'' || next == '"';
	}

	/**
	 * Parses the expression part of a PHP statement (echo, include, require).
	 * Extracts everything between the keyword and the closing ?>, stripping
	 * trailing semicolons and optional outer parentheses.
	 */
	private PhpToken parseStatementExpression(int startPos, PhpToken.Type type) {
		skipWhitespace();

		int contentStart = position;
		while (position < length - 1) {
			if (content.charAt(position) == '?' && content.charAt(position + 1) == '>') {
				String expression = content.substring(contentStart, position).trim();
				// Remove trailing semicolon if present
				if (expression.endsWith(";")) {
					expression = expression.substring(0, expression.length() - 1).trim();
				}
				position += 2; // Skip ?>
				return new PhpToken(type, expression, startPos, position);
			}
			position++;
		}

		throw new PhpProcessingException("Unclosed PHP tag at position " + startPos);
	}

	/**
	 * Skips whitespace characters at the current position
	 */
	private void skipWhitespace() {
		while (position < length && Character.isWhitespace(content.charAt(position))) {
			position++;
		}
	}
}
