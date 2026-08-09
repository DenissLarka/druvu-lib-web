package com.druvu.web.php.internal.parse;

import com.druvu.web.php.internal.Location;
import com.druvu.web.php.internal.PhpEngineConfig;
import com.druvu.web.php.internal.PhpSyntaxException;
import com.druvu.web.php.internal.ast.PhpExpression;
import com.druvu.web.php.internal.ast.PhpStatement;
import com.druvu.web.php.internal.ast.PhpTemplate;
import com.druvu.web.php.internal.ast.expr.ArrayAccessExpression;
import com.druvu.web.php.internal.ast.expr.ArrayLiteralExpression;
import com.druvu.web.php.internal.ast.expr.ArrowFunctionExpression;
import com.druvu.web.php.internal.ast.expr.BinaryExpression;
import com.druvu.web.php.internal.ast.expr.BinaryOperator;
import com.druvu.web.php.internal.ast.expr.CastExpression;
import com.druvu.web.php.internal.ast.expr.ClosureCallExpression;
import com.druvu.web.php.internal.ast.expr.CoalesceExpression;
import com.druvu.web.php.internal.ast.expr.EmptyExpression;
import com.druvu.web.php.internal.ast.expr.FunctionCallExpression;
import com.druvu.web.php.internal.ast.expr.IncludeExpression;
import com.druvu.web.php.internal.ast.expr.IncrementExpression;
import com.druvu.web.php.internal.ast.expr.IssetExpression;
import com.druvu.web.php.internal.ast.expr.LiteralExpression;
import com.druvu.web.php.internal.ast.expr.LogicalExpression;
import com.druvu.web.php.internal.ast.expr.MatchExpression;
import com.druvu.web.php.internal.ast.expr.PrintExpression;
import com.druvu.web.php.internal.ast.expr.PropertyExpression;
import com.druvu.web.php.internal.ast.expr.TernaryExpression;
import com.druvu.web.php.internal.ast.expr.UnaryExpression;
import com.druvu.web.php.internal.ast.expr.VariableExpression;
import com.druvu.web.php.internal.ast.stmt.BlockStatement;
import com.druvu.web.php.internal.ast.stmt.BreakStatement;
import com.druvu.web.php.internal.ast.stmt.ContinueStatement;
import com.druvu.web.php.internal.ast.stmt.DoWhileStatement;
import com.druvu.web.php.internal.ast.stmt.EchoStatement;
import com.druvu.web.php.internal.ast.stmt.ExpressionStatement;
import com.druvu.web.php.internal.ast.stmt.ForStatement;
import com.druvu.web.php.internal.ast.stmt.ForeachStatement;
import com.druvu.web.php.internal.ast.stmt.IfStatement;
import com.druvu.web.php.internal.ast.stmt.ReturnStatement;
import com.druvu.web.php.internal.ast.stmt.SwitchStatement;
import com.druvu.web.php.internal.ast.stmt.TextStatement;
import com.druvu.web.php.internal.ast.stmt.UnsetStatement;
import com.druvu.web.php.internal.ast.stmt.WhileStatement;
import com.druvu.web.php.internal.builtin.Constants;
import com.druvu.web.php.internal.lex.PhpLexer;
import com.druvu.web.php.internal.lex.Symbol;
import com.druvu.web.php.internal.lex.Token;
import com.druvu.web.php.internal.lex.TokenType;
import com.druvu.web.php.internal.value.PhpBool;
import com.druvu.web.php.internal.value.PhpNull;
import com.druvu.web.php.internal.value.PhpString;
import com.druvu.web.php.internal.value.PhpValue;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Turns tokens into a tree.
 *
 * <p>Recursive descent, one method per rung of PHP 8's precedence ladder, written from loosest to tightest so the file
 * reads in the same order as the language's own table. That shape is the point: precedence is not encoded in a table
 * somewhere else, it <em>is</em> the call order, and a rung can be checked by reading one method.
 *
 * <p>PHP 8 rather than PHP 7 shows up in two places worth naming. Concatenation now binds looser than {@code +} and
 * {@code -}, so {@code "x" . 1 + 2} is {@code "x3"}. And comparison and the ternary are non-associative: {@code 1 < 2 <
 * 3} and an unparenthesised {@code a ? b : c ? d : e} are refused here exactly as PHP refuses them, rather than quietly
 * picking an association the author did not mean.
 *
 * @author Deniss Larka
 */
public final class PhpParser {

