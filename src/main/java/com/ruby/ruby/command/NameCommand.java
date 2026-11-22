package com.ruby.ruby.command;

import com.ruby.ruby.memory.ConversationMemory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * "my name is Priya" / "call me Priya" at any time, and "I'm Priya" right
 * after RUBY asked. A bare "Priya" is handled in RubyService, because it
 * must only count as a name when no other command understood it.
 */
@Component
@Order(1)
public class NameCommand implements Command {

    @Override
    public boolean matches(Message message) {
        return Names.EXPLICIT.matcher(message.original()).matches();
    }

    @Override
    public boolean matches(Message message, ConversationMemory memory) {
        return matches(message)
                || (memory.awaitingName() && Names.INTRODUCTION.matcher(message.original()).matches());
    }

    @Override
    public Reply execute(Message message, ConversationMemory memory) {
        Optional<String> name = Names.fromExplicit(message).or(() -> Names.fromIntroduction(message));
        if (name.isEmpty()) {
            memory.askForName();
            return Reply.say("Sorry, I did not catch your name. What should I call you?");
        }
        return greetNewName(name.get(), memory);
    }

    public static Reply greetNewName(String name, ConversationMemory memory) {
        memory.setUserName(name);
        return Reply.say("Nice to meet you, " + name + ". How can I help you today?");
    }
}
