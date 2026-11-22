package com.ruby.ruby.command;

import com.ruby.ruby.memory.ConversationMemory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(30)
public class NotesCommand implements Command {

    /** Longest first, so "remind me to" wins over "remind me". */
    private static final String[] ADD_PREFIXES = {
            "remind me to", "remind me that", "remember that", "remember to", "remember",
            "make a note that", "make a note of", "make a note", "take a note that", "take a note",
            "add a note", "note that", "note down", "write down"};

    @Override
    public boolean matches(Message message) {
        return message.startsWith(ADD_PREFIXES) || message.has("notes", "note list");
    }

    @Override
    public Reply execute(Message message, ConversationMemory memory) {
        for (String prefix : ADD_PREFIXES) {
            if (message.startsWith(prefix)) {
                return addNote(message.originalAfter(prefix), memory);
            }
        }
        if (message.has("clear", "delete", "erase", "remove", "forget", "wipe")) {
            memory.clearNotes();
            return Reply.say("All notes have been cleared.");
        }
        return readNotes(memory.notes());
    }

    private static Reply addNote(String text, ConversationMemory memory) {
        String note = text.replaceFirst("^[:,\\s]+", "").replaceFirst("[.!]+$", "").trim();
        if (note.isEmpty()) {
            return Reply.say("What should I remember?");
        }
        return memory.addNote(note)
                ? Reply.say("Got it. I will remember: " + note + ".")
                : Reply.say("My notes are full. Say clear notes to make room.");
    }

    private static Reply readNotes(List<String> notes) {
        if (notes.isEmpty()) {
            return Reply.say("You have no saved notes.");
        }
        StringBuilder text = new StringBuilder(
                notes.size() == 1 ? "You have 1 note: " : "You have " + notes.size() + " notes: ");
        for (int i = 0; i < notes.size(); i++) {
            text.append(i + 1).append(". ").append(notes.get(i)).append(". ");
        }
        return Reply.say(text.toString().trim());
    }
}