    private static final Map<Symbol, BinaryOperator> EQUALITY = Map.of(
            Symbol.EQUAL, BinaryOperator.EQUAL,
            Symbol.NOT_EQUAL, BinaryOperator.NOT_EQUAL,
            Symbol.NOT_EQUAL_ALT, BinaryOperator.NOT_EQUAL,
            Symbol.IDENTICAL, BinaryOperator.IDENTICAL,
            Symbol.NOT_IDENTICAL, BinaryOperator.NOT_IDENTICAL,
            Symbol.SPACESHIP, BinaryOperator.SPACESHIP);

    private static final Map<Symbol, BinaryOperator> RELATIONAL = Map.of(
            Symbol.LESS, BinaryOperator.LESS,
            Symbol.LESS_OR_EQUAL, BinaryOperator.LESS_OR_EQUAL,
            Symbol.GREATER, BinaryOperator.GREATER,
            Symbol.GREATER_OR_EQUAL, BinaryOperator.GREATER_OR_EQUAL);

    /** The compound assignments, each paired with the operator it stands for. {@code ??=} is not here: see below. */
    private static final Map<Symbol, BinaryOperator> COMPOUND_ASSIGNMENT = Map.of(
            Symbol.PLUS_ASSIGN, BinaryOperator.ADD,
            Symbol.MINUS_ASSIGN, BinaryOperator.SUBTRACT,
            Symbol.TIMES_ASSIGN, BinaryOperator.MULTIPLY,
            Symbol.DIVIDE_ASSIGN, BinaryOperator.DIVIDE,
            Symbol.MODULO_ASSIGN, BinaryOperator.MODULO,
            Symbol.CONCAT_ASSIGN, BinaryOperator.CONCAT);

    /**
     * The words that close a block written in the colon form, plus the two that open the next arm of one. A statement
     * list stops at any of them and lets whichever construct owns it consume it.
     */
    private static final Set<String> BLOCK_ENDERS =
            Set.of("endif", "else", "elseif", "endforeach", "endfor", "endwhile", "endswitch", "case", "default");

    private final List<Token> tokens;

    private int at;

    public PhpParser(List<Token> tokens) {
        this.tokens = List.copyOf(tokens);
    }

    /** One expression, starting at the current token. */
    public PhpExpression parseExpression() {
        return wordOr();
    }

    public boolean atEnd() {
        return peek().is(TokenType.EOF);
    }

    public Token peek() {
        return tokens.get(Math.min(at, tokens.size() - 1));
    }

    // ------------------------------------------------------------ the ladder

    private PhpExpression wordOr() {
        PhpExpression left = wordXor();
        while (peek().isIdentifier("or")) {
            Location start = advance().location();
            left = new LogicalExpression(start, LogicalExpression.Operator.OR, left, wordXor());
        }
        return left;
    }

    private PhpExpression wordXor() {
        PhpExpression left = wordAnd();
        while (peek().isIdentifier("xor")) {
            Location start = advance().location();
            left = new LogicalExpression(start, LogicalExpression.Operator.XOR, left, wordAnd());
        }
        return left;
    }

    private PhpExpression wordAnd() {
        PhpExpression left = assignment();
        while (peek().isIdentifier("and")) {
            Location start = advance().location();
            left = new LogicalExpression(start, LogicalExpression.Operator.AND, left, assignment());
        }
        return left;
    }

    /**
     * Assignment, which binds looser than the ternary above it — so {@code $a = $b ? 1 : 2} assigns the whole
     * conditional — and is right-associative, so {@code $a = $b = 1} works.
     */
    private PhpExpression assignment() {
        PhpExpression left = ternary();
        Token operator = peek();

        if (operator.is(Symbol.ASSIGN)) {
            advance();
            return store(left, operator, assignment());
        }
        if (operator.is(Symbol.COALESCE_ASSIGN)) {
            advance();
            // $a ??= $b keeps whatever $a already holds, which is exactly what $a = $a ?? $b does.
            return store(left, operator, new CoalesceExpression(operator.location(), left, assignment()));
        }
        BinaryOperator compound = lookUp(COMPOUND_ASSIGNMENT);
        if (compound != null) {
            advance();
            return store(left, operator, new BinaryExpression(operator.location(), compound, left, assignment()));
        }
        return left;
    }

    private PhpExpression store(PhpExpression target, Token operator, PhpExpression value) {
        PhpExpression assignment = target.toAssignment(value);
        if (assignment == null) {
            throw new PhpSyntaxException(operator.location(), "Cannot assign to this expression");
        }
        return assignment;
    }

    private PhpExpression ternary() {
        PhpExpression condition = coalesce();

        if (peek().is(Symbol.ELVIS)) {
            Location start = advance().location();
            PhpExpression otherwise = coalesce();
            refuseChainedTernary();
            return new TernaryExpression(start, condition, null, otherwise);
        }
        if (peek().is(Symbol.QUESTION)) {
            Location start = advance().location();
            PhpExpression whenTrue = parseExpression();
            expect(Symbol.COLON, "':' to finish the conditional");
            PhpExpression whenFalse = coalesce();
            refuseChainedTernary();
            return new TernaryExpression(start, condition, whenTrue, whenFalse);
        }
        return condition;
    }

