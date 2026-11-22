package com.ruby.ruby.command;

/**
 * Small recursive-descent parser for arithmetic: + - * / ^ with normal
 * precedence, parentheses, unary minus and decimals.
 *
 * <pre>
 * expression := term (('+' | '-') term)*
 * term       := factor (('*' | '/') factor)*
 * factor     := ('+' | '-') factor | power
 * power      := primary ('^' factor)?        (right-associative, -2^2 = -4)
 * primary    := number | '(' expression ')'
 * </pre>
 */
public final class ExpressionParser {

    private final String input;
    private int pos;

    private ExpressionParser(String input) {
        this.input = input;
    }

    /**
     * @throws IllegalArgumentException if the expression is malformed
     * @throws ArithmeticException      on division by zero
     */
    public static double evaluate(String expression) {
        ExpressionParser parser = new ExpressionParser(expression);
        double value = parser.parseExpression();
        parser.skipSpaces();
        if (parser.pos < parser.input.length()) {
            throw new IllegalArgumentException(
                    "Unexpected '" + parser.input.charAt(parser.pos) + "'");
        }
        return value;
    }

    private double parseExpression() {
        double value = parseTerm();
        while (true) {
            if (eat('+')) {
                value += parseTerm();
            } else if (eat('-')) {
                value -= parseTerm();
            } else {
                return value;
            }
        }
    }

    private double parseTerm() {
        double value = parseFactor();
        while (true) {
            if (eat('*')) {
                value *= parseFactor();
            } else if (eat('/')) {
                double divisor = parseFactor();
                if (divisor == 0) {
                    throw new ArithmeticException("Division by zero");
                }
                value /= divisor;
            } else {
                return value;
            }
        }
    }

    private double parseFactor() {
        if (eat('+')) {
            return parseFactor();
        }
        if (eat('-')) {
            return -parseFactor();
        }
        double base = parsePrimary();
        if (eat('^')) {
            return Math.pow(base, parseFactor());
        }
        return base;
    }

    private double parsePrimary() {
        if (eat('(')) {
            double value = parseExpression();
            if (!eat(')')) {
                throw new IllegalArgumentException("Missing ')'");
            }
            return value;
        }
        return parseNumber();
    }

    private double parseNumber() {
        skipSpaces();
        int start = pos;
        while (pos < input.length()
                && (Character.isDigit(input.charAt(pos)) || input.charAt(pos) == '.')) {
            pos++;
        }
        if (start == pos) {
            throw new IllegalArgumentException("Expected a number");
        }
        return Double.parseDouble(input.substring(start, pos));
    }

    private boolean eat(char expected) {
        skipSpaces();
        if (pos < input.length() && input.charAt(pos) == expected) {
            pos++;
            return true;
        }
        return false;
    }

    private void skipSpaces() {
        while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) {
            pos++;
        }
    }
}
