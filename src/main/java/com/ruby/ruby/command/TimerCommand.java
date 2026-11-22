package com.ruby.ruby.command;

import com.ruby.ruby.memory.ConversationMemory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.OptionalInt;

/**
 * "set a timer for 5 minutes", "remind me in 2 minutes to drink water",
 * "cancel my timers". The browser runs the countdown; RUBY speaks when it ends.
 * Runs before NotesCommand so "remind me to X in 5 minutes" is a reminder,
 * while "remind me to X" (no time) is still saved as a note.
 */
@Component
@Order(25)
public class TimerCommand implements Command {

    @Override
    public boolean matches(Message message) {
        return isCancel(message) || isAlarm(message)
                || (isTimerWord(message) && Durations.parseSeconds(message.normalized()).isPresent());
    }

    @Override
    public Reply execute(Message message, ConversationMemory memory) {
        if (isCancel(message)) {
            return Reply.cancelTimers("All timers and reminders are cancelled.");
        }
        OptionalInt seconds = Durations.parseSeconds(message.normalized());
        if (seconds.isEmpty()) {
            // e.g. "set an alarm for 7 am"
            return Reply.say("I can set timers for a length of time, like: set a timer for 10 minutes.");
        }

        int length = seconds.getAsInt();
        String duration = Durations.describe(length);
        String name = memory.userName().map(n -> n + ", ").orElse("");

        if (message.startsWith("remind me")) {
            String task = reminderTask(message.original());
            if (!task.isEmpty()) {
                return Reply.startTimer("Okay. I will remind you to " + task + " in " + duration + ".",
                        new TimerSpec(length, capitalize(task),
                                capitalize(name + "this is your reminder to " + task + ".")));
            }
            return Reply.startTimer("Okay. I will remind you in " + duration + ".",
                    new TimerSpec(length, "Reminder", capitalize(name + "this is your reminder.")));
        }
        String adjective = Durations.describeAsAdjective(length);
        return Reply.startTimer("Timer set for " + duration + ".",
                new TimerSpec(length, capitalize(adjective + " timer"),
                        capitalize(name + "your " + adjective + " timer is done.")));
    }

    private static boolean isTimerWord(Message message) {
        return message.has("timer", "countdown", "count down", "alarm")
                || message.startsWith("remind me");
    }

    private static boolean isAlarm(Message message) {
        return message.has("set an alarm", "set alarm", "wake me");
    }

    private static boolean isCancel(Message message) {
        return message.has("cancel", "stop", "clear", "delete", "remove")
                && message.has("timer", "timers", "reminder", "reminders", "alarm", "alarms");
    }

    /** "remind me in 2 minutes to call my mom" -> "call your mom". */
    static String reminderTask(String original) {
        String task = original
                .replaceFirst("(?i)^remind me\\b", "")
                .replaceAll("(?i)\\b(?:in|after|for)\\s+" + Durations.PHRASE + "\\b", "")
                .replaceAll("(?i)\\b" + Durations.PHRASE + "\\b\\s*(?:from now|later)?", "")
                .replaceFirst("(?i)^\\s*(?:to|that|about)\\b", "")
                .replaceAll("[.!?,]+$", "")
                .replaceAll("\\s+", " ")
                .trim();
        return task
                .replaceAll("(?i)\\bmy\\b", "your")
                .replaceAll("(?i)\\bmyself\\b", "yourself")
                .replaceAll("(?i)\\bme\\b", "you")
                .replaceAll("\\bI\\b", "you");
    }

    private static String capitalize(String text) {
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
