package com.ruby.ruby.command;

import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads spoken durations: "5 minutes", "one hour and 30 minutes", "half an hour", "a minute". */
final class Durations {

    static final int MAX_SECONDS = 24 * 60 * 60;

    private static final Pattern PART = Pattern.compile(
            "(\\d+(?:\\.\\d+)?)\\s*(seconds?|secs?|minutes?|mins?|hours?|hrs?)\\b");

    /** Matches a duration phrase in the original text, for removing it from reminder labels. */
    static final String PHRASE =
            "(?:\\d+(?:\\.\\d+)?|an?|half an?|one|two|three|four|five|six|seven|eight|nine|ten"
                    + "|fifteen|twenty|thirty|forty|forty five|fifty|sixty|ninety)\\s*"
                    + "(?:seconds?|secs?|minutes?|mins?|hours?|hrs?)"
                    + "(?:\\s+and\\s+(?:\\d+|an?|half|a half|one|two|three|four|five|ten|fifteen|twenty|thirty|"
                    + "forty|forty five|fifty)\\s*(?:seconds?|secs?|minutes?|mins?|hours?|hrs?)?)?";

    private Durations() {
    }

    static OptionalInt parseSeconds(String normalized) {
        String text = CalculatorCommand.wordsToDigits(normalized)
                .replaceAll("\\bhalf an hour\\b", "30 minutes")
                .replaceAll("\\bhalf a minute\\b", "30 seconds")
                .replaceAll("\\b(\\d+) and a half (hours?|minutes?)\\b", "$1.5 $2")
                .replaceAll("\\ban? (hour|minute|second)\\b", "1 $1");
        Matcher matcher = PART.matcher(text);
        double seconds = 0;
        boolean found = false;
        while (matcher.find()) {
            found = true;
            double amount = Double.parseDouble(matcher.group(1));
            String unit = matcher.group(2);
            seconds += amount * (unit.startsWith("h") ? 3600 : unit.startsWith("m") ? 60 : 1);
        }
        if (!found || seconds < 1 || seconds > MAX_SECONDS) {
            return OptionalInt.empty();
        }
        return OptionalInt.of((int) Math.round(seconds));
    }

    /** 90 -> "1 minute 30 seconds", 3600 -> "1 hour". */
    static String describe(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        StringBuilder text = new StringBuilder();
        append(text, hours, "hour");
        append(text, minutes, "minute");
        append(text, seconds, "second");
        return text.toString();
    }

    /** As an adjective: 300 -> "5 minute" (as in "your 5 minute timer"). */
    static String describeAsAdjective(int totalSeconds) {
        return describe(totalSeconds).replaceAll("(hour|minute|second)s\\b", "$1");
    }

    private static void append(StringBuilder text, int amount, String unit) {
        if (amount == 0) {
            return;
        }
        if (!text.isEmpty()) {
            text.append(' ');
        }
        text.append(amount).append(' ').append(unit).append(amount == 1 ? "" : "s");
    }
}
