package com.ruby.ruby.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CalculatorCommandTest {

    @Test
    void convertsSpokenOperators() {
        assertEquals("5 + 3 * 2", CalculatorCommand.toSymbols("5 plus 3 times 2"));
        assertEquals("10 / 2", CalculatorCommand.toSymbols("10 divided by 2"));
        assertEquals("4 * 3", CalculatorCommand.toSymbols("4 x 3"));
        assertEquals("1000 - 1", CalculatorCommand.toSymbols("1,000 minus 1?"));
        assertEquals("(50 / 100) * 200", CalculatorCommand.toSymbols("50 percent of 200"));
        assertEquals("2 ^ 8", CalculatorCommand.toSymbols("2 to the power of 8"));
    }

    @Test
    void convertsNumberWords() {
        assertEquals("25 + 3", CalculatorCommand.wordsToDigits("twenty five + three"));
        assertEquals("40 - 12", CalculatorCommand.wordsToDigits("forty - twelve"));
    }

    @Test
    void formatsResults() {
        assertEquals("12", CalculatorCommand.format(12.0));
        assertEquals("0.3", CalculatorCommand.format(0.1 + 0.2));
        assertEquals("0.333333", CalculatorCommand.format(1.0 / 3));
        assertEquals("-2.5", CalculatorCommand.format(-2.5));
    }
}
