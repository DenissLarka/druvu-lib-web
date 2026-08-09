package com.druvu.web.php.internal.ast.expr;

import com.druvu.web.php.internal.value.PhpArithmetic;
import com.druvu.web.php.internal.value.PhpBool;
import com.druvu.web.php.internal.value.PhpComparison;
import com.druvu.web.php.internal.value.PhpInt;
import com.druvu.web.php.internal.value.PhpValue;

/**
 * Every operator that takes two already-evaluated operands, as a table.
 *
 * <p>One enum with a behaviour per constant rather than a class per operator: sixteen tiny classes would say the same
 * thing over sixteen files, and here the whole set can be read at once.
 *
 * <p>The short-circuiting operators are not here — they cannot evaluate both sides up front, so they live in
 * {@link LogicalExpression}.
 */
public enum BinaryOperator {
    ADD("+", PhpArithmetic::add),
    SUBTRACT("-", PhpArithmetic::subtract),
    MULTIPLY("*", PhpArithmetic::multiply),
    DIVIDE("/", PhpArithmetic::divide),
    MODULO("%", PhpArithmetic::modulo),
    POWER("**", PhpArithmetic::power),
    CONCAT(".", PhpArithmetic::concat),

    EQUAL("==", (left, right) -> PhpBool.of(PhpComparison.looseEquals(left, right))),
    NOT_EQUAL("!=", (left, right) -> PhpBool.of(!PhpComparison.looseEquals(left, right))),
    IDENTICAL("===", (left, right) -> PhpBool.of(PhpComparison.identical(left, right))),
    NOT_IDENTICAL("!==", (left, right) -> PhpBool.of(!PhpComparison.identical(left, right))),

    LESS("<", (left, right) -> PhpBool.of(PhpComparison.lessThan(left, right))),
    LESS_OR_EQUAL("<=", (left, right) -> PhpBool.of(PhpComparison.lessOrEqual(left, right))),
    GREATER(">", (left, right) -> PhpBool.of(PhpComparison.greaterThan(left, right))),
    GREATER_OR_EQUAL(">=", (left, right) -> PhpBool.of(PhpComparison.greaterOrEqual(left, right))),
    SPACESHIP("<=>", (left, right) -> PhpInt.of(PhpComparison.compare(left, right)));

    /** What an operator does once both sides are values. */
    @FunctionalInterface
    private interface Operation {
        PhpValue apply(PhpValue left, PhpValue right);
    }

    private final String spelling;
    private final Operation operation;

    BinaryOperator(String spelling, Operation operation) {
        this.spelling = spelling;
        this.operation = operation;
    }

    public String spelling() {
        return spelling;
    }

    public PhpValue apply(PhpValue left, PhpValue right) {
        return operation.apply(left, right);
    }
}
