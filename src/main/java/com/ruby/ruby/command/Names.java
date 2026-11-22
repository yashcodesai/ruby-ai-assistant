package com.ruby.ruby.command;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Pulls a person's name out of answers like "my name is Priya" or just "priya". */
public final class Names {

    /** Always means "set my name", whenever it is said. */
    static final Pattern EXPLICIT = Pattern.compile(
            "^(?:my name is|my name's|you can call me|call me|name is)\\s+(.+)$",
            Pattern.CASE_INSENSITIVE);

    /** Only a name when RUBY has just asked for it: "I'm hungry" is not a name otherwise. */
    static final Pattern INTRODUCTION = Pattern.compile(
            "^(?:i am|i'm|im|this is|it's|it is)\\s+(.+)$", Pattern.CASE_INSENSITIVE);

    private static final Pattern WORD = Pattern.compile("\\p{L}[\\p{L}'-]*");
    private static final int MAX_WORDS = 3;
    private static final int MAX_LENGTH = 40;

    /** Words that answer "what is your name?" but are not names. */
    private static final Set<String> NOT_NAMES = Set.of(
            "a", "an", "the", "not", "no", "yes", "yeah", "nope", "ok", "okay", "sure", "maybe",
            "fine", "good", "great", "well", "bad", "sad", "happy", "tired", "hungry", "busy",
            "back", "here", "ready", "sorry", "doing", "just", "very", "so", "nothing", "nobody",
            "none", "what", "who", "how", "why", "where", "when", "hello", "hi", "hey", "ruby",
            "thanks", "thank", "stop", "bye", "goodbye", "open", "calculate", "remember", "tell",
            "show", "help", "your", "my", "is", "me", "you", "unknown", "secret", "blah");

    private Names() {
    }

    public static Optional<String> fromExplicit(Message message) {
        return fromPattern(EXPLICIT, message);
    }

    public static Optional<String> fromIntroduction(Message message) {
        return fromPattern(INTRODUCTION, message);
    }

    /** A bare answer such as "Priya" or "mary jane". */
    public static Optional<String> fromBareAnswer(Message message) {
        return clean(message.original());
    }

    private static Optional<String> fromPattern(Pattern pattern, Message message) {
        Matcher matcher = pattern.matcher(message.original());
        return matcher.matches() ? clean(matcher.group(1)) : Optional.empty();
    }

    /** 1-3 words of letters, not a filler word; "priya sharma" -> "Priya Sharma". */
    public static Optional<String> clean(String raw) {
        String text = raw.trim().replaceAll("[.!?,]+$", "").trim();
        String[] words = text.split("\\s+");
        if (text.isEmpty() || text.length() > MAX_LENGTH || words.length > MAX_WORDS) {
            return Optional.empty();
        }
        StringBuilder name = new StringBuilder();
        for (String word : words) {
            if (!WORD.matcher(word).matches() || NOT_NAMES.contains(word.toLowerCase(Locale.ROOT))) {
                return Optional.empty();
            }
            if (!name.isEmpty()) {
                name.append(' ');
            }
            name.append(capitalize(word));
        }
        return Optional.of(name.toString());
    }

    /** "priya" -> "Priya"; mixed case such as "McKenzie" is kept as typed. */
    private static String capitalize(String word) {
        if (!word.equals(word.toLowerCase(Locale.ROOT))) {
            return word;
        }
        return word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1);
    }
}
