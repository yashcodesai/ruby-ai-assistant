package com.ruby.ruby.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageTest {

    @Test
    void matchesWholeWordsOnly() {
        assertTrue(Message.of("Hi there").has("hi"));
        assertTrue(Message.of("hi!").has("hi"));
        assertFalse(Message.of("what is this").has("hi"));
        assertFalse(Message.of("show my history").has("hi"));
        assertFalse(Message.of("weather in delhi").has("hi"));
        assertFalse(Message.of("when is my birthday").has("day"));
    }

    @Test
    void matchesPhrasesWithFlexibleSpacing() {
        assertTrue(Message.of("please show   notes").has("show notes"));
    }

    @Test
    void keepsOriginalCaseAfterPrefix() {
        assertEquals("Call Mom at 5", Message.of("Remember Call Mom at 5").originalAfter("remember"));
    }
}