    private void refuseChainedTernary() {
        if (peek().is(Symbol.QUESTION) || peek().is(Symbol.ELVIS)) {
            throw new PhpSyntaxException(
                    peek().location(),
                    "An unparenthesised 'a ? b : c ? d : e' has no defined meaning; parenthesise the half you mean");
        }
    }

    private PhpExpression coalesce() {
        PhpExpression left = symbolicOr();
        if (peek().is(Symbol.COALESCE)) {
            Location start = advance().location();
            return new CoalesceExpression(start, left, coalesce());
        }
        return left;
    }

    private PhpExpression symbolicOr() {
        PhpExpression left = symbolicAnd();
        while (peek().is(Symbol.OR)) {
            Location start = advance().location();
            left = new LogicalExpression(start, LogicalExpression.Operator.OR, left, symbolicAnd());
        }
        return left;
    }

    private PhpExpression symbolicAnd() {
        PhpExpression left = equality();
        while (peek().is(Symbol.AND)) {
            Location start = advance().location();
            left = new LogicalExpression(start, LogicalExpression.Operator.AND, left, equality());
        }
        return left;
    }

    private PhpExpression equality() {
        PhpExpression left = relational();
        BinaryOperator operator = lookUp(EQUALITY);
        if (operator == null) {
            return left;
        }
        Location start = advance().location();
        PhpExpression right = relational();
        refuseChain(EQUALITY, "equality");
        return new BinaryExpression(start, operator, left, right);
    }

    private PhpExpression relational() {
        PhpExpression left = concatenation();
        BinaryOperator operator = lookUp(RELATIONAL);
        if (operator == null) {
            return left;
        }
        Location start = advance().location();
        PhpExpression right = concatenation();
        refuseChain(RELATIONAL, "comparison");
        return new BinaryExpression(start, operator, left, right);
    }

    private void refuseChain(Map<Symbol, BinaryOperator> operators, String family) {
        if (lookUp(operators) != null) {
            throw new PhpSyntaxException(
                    peek().location(), "The " + family + " operators do not chain; parenthesise what you mean");
        }
    }

    /** Concatenation sits below {@code +} and {@code -} — the PHP 8 change that makes {@code "x" . 1 + 2} be "x3". */
    private PhpExpression concatenation() {
        PhpExpression left = additive();
        while (peek().is(Symbol.CONCAT)) {
            Location start = advance().location();
            left = new BinaryExpression(start, BinaryOperator.CONCAT, left, additive());
        }
        return left;
    }

    private PhpExpression additive() {
        PhpExpression left = multiplicative();
        while (peek().is(Symbol.PLUS) || peek().is(Symbol.MINUS)) {
            BinaryOperator operator = peek().is(Symbol.PLUS) ? BinaryOperator.ADD : BinaryOperator.SUBTRACT;
            Location start = advance().location();
            left = new BinaryExpression(start, operator, left, multiplicative());
        }
        return left;
    }

    private PhpExpression multiplicative() {
        PhpExpression left = unary();
        while (peek().is(Symbol.TIMES) || peek().is(Symbol.DIVIDE) || peek().is(Symbol.MODULO)) {
            BinaryOperator operator = peek().is(Symbol.TIMES)
                    ? BinaryOperator.MULTIPLY
                    : peek().is(Symbol.DIVIDE) ? BinaryOperator.DIVIDE : BinaryOperator.MODULO;
            Location start = advance().location();
            left = new BinaryExpression(start, operator, left, unary());
        }
        return left;
    }

    private PhpExpression unary() {
        Token token = peek();
        if (token.is(Symbol.NOT)) {
            advance();
            return new UnaryExpression(token.location(), UnaryExpression.Operator.NOT, unary());
        }
        if (token.is(Symbol.MINUS)) {
            advance();
            return new UnaryExpression(token.location(), UnaryExpression.Operator.NEGATE, unary());
        }
        if (token.is(Symbol.PLUS)) {
            advance();
            return new UnaryExpression(token.location(), UnaryExpression.Operator.IDENTITY, unary());
        }
        if (token.is(Symbol.INCREMENT) || token.is(Symbol.DECREMENT)) {
            advance();
            return new IncrementExpression(token.location(), unary(), token.is(Symbol.INCREMENT), true);
        }
        CastExpression.Target cast = castAhead();
        if (cast != null) {
            advance();
            advance();
            advance();
            return new CastExpression(token.location(), cast, unary());
        }
        return power();
    }

