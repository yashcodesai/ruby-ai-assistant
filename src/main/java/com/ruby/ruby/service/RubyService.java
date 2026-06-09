package com.ruby.ruby.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class RubyService {
    private final List<String> conversationHistory =
            new ArrayList<>();

    private final List<String> notes =
            new ArrayList<>();

    private final Random random =
            new Random();

    public String processMessage(String message) {

        message = message.toLowerCase();

        conversationHistory.add(message);

        // App Opening

        if(message.contains("open google")) {

            return "OPEN_GOOGLE";
        }

        if(message.contains("open youtube")) {

            return "OPEN_YOUTUBE";
        }

        if(message.contains("open github")) {

            return "OPEN_GITHUB";
        }

        if(message.contains("open chatgpt")) {

            return "OPEN_CHATGPT";
        }

        // Voice Friendly Calculator

        message = message
                .replace("plus", "+")
                .replace("minus", "-")
                .replace("times", "*")
                .replace("multiplied by", "*")
                .replace("divided by", "/");

        // Calculator

        if(message.startsWith("calculate")) {

            String expression =
                    message.replace(
                            "calculate",
                            ""
                    ).trim();

            return calculate(expression);
        }

        // Notes

        if(message.startsWith("remember")) {

            String note =
                    message.replace(
                            "remember",
                            ""
                    ).trim();

            notes.add(note);

            return "I will remember that.";
        }

        if(message.contains("show notes")) {

            if(notes.isEmpty()) {

                return "You have no saved notes.";
            }

            StringBuilder builder =
                    new StringBuilder();

            builder.append(
                    "Your notes are. "
            );

            for(String note : notes) {

                builder.append(note)
                        .append(". ");
            }

            return builder.toString();
        }

        if(message.contains("clear notes")) {

            notes.clear();

            return "All notes have been cleared.";
        }

        // Memory

        if(message.contains(
                "what did i ask before")) {

            if(conversationHistory.size() < 2) {

                return "You have not asked enough questions yet.";
            }

            return "Your previous command was "
                    + conversationHistory.get(
                    conversationHistory.size() - 2
            );
        }

        if(message.contains(
                "memory status")) {

            return "I currently remember "
                    + notes.size()
                    + " notes and "
                    + conversationHistory.size()
                    + " commands.";
        }

        // Greetings

        if(message.contains("hello")
                || message.contains("hi")
                || message.contains("hey")) {

            return "Hello Yash. How can I assist you today?";
        }

        // Time

        if(message.contains("time")) {

            return "The current time is "
                    + LocalTime.now()
                    .withNano(0);
        }

        // Date

        if(message.contains("date")) {

            return "Today's date is "
                    + LocalDate.now();
        }

        // Day

        if(message.contains("day")) {

            return "Today is "
                    + LocalDate.now()
                    .getDayOfWeek();
        }

        // Identity

        if(message.contains(
                "who are you")) {

            return """
                I am RUBY,
                your intelligent
                voice assistant.
                """;
        }

        if(message.contains(
                "your name")) {

            return "My name is RUBY.";
        }

        if(message.contains(
                "who created you")
                || message.contains(
                "who made you")) {

            return "I was created by Yash.";
        }

        // Health

        if(message.contains(
                "how are you")) {

            return "I am functioning perfectly.";
        }

        // System Status

        if(message.contains(
                "system status")) {

            return """
                All systems operational.
                Voice engine active.
                AI core online.
                Security level maximum.
                """;
        }

        // Motivation

        if(message.contains(
                "motivate me")) {

            return """
                Success is the sum of
                small efforts repeated
                day after day.
                """;
        }

        // Joke

        if(message.contains("joke")) {

            return """
                Why do programmers
                prefer dark mode?
                Because light
                attracts bugs.
                """;
        }

        // Dice

        if(message.contains(
                "roll dice")
                || message.contains(
                "roll a dice")) {

            return "You rolled "
                    + (random.nextInt(6) + 1);
        }

        // Coin Flip

        if(message.contains(
                "flip coin")
                || message.contains(
                "flip a coin")) {

            return random.nextBoolean()
                    ? "Heads"
                    : "Tails";
        }

        // Random Number

        if(message.contains(
                "random number")) {

            return "Your random number is "
                    + random.nextInt(100);
        }

        // Capabilities

        if(message.contains(
                "what can you do")) {

            return """
                I can tell time,
                date, day,
                perform calculations,
                remember notes,
                show notes,
                clear notes,
                roll dice,
                flip coins,
                generate random numbers,
                tell jokes,
                motivate you,
                and provide
                system status.
                """;
        }

        // Goodbye

        if(message.contains("bye")
                || message.contains(
                "goodbye")) {

            return """
                Goodbye Yash.
                Have a productive day.
                """;
        }

        return """
            I am still learning.
            I do not know how to
            answer that yet.
            """;
    }

    private String calculate(
            String expression) {

        try {

            if(expression.contains("+")) {

                String[] parts =
                        expression.split(
                                "\\+");

                double a =
                        Double.parseDouble(
                                parts[0].trim());

                double b =
                        Double.parseDouble(
                                parts[1].trim());

                return "The answer is "
                        + (a + b);
            }

            if(expression.contains("-")) {

                String[] parts =
                        expression.split(
                                "-");

                double a =
                        Double.parseDouble(
                                parts[0].trim());

                double b =
                        Double.parseDouble(
                                parts[1].trim());

                return "The answer is "
                        + (a - b);
            }

            if(expression.contains("*")) {

                String[] parts =
                        expression.split(
                                "\\*");

                double a =
                        Double.parseDouble(
                                parts[0].trim());

                double b =
                        Double.parseDouble(
                                parts[1].trim());

                return "The answer is "
                        + (a * b);
            }

            if(expression.contains("/")) {

                String[] parts =
                        expression.split("/");

                double a =
                        Double.parseDouble(
                                parts[0].trim());

                double b =
                        Double.parseDouble(
                                parts[1].trim());

                if(b == 0) {

                    return "Division by zero is not allowed.";
                }

                return "The answer is "
                        + (a / b);
            }

            return "I can only perform basic calculations.";

        } catch(Exception e) {

            return "Invalid calculation.";
        }
    }
}