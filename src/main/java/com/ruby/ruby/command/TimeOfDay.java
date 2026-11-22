package com.ruby.ruby.command;

import java.time.ZonedDateTime;

/** "Good morning" / "Good afternoon" / "Good evening" in the user's own timezone. */
public final class TimeOfDay {

    private TimeOfDay() {
    }

    public static String greeting(ZonedDateTime now) {
        int hour = now.getHour();
        if (hour >= 5 && hour < 12) {
            return "Good morning";
        }
        if (hour >= 12 && hour < 17) {
            return "Good afternoon";
        }
        return "Good evening";
    }
}