    /** {@code (int)} and friends: a parenthesised type name, distinguished from an ordinary bracketed expression. */
    private CastExpression.Target castAhead() {
        if (!peek().is(Symbol.LEFT_PAREN)
                || !peekAt(1).is(TokenType.IDENTIFIER)
                || !peekAt(2).is(Symbol.RIGHT_PAREN)) {
            return null;
        }
        return CastExpression.Target.named(peekAt(1).text());
    }

    /**
     * {@code **} is right-associative and binds tighter than unary minus, which is why {@code -2 ** 2} is −4: the power
     * is taken first and the sign applied to the result.
     */
    private PhpExpression power() {
        PhpExpression base = postfix();
        if (peek().is(Symbol.POWER)) {
            Location start = advance().location();
            return new BinaryExpression(start, BinaryOperator.POWER, base, unary());
        }
        return base;
    }

    private PhpExpression postfix() {
        PhpExpression expression = primary();
        while (true) {
            Token token = peek();
            if (token.is(Symbol.LEFT_BRACKET)) {
                advance();
                PhpExpression index = peek().is(Symbol.RIGHT_BRACKET) ? null : parseExpression();
                expect(Symbol.RIGHT_BRACKET, "']'");
                expression = new ArrayAccessExpression(token.location(), expression, index);
            } else if (token.is(Symbol.LEFT_PAREN)) {
                expression = new ClosureCallExpression(token.location(), expression, arguments());
            } else if (token.is(Symbol.ARROW) || token.is(Symbol.NULLSAFE_ARROW)) {
                advance();
                Token property = peek();
                if (!property.is(TokenType.IDENTIFIER)) {
                    throw new PhpSyntaxException(property.location(), "Expected a property name after " + token.text());
                }
                advance();
                expression = new PropertyExpression(
                        token.location(), expression, property.text(), token.is(Symbol.NULLSAFE_ARROW));
            } else if (token.is(Symbol.INCREMENT) || token.is(Symbol.DECREMENT)) {
                advance();
                expression = new IncrementExpression(token.location(), expression, token.is(Symbol.INCREMENT), false);
            } else {
                return expression;
            }
        }
    }

    private PhpExpression primary() {
        Token token = peek();

        if (token.is(TokenType.INT_LITERAL)
                || token.is(TokenType.FLOAT_LITERAL)
                || token.is(TokenType.STRING_LITERAL)) {
            advance();
            return new LiteralExpression(token.location(), token.value());
        }
        if (token.is(TokenType.VARIABLE)) {
            advance();
            return new VariableExpression(token.location(), token.text());
        }
        if (token.is(TokenType.INTERPOLATION_START)) {
            return interpolation(token.location());
        }
        if (token.is(Symbol.LEFT_BRACKET)) {
            advance();
            return arrayLiteral(token.location(), Symbol.RIGHT_BRACKET);
        }
        if (token.is(Symbol.LEFT_PAREN)) {
            advance();
            PhpExpression grouped = parseExpression();
            expect(Symbol.RIGHT_PAREN, "')'");
            return grouped;
        }
        if (token.is(TokenType.IDENTIFIER)) {
            return word(token);
        }
        throw new PhpSyntaxException(token.location(), "Expected an expression but found " + describe(token));
    }

    /** A bare word: one of the three constants, one of the four constructs that look like calls, or a function name. */
    private PhpExpression word(Token token) {
        if (token.isIdentifier("true")) {
            advance();
            return new LiteralExpression(token.location(), PhpBool.TRUE);
        }
        if (token.isIdentifier("false")) {
            advance();
            return new LiteralExpression(token.location(), PhpBool.FALSE);
        }
        if (token.isIdentifier("null")) {
            advance();
            return new LiteralExpression(token.location(), PhpNull.NULL);
        }
        IncludeExpression.Kind include = includeKind(token);
        if (include != null) {
            advance();
            return new IncludeExpression(token.location(), include, parseExpression());
        }
        if (token.isIdentifier("print")) {
            advance();
            return new PrintExpression(token.location(), parseExpression());
        }
        if (token.isIdentifier("isset")) {
            advance();
            return new IssetExpression(token.location(), arguments());
        }
        if (token.isIdentifier("empty")) {
            advance();
            List<PhpExpression> operands = arguments();
            if (operands.size() != 1) {
                throw new PhpSyntaxException(token.location(), "empty() takes exactly one argument");
            }
            return new EmptyExpression(token.location(), operands.get(0));
        }
        if (token.isIdentifier("array") && peekAt(1).is(Symbol.LEFT_PAREN)) {
            advance();
            advance();
            return arrayLiteral(token.location(), Symbol.RIGHT_PAREN);
        }
        if (token.isIdentifier("match") && peekAt(1).is(Symbol.LEFT_PAREN)) {
            advance();
            return matchExpression(token.location());
        }
        if (token.isIdentifier("fn") && peekAt(1).is(Symbol.LEFT_PAREN)) {
            advance();
            return arrowFunction(token.location());
        }
        if (peekAt(1).is(Symbol.LEFT_PAREN)) {
            advance();
            return new FunctionCallExpression(token.location(), token.text(), arguments());
        }
        PhpValue constant = Constants.find(token.text());
        if (constant != null) {
            advance();
            return new LiteralExpression(token.location(), constant);
        }
        throw new PhpSyntaxException(
                token.location(),
                "Undefined constant \"" + token.text() + "\"; this dialect has no user-defined constants");
    }

