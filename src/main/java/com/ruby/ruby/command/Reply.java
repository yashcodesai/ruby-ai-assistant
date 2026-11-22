package com.ruby.ruby.command;

/**
 * @param text          what RUBY says
 * @param url           website to open in the browser, or null
 * @param stopListening true if voice mode should switch off after this reply
 */
public record Reply(String text, String url, boolean stopListening) {

    public static Reply say(String text) {
        return new Reply(text, null, false);
    }

    public static Reply open(String text, String url) {
        return new Reply(text, url, false);
    }

    public static Reply sayAndStop(String text) {
        return new Reply(text, null, true);
    }
}
