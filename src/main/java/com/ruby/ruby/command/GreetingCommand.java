package com.ruby.ruby.command;

import com.ruby.ruby.memory.ConversationMemory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Runs last, so "hi, what time is it" answers the time instead of just greeting. */
@Component
@Order(90)
public class GreetingCommand implements Command {

    @Override
    public boolean matches(Message message) {
        return message.has("hello", "hi", "hey", "good morning", "good afternoon", "good evening")
                || message.is("ruby", "yo", "namaste");
    }

    @Override
    public Reply execute(Message message, ConversationMemory memory) {
        return memory.userName()
                .map(name -> Reply.say("Hello " + name + ". How can I assist you today?"))
                .orElseGet(() -> {
                    memory.askForName();
                    return Reply.say("Hello! I am RUBY. What is your name?");
                });
    }
}
