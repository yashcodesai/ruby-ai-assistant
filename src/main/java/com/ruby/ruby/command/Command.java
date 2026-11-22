package com.ruby.ruby.command;

import com.ruby.ruby.memory.ConversationMemory;

/**
 * One thing RUBY knows how to do. Commands are Spring beans, tried in
 * {@link org.springframework.core.annotation.Order} order; the first
 * one that matches handles the message.
 */
public interface Command {

    boolean matches(Message message);

    /** Override when matching depends on the conversation (e.g. a pending question). */
    default boolean matches(Message message, ConversationMemory memory) {
        return matches(message);
    }

    Reply execute(Message message, ConversationMemory memory);
}