    /**
     * The pieces of an interpolated string, joined back into one.
     *
     * <p>A concatenation rather than a node of its own: the tree ends up holding what the author could have written by
     * hand, and nothing has to know at run time that the string was ever interpolated. A single piece still gets an
     * empty string concatenated onto it, because {@code "$n"} is a string even when {@code $n} is a number.
     */
    private PhpExpression interpolation(Location start) {
        advance();
        List<PhpExpression> pieces = new ArrayList<>();
        while (!peek().is(TokenType.INTERPOLATION_END)) {
            if (atEnd()) {
                throw new PhpSyntaxException(start, "Unterminated interpolated string");
            }
            pieces.add(parseExpression());
        }
        advance();

        PhpExpression joined = pieces.isEmpty() ? new LiteralExpression(start, PhpString.of("")) : pieces.get(0);
        if (pieces.size() == 1) {
            joined = new BinaryExpression(
                    start, BinaryOperator.CONCAT, new LiteralExpression(start, PhpString.of("")), joined);
        }
        for (int i = 1; i < pieces.size(); i++) {
            joined = new BinaryExpression(start, BinaryOperator.CONCAT, joined, pieces.get(i));
        }
        return joined;
    }

    /** The kind of include this word spells, or null when it spells none. */
    private static IncludeExpression.Kind includeKind(Token token) {
        for (IncludeExpression.Kind kind : IncludeExpression.Kind.values()) {
            if (token.isIdentifier(kind.spelling())) {
                return kind;
            }
        }
        return null;
    }

    private PhpExpression arrayLiteral(Location start, Symbol closer) {
        List<ArrayLiteralExpression.Entry> entries = new ArrayList<>();
        while (!peek().is(closer)) {
            PhpExpression first = parseExpression();
            if (peek().is(Symbol.DOUBLE_ARROW)) {
                advance();
                entries.add(new ArrayLiteralExpression.Entry(first, parseExpression()));
            } else {
                entries.add(new ArrayLiteralExpression.Entry(null, first));
            }
            if (!peek().is(Symbol.COMMA)) {
                break;
            }
            advance();
        }
        expect(closer, "'" + closer.spelling() + "' to close the array");
        return new ArrayLiteralExpression(start, entries);
    }

    private PhpExpression matchExpression(Location start) {
        expect(Symbol.LEFT_PAREN, "'(' after match");
        PhpExpression subject = parseExpression();
        expect(Symbol.RIGHT_PAREN, "')' after the match subject");
        expect(Symbol.LEFT_BRACE, "'{' to open the match arms");

        List<MatchExpression.Arm> arms = new ArrayList<>();
        PhpExpression fallback = null;
        while (!peek().is(Symbol.RIGHT_BRACE)) {
            if (peek().isIdentifier("default")) {
                advance();
                expect(Symbol.DOUBLE_ARROW, "'=>' after default");
                fallback = parseExpression();
            } else {
                arms.add(matchArm());
            }
            if (!peek().is(Symbol.COMMA)) {
                break;
            }
            advance();
        }
        expect(Symbol.RIGHT_BRACE, "'}' to close the match arms");
        return new MatchExpression(start, subject, arms, fallback);
    }

    private MatchExpression.Arm matchArm() {
        List<PhpExpression> conditions = new ArrayList<>();
        conditions.add(parseExpression());
        while (peek().is(Symbol.COMMA)) {
            advance();
            if (peek().is(Symbol.DOUBLE_ARROW)) {
                break;
            }
            conditions.add(parseExpression());
        }
        expect(Symbol.DOUBLE_ARROW, "'=>' after the match conditions");
        return new MatchExpression.Arm(conditions, parseExpression());
    }

