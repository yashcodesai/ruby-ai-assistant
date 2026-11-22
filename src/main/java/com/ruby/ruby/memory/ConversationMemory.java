package com.ruby.ruby.memory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

/**
 * Notes, command history and the user's name for one browser session.
 * Thread-safe and size-capped so it cannot grow without limit.
 */
public class ConversationMemory {

    public static final int MAX_HISTORY = 50;
    public static final int MAX_NOTES = 100;

    private final Deque<String> history = new ArrayDeque<>();
    private final List<String> notes = new ArrayList<>();
    private String userName;
    private boolean awaitingName;

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
        notes.add(note);
        return true;
    }

    public synchronized List<String> notes() {
        return List.copyOf(notes);
    }

    public synchronized void clearNotes() {
        notes.clear();
    }
}
