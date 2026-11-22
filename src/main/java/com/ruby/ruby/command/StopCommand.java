package com.ruby.ruby.command;

import com.ruby.ruby.memory.ConversationMemory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** "stop" switches voice mode off. Only exact phrases, so "stop watch" etc. are not caught. */
@Component
@Order(5)
public class StopCommand implements Command {

    @Override
    public boolean matches(Message message) {
        return message.is("stop", "stop listening", "stop it", "be quiet", "quiet",
                "shut up", "cancel", "never mind", "nevermind", "that's all", "that is all");
    }

    @Override
    public Reply execute(Message message, ConversationMemory memory) {
        return Reply.sayAndStop("Okay. Voice mode off.");
    }
}