    private PhpExpression arrowFunction(Location start) {
        expect(Symbol.LEFT_PAREN, "'(' after fn");
        List<String> parameters = new ArrayList<>();
        while (!peek().is(Symbol.RIGHT_PAREN)) {
            Token parameter = peek();
            if (!parameter.is(TokenType.VARIABLE)) {
                throw new PhpSyntaxException(
                        parameter.location(), "Expected a parameter but found " + describe(parameter));
            }
            advance();
            parameters.add(parameter.text());
            if (!peek().is(Symbol.COMMA)) {
                break;
            }
            advance();
        }
        expect(Symbol.RIGHT_PAREN, "')' after the parameters");
        expect(Symbol.DOUBLE_ARROW, "'=>' before the body of the arrow function");
        return new ArrowFunctionExpression(start, parameters, parseExpression());
    }

    private List<PhpExpression> arguments() {
        expect(Symbol.LEFT_PAREN, "'('");
        List<PhpExpression> values = new ArrayList<>();
        while (!peek().is(Symbol.RIGHT_PAREN)) {
            values.add(parseExpression());
            if (!peek().is(Symbol.COMMA)) {
                break;
            }
            advance();
        }
        expect(Symbol.RIGHT_PAREN, "')'");
        return values;
    }

    // ------------------------------------------------------------ statements

    /** Lexes and parses a whole template. */
    public static PhpTemplate parse(String source, String path, PhpEngineConfig config) {
        return new PhpParser(new PhpLexer(source, path, config).tokenize()).parseTemplate(path);
    }

    public PhpTemplate parseTemplate(String path) {
        List<PhpStatement> statements = statementList();
        if (!atEnd()) {
            throw new PhpSyntaxException(peek().location(), "Unexpected " + describe(peek()));
        }
        return new PhpTemplate(path, statements);
    }

    /**
     * Statements until something closes the block.
     *
     * <p>This one method is what makes the dialect a templating language rather than a scripting one. Literal markup is
     * a statement here like any other, so the body of an {@code if} can be HTML, and {@code <?php if ($x): ?><li><?php
     * endif; ?>} needs no special case anywhere: it is a list holding one text statement. Everything with a colon form
     * goes through here.
     */
    private List<PhpStatement> statementList() {
        List<PhpStatement> statements = new ArrayList<>();
        while (!atEnd()) {
            Token token = peek();
            if (token.is(TokenType.INLINE_HTML)) {
                advance();
                statements.add(new TextStatement(token.location(), token.text()));
            } else if (token.is(TokenType.OPEN_TAG) || token.is(TokenType.CLOSE_TAG)) {
                advance();
            } else if (token.is(TokenType.OPEN_TAG_ECHO)) {
                advance();
                statements.add(echoStatement(token.location()));
            } else if (endsBlock(token)) {
                return statements;
            } else {
                statements.add(statement());
            }
        }
        return statements;
    }

    private boolean endsBlock(Token token) {
        return token.is(Symbol.RIGHT_BRACE)
                || token.is(TokenType.IDENTIFIER)
                        && BLOCK_ENDERS.contains(token.text().toLowerCase(Locale.ROOT));
    }

    private PhpStatement statement() {
        Token token = peek();

        if (token.is(Symbol.LEFT_BRACE)) {
            return block();
        }
        if (token.is(Symbol.SEMICOLON)) {
            advance();
            return new BlockStatement(token.location(), List.of());
        }
        if (token.is(TokenType.IDENTIFIER)) {
            PhpStatement construct = construct(token);
            if (construct != null) {
                return construct;
            }
        }
        PhpExpression expression = parseExpression();
        endStatement();
        return new ExpressionStatement(token.location(), expression);
    }

    /** The statement this word opens, or null when the word is not one of the language's own. */
    private PhpStatement construct(Token token) {
        Location start = token.location();
        if (token.isIdentifier("echo")) {
            advance();
            return echoStatement(start);
        }
        if (token.isIdentifier("if")) {
            advance();
            return ifStatement(start);
        }
        if (token.isIdentifier("while")) {
            advance();
            return whileStatement(start);
        }
        if (token.isIdentifier("do")) {
            advance();
            return doWhileStatement(start);
        }
        if (token.isIdentifier("for")) {
            advance();
            return forStatement(start);
        }
        if (token.isIdentifier("foreach")) {
            advance();
            return foreachStatement(start);
        }
        if (token.isIdentifier("switch")) {
            advance();
            return switchStatement(start);
        }
        if (token.isIdentifier("break")) {
            advance();
            int levels = optionalLevel();
            endStatement();
            return new BreakStatement(start, levels);
        }
        if (token.isIdentifier("continue")) {
            advance();
            int levels = optionalLevel();
            endStatement();
            return new ContinueStatement(start, levels);
        }
        if (token.isIdentifier("return")) {
            advance();
            PhpExpression value = statementEndsHere() ? null : parseExpression();
            endStatement();
            return new ReturnStatement(start, value);
        }
        if (token.isIdentifier("unset")) {
            advance();
            List<PhpExpression> targets = arguments();
            endStatement();
            return new UnsetStatement(start, targets);
        }
        return null;
    }

