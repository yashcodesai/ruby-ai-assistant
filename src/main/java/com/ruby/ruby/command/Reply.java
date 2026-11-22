package com.ruby.ruby.command;

/**
 * Everything a command can send back.
 *
 * @param text          what RUBY says
 * @param url           website to open in the browser, or null
 * @param stopListening true if voice mode should switch off after this reply
 * @param card          rich result to display, or null
 * @param timer         countdown to start in the browser, or null
 * @param cancelTimers  true if the browser should cancel all running timers
 */
public record Reply(String text, String url, boolean stopListening,
                    Card card, TimerSpec timer, boolean cancelTimers) {

    public static Reply say(String text) {
        return new Reply(text, null, false, null, null, false);
    }

    public static Reply open(String text, String url) {
        return new Reply(text, url, false, null, null, false);
    }

    public static Reply sayAndStop(String text) {
        return new Reply(text, null, true, null, null, false);
    }

    public static Reply withCard(String text, Card card) {
        return new Reply(text, null, false, card, null, false);
    }

    public static Reply startTimer(String text, TimerSpec timer) {
        return new Reply(text, null, false, null, timer, false);
    }

    public static Reply cancelTimers(String text) {
        return new Reply(text, null, false, null, null, true);
    }
}
