package com.ruby.ruby.command;

import com.ruby.ruby.memory.ConversationMemory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(70)
public class SmallTalkCommand implements Command {

    @Override
    public boolean matches(Message message) {
        return message.has("thank you", "thanks", "thank u", "hear me", "weather", "temperature outside",
                "nice day", "good day");
    }

    @Override
    public Reply execute(Message message, ConversationMemory memory) {
        if (message.has("hear me")) {
            return Reply.say("Yes, I can hear you clearly.");
        }
        if (message.has("weather", "temperature outside")) {
            return Reply.say("I cannot check the weather yet. Try saying open Google.");
        }
        if (message.has("nice day", "good day")) {
            return Reply.say("Thank you. You too.");
        }
        return Reply.say("You're welcome.");
    }
}
