package com.ruby.ruby.command;

import com.ruby.ruby.memory.ConversationMemory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * "calculate 2 + 3 * 4" always tries to calculate. Questions such as
 * "what is 5 plus 3" are treated as maths only if the rest is clearly
 * an arithmetic expression, so "what is your name" is left alone.
 */
@Component
@Order(20)
public class CalculatorCommand implements Command {

    private static final String[] COMMAND_PREFIXES = {"calculate", "compute", "solve"};
    private static final String[] QUESTION_PREFIXES = {"what is", "what's", "whats", "how much is"};

    private static final Pattern MATH_ONLY = Pattern.compile("[\\d\\s+\\-*/^().]+");
    private static final Pattern HAS_OPERATOR = Pattern.compile("\\d\\s*[-+*/^]|[-+*/^]\\s*[\\d(]");

    private static final Map<String, Integer> UNITS = Map.ofEntries(
            Map.entry("zero", 0), Map.entry("one", 1), Map.entry("two", 2), Map.entry("three", 3),
            Map.entry("four", 4), Map.entry("five", 5), Map.entry("six", 6), Map.entry("seven", 7),
            Map.entry("eight", 8), Map.entry("nine", 9), Map.entry("ten", 10), Map.entry("eleven", 11),
            Map.entry("twelve", 12), Map.entry("thirteen", 13), Map.entry("fourteen", 14),
            Map.entry("fifteen", 15), Map.entry("sixteen", 16), Map.entry("seventeen", 17),
            Map.entry("eighteen", 18), Map.entry("nineteen", 19));

    private static final Map<String, Integer> TENS = Map.of(
            "twenty", 20, "thirty", 30, "forty", 40, "fifty", 50,
            "sixty", 60, "seventy", 70, "eighty", 80, "ninety", 90);

    @Override
    public boolean matches(Message message) {
        return message.startsWith(COMMAND_PREFIXES) || questionExpression(message) != null;
    }

    @Override
    public Reply execute(Message message, ConversationMemory memory) {
        String expression = message.startsWith(COMMAND_PREFIXES)
                ? stripQuestion(toSymbols(removePrefix(message.normalized(), COMMAND_PREFIXES)))
                : questionExpression(message);
        if (expression.isBlank()) {
            return Reply.say("What would you like me to calculate?");
        }
        try {
            return Reply.say("The answer is " + format(ExpressionParser.evaluate(expression)) + ".");
        } catch (ArithmeticException e) {
            return Reply.say("Division by zero is not allowed.");
        } catch (IllegalArgumentException e) {
            return Reply.say("Sorry, I could not understand that calculation.");
        }
    }

    /** The expression in "what is 5 plus 3", or null if the question is not maths. */
    private static String questionExpression(Message message) {
        if (!message.startsWith(QUESTION_PREFIXES)) {
            return null;
        }
        String expression = toSymbols(removePrefix(message.normalized(), QUESTION_PREFIXES));
        boolean isMath = MATH_ONLY.matcher(expression).matches()
                && HAS_OPERATOR.matcher(expression).find();
        return isMath ? expression : null;
    }

    private static String removePrefix(String text, String[] prefixes) {
        for (String prefix : prefixes) {
            if (text.startsWith(prefix)) {
                return text.substring(prefix.length());
            }
        }
        return text;
    }

    private static String stripQuestion(String expression) {
        return removePrefix(expression.trim(), QUESTION_PREFIXES).trim();
    }

    /** Turns spoken maths ("five plus 3", "2 x 4", "10% of 50") into symbols. */
    static String toSymbols(String spoken) {
        String text = wordsToDigits(spoken)
                .replace(",", "")
                .replaceAll("[?!=]+", " ")
                .replaceAll("\\.\\s*$", "")
                .replaceAll("\\bmultiplied by\\b", "*")
                .replaceAll("\\bdivided by\\b", "/")
                .replaceAll("\\bto the power of\\b", "^")
                .replaceAll("\\bsquared\\b", "^2")
                .replaceAll("\\bcubed\\b", "^3")
                .replaceAll("\\bplus\\b", "+")
                .replaceAll("\\bminus\\b", "-")
                .replaceAll("\\btimes\\b", "*")
                .replaceAll("\\binto\\b", "*")
                .replaceAll("\\bover\\b", "/")
                .replaceAll("(?<=[\\d)\\s])x(?=[\\d(\\s])", "*")
                .replace("×", "*")
                .replace("÷", "/")
                .replaceAll("(\\d+(?:\\.\\d+)?)\\s*(?:%|percent)\\s+of\\b", "($1 / 100) *")
                .replaceAll("(\\d+(?:\\.\\d+)?)\\s*(?:%|percent)", "($1 / 100)")
                .replaceAll("\\bequals?\\b", "")
                .replaceAll("\\s+", " ");
        return text.trim();
    }

    /** "twenty five" -> "25", "three" -> "3". Covers 0-99, which is what people say. */
    static String wordsToDigits(String text) {
        Matcher matcher = Pattern.compile(
                "\\b(" + String.join("|", TENS.keySet()) + ")(?:(?:\\s+|-)("
                        + "one|two|three|four|five|six|seven|eight|nine))?\\b"
                        + "|\\b(" + String.join("|", UNITS.keySet()) + ")\\b").matcher(text);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            int value;
            if (matcher.group(1) != null) {
                value = TENS.get(matcher.group(1))
                        + (matcher.group(2) == null ? 0 : UNITS.get(matcher.group(2)));
            } else {
                value = UNITS.get(matcher.group(3));
            }
            matcher.appendReplacement(out, String.valueOf(value));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    /** 12.0 -> "12", 0.1 + 0.2 -> "0.3", 1 / 3 -> "0.333333". */
    static String format(double value) {
        if (Double.isInfinite(value) || Double.isNaN(value)) {
            return "too large to display";
        }
        return BigDecimal.valueOf(value)
                .setScale(6, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }
}