    private PhpStatement block() {
        Token brace = expect(Symbol.LEFT_BRACE, "'{'");
        List<PhpStatement> statements = statementList();
        expect(Symbol.RIGHT_BRACE, "'}'");
        return new BlockStatement(brace.location(), statements);
    }

    /** A construct's body: a braced block, or a single statement when there are no braces. */
    private PhpStatement body() {
        return peek().is(Symbol.LEFT_BRACE) ? block() : statement();
    }

    private PhpStatement echoStatement(Location start) {
        List<PhpExpression> values = new ArrayList<>();
        values.add(parseExpression());
        while (peek().is(Symbol.COMMA)) {
            advance();
            values.add(parseExpression());
        }
        endStatement();
        return new EchoStatement(start, values);
    }

    private PhpStatement ifStatement(Location start) {
        PhpExpression condition = parenthesised();

        if (peek().is(Symbol.COLON)) {
            advance();
            PhpStatement chain = colonIfChain(start, condition);
            expectWord("endif");
            endStatement();
            return chain;
        }

        PhpStatement whenTrue = body();
        PhpStatement whenFalse = null;
        if (peek().isIdentifier("elseif")) {
            whenFalse = ifStatement(advance().location());
        } else if (peek().isIdentifier("else")) {
            Location elseAt = advance().location();
            whenFalse = peek().isIdentifier("if") ? ifStatement(advance().location()) : body();
            if (whenFalse == null) {
                throw new PhpSyntaxException(elseAt, "Expected a body after else");
            }
        }
        return new IfStatement(start, condition, whenTrue, whenFalse);
    }

    /** The colon form, where one {@code endif} closes however long the chain grows — so this recurses without it. */
    private PhpStatement colonIfChain(Location start, PhpExpression condition) {
        PhpStatement whenTrue = new BlockStatement(start, statementList());
        PhpStatement whenFalse = null;

        if (peek().isIdentifier("elseif")) {
            Location elseIfAt = advance().location();
            PhpExpression next = parenthesised();
            expect(Symbol.COLON, "':' after elseif");
            whenFalse = colonIfChain(elseIfAt, next);
        } else if (peek().isIdentifier("else")) {
            Location elseAt = advance().location();
            expect(Symbol.COLON, "':' after else");
            whenFalse = new BlockStatement(elseAt, statementList());
        }
        return new IfStatement(start, condition, whenTrue, whenFalse);
    }

    private PhpStatement whileStatement(Location start) {
        PhpExpression condition = parenthesised();
        if (peek().is(Symbol.COLON)) {
            advance();
            PhpStatement body = new BlockStatement(start, statementList());
            expectWord("endwhile");
            endStatement();
            return new WhileStatement(start, condition, body);
        }
        return new WhileStatement(start, condition, body());
    }

    private PhpStatement doWhileStatement(Location start) {
        PhpStatement body = body();
        expectWord("while");
        PhpExpression condition = parenthesised();
        endStatement();
        return new DoWhileStatement(start, body, condition);
    }

    private PhpStatement forStatement(Location start) {
        expect(Symbol.LEFT_PAREN, "'(' after for");
        List<PhpExpression> initialisers = expressionsUntil(Symbol.SEMICOLON);
        expect(Symbol.SEMICOLON, "';' after the for initialiser");
        List<PhpExpression> conditions = expressionsUntil(Symbol.SEMICOLON);
        expect(Symbol.SEMICOLON, "';' after the for condition");
        List<PhpExpression> steps = expressionsUntil(Symbol.RIGHT_PAREN);
        expect(Symbol.RIGHT_PAREN, "')' after the for step");

        if (peek().is(Symbol.COLON)) {
            advance();
            PhpStatement body = new BlockStatement(start, statementList());
            expectWord("endfor");
            endStatement();
            return new ForStatement(start, initialisers, conditions, steps, body);
        }
        return new ForStatement(start, initialisers, conditions, steps, body());
    }

    private PhpStatement foreachStatement(Location start) {
        expect(Symbol.LEFT_PAREN, "'(' after foreach");
        PhpExpression subject = parseExpression();
        expectWord("as");

        String keyName = null;
        String valueName = expectVariable().text();
        if (peek().is(Symbol.DOUBLE_ARROW)) {
            advance();
            keyName = valueName;
            valueName = expectVariable().text();
        }
        expect(Symbol.RIGHT_PAREN, "')' after the foreach subject");

        if (peek().is(Symbol.COLON)) {
            advance();
            PhpStatement body = new BlockStatement(start, statementList());
            expectWord("endforeach");
            endStatement();
            return new ForeachStatement(start, subject, keyName, valueName, body);
        }
        return new ForeachStatement(start, subject, keyName, valueName, body());
    }

