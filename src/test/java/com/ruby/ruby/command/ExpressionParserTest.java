package com.ruby.ruby.command;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExpressionParserTest {

    @ParameterizedTest
    @CsvSource(delimiter = '=', value = {
            "5 + 7 = 12",
            "2 + 3 * 4 = 14",
            "(2 + 3) * 4 = 20",
            "10 - 3 - 2 = 5",
            "100 / 10 / 2 = 5",
            "-5 + 3 = -2",
            "10 - -2 = 12",
            "1.5 * 2 = 3",
            "-(2 + 3) = -5",
            "2 ^ 10 = 1024",
            "2 ^ 3 ^ 2 = 512",
            "-2 ^ 2 = -4",
            "2 * 3 ^ 2 = 18",
    })
    void evaluates(String expression, double expected) {
        assertEquals(expected, ExpressionParser.evaluate(expression), 1e-9);
    }

    @Test
    void divisionByZeroThrows() {
        assertThrows(ArithmeticException.class, () -> ExpressionParser.evaluate("5 / 0"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "2 +", "abc", "(2 + 3", "2 3", "1..2"})
    void malformedInputThrows(String expression) {
        assertThrows(IllegalArgumentException.class, () -> ExpressionParser.evaluate(expression));
    }
}
