package com.ruby.ruby.command;

import com.ruby.ruby.memory.ConversationMemory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Only answers real time/date/day questions: "have a nice day" or
 * "what's the weather today" must not get "Today is Sunday".
 */
@Component
@Order(60)
public class DateTimeCommand implements Command {

    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter DAY =
            DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH);

    private final Clock clock;

    public DateTimeCommand(Clock clock) {
        this.clock = clock;
    }

    @Override
    public boolean matches(Message message) {
        return asksTime(message) || asksDate(message) || asksDay(message)
                || message.has("date and time", "time and date");
    }

    @Override
    public Reply execute(Message message, ConversationMemory memory) {
        // The user's timezone, not the server's: they may be far apart once hosted.
        ZonedDateTime now = ZonedDateTime.now(clock.withZone(memory.zone()));
        String time = now.format(TIME);
        String date = now.format(DATE);
        if (message.has("date and time", "time and date")
                || (asksTime(message) && asksDate(message))) {
            return Reply.say("It is " + time + " on " + date + ".");
        }
        if (asksTime(message)) {
            return Reply.say("The current time is " + time + ".");
        }
        if (asksDate(message)) {
            return Reply.say("Today's date is " + date + ".");
        }
        return Reply.say("Today is " + now.format(DAY) + ".");
    }

    private static boolean asksTime(Message m) {
        return m.has("what time", "the time", "time is it", "current time", "time now", "what's the time")
                || m.is("time");
    }

    private static boolean asksDate(Message m) {
        return m.has("what date", "the date", "today's date", "date today", "date is it", "current date")
                || m.is("date");
    }

    private static boolean asksDay(Message m) {
        return m.has("what day", "which day", "day is it", "day today", "day of the week",
                "what is today", "what's today")
                || m.is("day", "today");
    }
}
