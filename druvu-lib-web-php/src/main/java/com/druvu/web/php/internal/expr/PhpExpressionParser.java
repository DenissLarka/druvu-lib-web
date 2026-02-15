package com.druvu.web.php.internal.expr;

import java.util.ArrayList;
import java.util.List;

import com.druvu.web.php.internal.PhpProcessingException;

/**
 * Parser for PHP expressions that builds an expression tree.
 * <p>
 * Currently supports:
 * - String literals: 'text', "text"
 * - String concatenation: expr . expr
 * - Function calls: name(), name('arg'), name('a', 'b')
 * - Escape sequences: \', \", \\, \n, \r, \t
 */
public class PhpExpressionParser {

	private final String expression;
	private int position;
	private final int length;

	public PhpExpressionParser(String expression) {
		this.expression = expression.trim();
		this.position = 0;
		this.length = this.expression.length();
	}

	/**
	 * Parses the expression and returns the root expression node.
	 *
	 * @return parsed expression tree
	 */
	public PhpExpr parse() {
		if (length == 0) {
			return new LiteralStringExpr("");
		}

		PhpExpr expr = parseConcat();

		// Ensure we consumed the entire expression
		skipWhitespace();
		if (position < length) {
			throw new PhpProcessingException(
					"Unexpected characters at position " + position + ": " + expression.substring(position)
			);
		}

		return expr;
	}

	/**
	 * Parses concatenation expressions (the lowest precedence).
	 * Example: 'Hello' . ' ' . 'World'
	 */
	private PhpExpr parseConcat() {
		PhpExpr left = parsePrimary();

		while (true) {
			skipWhitespace();
			if (position < length && expression.charAt(position) == '.') {
				position++; // Skip .
				skipWhitespace();
				PhpExpr right = parsePrimary();
				left = new ConcatExpr(left, right);
			} else {
				break;
			}
		}

		return left;
	}

	/**
	 * Parses primary expressions (literals, variables, etc.).
	 */
	private PhpExpr parsePrimary() {
		skipWhitespace();

		if (position >= length) {
			throw new PhpProcessingException("Unexpected end of expression");
		}

		char ch = expression.charAt(position);

		// String literal with single quotes
		if (ch == '\'') {
			return parseStringLiteral('\'');
		}

		// String literal with double quotes
		if (ch == '"') {
			return parseStringLiteral('"');
		}

		// Parenthesized expression: (expr)
		if (ch == '(') {
			position++; // Skip (
			PhpExpr expr = parseConcat();
			skipWhitespace();
			if (position >= length || expression.charAt(position) != ')') {
				throw new PhpProcessingException("Expected ')' at position " + position);
			}
			position++; // Skip )
			return expr;
		}

		// Identifier or function call
		if (Character.isLetter(ch) || ch == '_') {
			return parseIdentifierOrFunctionCall();
		}

		throw new PhpProcessingException(
				"Unsupported expression at position " + position + ": " + expression.substring(position)
		);
	}

	/**
	 * Parses an identifier. If followed by '(', treats it as a function call.
	 * Otherwise, throws (variables are not yet supported).
	 */
	private PhpExpr parseIdentifierOrFunctionCall() {
		int start = position;
		while (position < length && (Character.isLetterOrDigit(expression.charAt(position)) || expression.charAt(position) == '_')) {
			position++;
		}
		String name = expression.substring(start, position);

		skipWhitespace();

		if (position < length && expression.charAt(position) == '(') {
			position++; // Skip (
			List<PhpExpr> args = new ArrayList<>();
			skipWhitespace();

			if (position < length && expression.charAt(position) != ')') {
				args.add(parseConcat());
				skipWhitespace();
				while (position < length && expression.charAt(position) == ',') {
					position++; // Skip ,
					skipWhitespace();
					args.add(parseConcat());
					skipWhitespace();
				}
			}

			if (position >= length || expression.charAt(position) != ')') {
				throw new PhpProcessingException("Expected ')' after function arguments at position " + position);
			}
			position++; // Skip )

			return new FunctionCallExpr(name, args);
		}

		throw new PhpProcessingException("Variables not yet supported: " + name + " at position " + start);
	}

	/**
	 * Parses a string literal with the given quote character.
	 * Handles escape sequences like \', \", \\, \n, \r, \t
	 */
	private PhpExpr parseStringLiteral(char quote) {
		position++; // Skip opening quote

		StringBuilder value = new StringBuilder();

		while (position < length) {
			char ch = expression.charAt(position);

			if (ch == '\\' && position + 1 < length) {
				// Handle escape sequences
				position++;
				char next = expression.charAt(position);
				switch (next) {
					case 'n':
						value.append('\n');
						break;
					case 'r':
						value.append('\r');
						break;
					case 't':
						value.append('\t');
						break;
					case '\\':
						value.append('\\');
						break;
					case '\'':
						value.append('\'');
						break;
					case '"':
						value.append('"');
						break;
					default:
						// Unknown escape sequence - keep the backslash
						value.append('\\').append(next);
						break;
				}
				position++;
			} else if (ch == quote) {
				// End of string
				position++; // Skip closing quote
				return new LiteralStringExpr(value.toString());
			} else {
				value.append(ch);
				position++;
			}
		}

		throw new PhpProcessingException("Unclosed string literal starting with " + quote);
	}

	/**
	 * Skips whitespace at current position
	 */
	private void skipWhitespace() {
		while (position < length && Character.isWhitespace(expression.charAt(position))) {
			position++;
		}
	}
}
