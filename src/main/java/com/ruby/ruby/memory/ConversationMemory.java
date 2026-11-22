package com.ruby.ruby.memory;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

/**
 * Notes, command history, the user's name and timezone for one browser session.
 * Thread-safe and size-capped so it cannot grow without limit.
 *
 * The browser also keeps the name and notes in its own storage and sends them
 * back with {@code /api/intro}, so they survive server restarts and work the
 * same way in the mobile app.
 */
public class ConversationMemory {

    public static final int MAX_HISTORY = 50;
    public static final int MAX_NOTES = 100;
    public static final int MAX_NOTE_LENGTH = 300;

    private final Deque<String> history = new ArrayDeque<>();
    private final List<String> notes = new ArrayList<>();
    private String userName;
    private boolean awaitingName;
    private ZoneId zone = ZoneId.systemDefault();

    /** The name the user told RUBY, if any. */
    public synchronized Optional<String> userName() {
        return Optional.ofNullable(userName);
    }

    public synchronized void setUserName(String name) {
        userName = name;
        awaitingName = false;
    }

    /** RUBY has just asked "what is your name?" and expects an answer next. */
    public synchronized boolean awaitingName() {
        return awaitingName;
    }

    public synchronized void askForName() {
        awaitingName = true;
    }

    /** The user's timezone, so "what time is it" is right even on a server abroad. */
    public synchronized ZoneId zone() {
        return zone;
    }

    /** Ignores missing or invalid ids such as "Mars/Base". */
    public synchronized void setZone(String zoneId) {
        if (zoneId == null || zoneId.isBlank()) {
            return;
        }
        try {
            zone = ZoneId.of(zoneId);
        } catch (DateTimeException e) {
            // keep the previous zone
        }
    }

    public synchronized void recordCommand(String command) {
        history.addLast(command);
        while (history.size() > MAX_HISTORY) {
            history.removeFirst();
        }
    }

    /** The most recently recorded command (the one before the current message). */
    public synchronized Optional<String> lastCommand() {
        return Optional.ofNullable(history.peekLast());
    }

    public synchronized int commandCount() {
        return history.size();
    }

    /** @return false if the notes list is full */
    public synchronized boolean addNote(String note) {
        if (notes.size() >= MAX_NOTES) {
            return false;
        }
        notes.add(truncate(note));
        return true;
    }

    public synchronized List<String> notes() {
        return List.copyOf(notes);
    }

    public synchronized void clearNotes() {
        notes.clear();
    }

    /** Loads notes saved on the device; only used when this session has none yet. */
    public synchronized void restoreNotes(List<String> saved) {
        if (saved == null || !notes.isEmpty()) {
            return;
        }
        saved.stream()
                .filter(note -> note != null && !note.isBlank())
                .limit(MAX_NOTES)
                .map(ConversationMemory::truncate)
                .forEach(notes::add);
    }

    private static String truncate(String note) {
        String trimmed = note.trim();
        return trimmed.length() <= MAX_NOTE_LENGTH ? trimmed : trimmed.substring(0, MAX_NOTE_LENGTH);
    }
}