    private PhpStatement switchStatement(Location start) {
        PhpExpression subject = parenthesised();
        boolean colonForm = peek().is(Symbol.COLON);
        if (colonForm) {
            advance();
        } else {
            expect(Symbol.LEFT_BRACE, "'{' to open the switch");
        }
        skipBlankBeforeFirstCase();

        List<SwitchStatement.Branch> branches = new ArrayList<>();
        while (peek().isIdentifier("case") || peek().isIdentifier("default")) {
            boolean isDefault = peek().isIdentifier("default");
            advance();
            PhpExpression test = isDefault ? null : parseExpression();
            if (!peek().is(Symbol.COLON) && !peek().is(Symbol.SEMICOLON)) {
                throw new PhpSyntaxException(peek().location(), "Expected ':' after the case label");
            }
            advance();
            branches.add(new SwitchStatement.Branch(test, statementList()));
        }

        if (colonForm) {
            expectWord("endswitch");
            endStatement();
        } else {
            expect(Symbol.RIGHT_BRACE, "'}' to close the switch");
        }
        return new SwitchStatement(start, subject, branches);
    }

    /**
     * Between a switch header and its first case PHP allows nothing but whitespace — and a closing tag, which is how a
     * template gets a newline in there without it counting.
     */
    private void skipBlankBeforeFirstCase() {
        while (true) {
            Token token = peek();
            boolean skippable = token.is(TokenType.OPEN_TAG)
                    || token.is(TokenType.CLOSE_TAG)
                    || token.is(TokenType.INLINE_HTML) && token.text().isBlank();
            if (!skippable) {
                return;
            }
            advance();
        }
    }

    private List<PhpExpression> expressionsUntil(Symbol closer) {
        List<PhpExpression> expressions = new ArrayList<>();
        if (peek().is(closer)) {
            return expressions;
        }
        expressions.add(parseExpression());
        while (peek().is(Symbol.COMMA)) {
            advance();
            expressions.add(parseExpression());
        }
        return expressions;
    }

    private PhpExpression parenthesised() {
        expect(Symbol.LEFT_PAREN, "'('");
        PhpExpression expression = parseExpression();
        expect(Symbol.RIGHT_PAREN, "')'");
        return expression;
    }

    private int optionalLevel() {
        if (!peek().is(TokenType.INT_LITERAL)) {
            return 1;
        }
        return (int) advance().value().toInt();
    }

    private boolean statementEndsHere() {
        return peek().is(Symbol.SEMICOLON) || peek().is(TokenType.CLOSE_TAG) || atEnd();
    }

    /** A statement ends at a semicolon — or at a closing tag, which supplies one, or at the end of the template. */
    private void endStatement() {
        if (peek().is(Symbol.SEMICOLON)) {
            advance();
            return;
        }
        if (statementEndsHere()) {
            return;
        }
        throw new PhpSyntaxException(peek().location(), "Expected ';' but found " + describe(peek()));
    }

    private Token expectWord(String word) {
        if (!peek().isIdentifier(word)) {
            throw new PhpSyntaxException(peek().location(), "Expected '" + word + "' but found " + describe(peek()));
        }
        return advance();
    }

    private Token expectVariable() {
        if (!peek().is(TokenType.VARIABLE)) {
            throw new PhpSyntaxException(peek().location(), "Expected a variable but found " + describe(peek()));
        }
        return advance();
    }

    // ---------------------------------------------------------------- cursor

    /**
     * The operator the token ahead stands for, or null when it stands for none.
     *
     * <p>The null check is not decoration: a token that is not a symbol carries a null symbol, and the immutable maps
     * above reject a null key with an exception rather than answering "not found".
     */
    private BinaryOperator lookUp(Map<Symbol, BinaryOperator> operators) {
        Symbol symbol = peek().symbol();
        return symbol == null ? null : operators.get(symbol);
    }

    private Token peekAt(int ahead) {
        return tokens.get(Math.min(at + ahead, tokens.size() - 1));
    }

    private Token advance() {
        Token token = peek();
        if (at < tokens.size() - 1) {
            at++;
        }
        return token;
    }

    private Token expect(Symbol symbol, String what) {
        if (!peek().is(symbol)) {
            throw new PhpSyntaxException(peek().location(), "Expected " + what + " but found " + describe(peek()));
        }
        return advance();
    }

    private static String describe(Token token) {
        if (token.is(TokenType.EOF)) {
            return "the end of the template";
        }
        return token.text() != null ? "'" + token.text() + "'" : token.type().toString();
    }
}
