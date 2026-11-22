package com.ruby.ruby.command;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * A user message: the original text (case preserved, used for notes)
 * and a lowercase form used for matching. Matching is done on whole
 * words so that "hi" does not match "this" or "history".
 *
 * Wake words and polite wrappers ("Ruby,", "hey Ruby", "please",
 * "can you", trailing "please") are removed first, so
 * "Ruby, can you remember X please" is handled like "remember X".
 */
public record Message(String original, String normalized) {

    private static final Pattern LEADING_FILLER = Pattern.compile(
            "^(?:(?:(?:hey|hi|hello|ok|okay)\\s+)?ruby\\b|please\\b|kindly\\b"
                    + "|(?:can|could|would|will)\\s+you\\b)[\\s,.!:]*",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern TRAILING_FILLER = Pattern.compile(
            "[\\s,]+(?:please|ruby)[.!?]*$", Pattern.CASE_INSENSITIVE);

    public static Message of(String text) {
        String cleaned = text.trim().replace('’', '\'');
        String stripped = stripFillers(cleaned);
        // "hi ruby" on its own is a greeting: keep it rather than strip to nothing.
        String result = stripped.isEmpty() ? cleaned : stripped;
        return new Message(result, result.toLowerCase(Locale.ROOT));
    }

    private static String stripFillers(String text) {
        String previous;
        String current = text;
        do {
            previous = current;
            current = LEADING_FILLER.matcher(current).replaceFirst("");
            current = TRAILING_FILLER.matcher(current).replaceFirst("").trim();
        } while (!current.equals(previous));
        return current;
    }

    /** True if any of the given words or phrases appears as whole words. */
    public boolean has(String... phrases) {
        for (String phrase : phrases) {
            if (Pattern.compile("\\b" + wordRegex(phrase) + "\\b").matcher(normalized).find()) {
                return true;
            }
        }
        return false;
    }

    /** True if the message starts with any of the given words or phrases. */
    public boolean startsWith(String... phrases) {
        for (String phrase : phrases) {
            if (Pattern.compile("^" + wordRegex(phrase) + "\\b").matcher(normalized).find()) {
                return true;
            }
        }
        return false;
    }

    /** True if the whole message (ignoring punctuation) is one of the phrases. */
    public boolean is(String... phrases) {
        String bare = normalized.replaceAll("[^a-z0-9' ]", "").trim();
        for (String phrase : phrases) {
            if (bare.equals(phrase)) {
                return true;
            }
        }
        return false;
    }

    /** Original text after a leading word/phrase, e.g. "remember Buy milk" -> "Buy milk". */
    public String originalAfter(String phrase) {
        return original.replaceFirst("(?i)^" + wordRegex(phrase) + "\\b", "").trim();
    }

    /** Lets "show notes" also match "show   notes"; everything else is literal. */
    private static String wordRegex(String phrase) {
        String[] words = phrase.trim().split("\\s+");
        StringBuilder regex = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                regex.append("\\s+");
            }
            regex.append(Pattern.quote(words[i]));
        }
        return regex.toString();
    }
}
