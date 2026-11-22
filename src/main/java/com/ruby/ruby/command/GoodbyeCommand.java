package com.ruby.ruby.command;

import com.ruby.ruby.memory.ConversationMemory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Saying goodbye also switches voice mode off. */
@Component
@Order(80)
public class GoodbyeCommand implements Command {

    @Override
    public boolean matches(Message message) {
        return message.has("bye", "goodbye", "good bye", "good night", "see you", "see ya");
    }

    @Override
    public Reply execute(Message message, ConversationMemory memory) {
        String name = memory.userName().map(n -> " " + n).orElse("");
        return message.has("good night")
                ? Reply.sayAndStop("Good night" + name + ". Sleep well.")
                : Reply.sayAndStop("Goodbye" + name + ". Have a productive day.");
    }
}
