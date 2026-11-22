package com.ruby.ruby.command;

/**
 * A countdown the browser runs; when it ends RUBY says {@code doneText}.
 *
 * @param seconds  length of the countdown
 * @param label    shown on the timer card, e.g. "5 min timer" or "Drink water"
 * @param doneText what RUBY says when time is up
 */
public record TimerSpec(int seconds, String label, String doneText) {
}
