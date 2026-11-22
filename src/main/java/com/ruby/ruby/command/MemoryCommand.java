package com.ruby.ruby.command;

import com.ruby.ruby.memory.ConversationMemory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(40)
public class MemoryCommand implements Command {

    @Override
    public boolean matches(Message message) {
        return message.has("what did i ask", "what did i say", "what was my last",
                "my last command", "my last question", "my previous command", "memory status");
    }

    @Override
    public Reply execute(Message message, ConversationMemory memory) {
        if (message.has("memory status")) {
            return Reply.say("I currently remember " + memory.notes().size()
                    + " notes and " + memory.commandCount() + " commands.");
        }
        return memory.lastCommand()
                .map(previous -> Reply.say("Your previous command was: " + previous))
                .orElse(Reply.say("You have not asked me anything before this."));
    }
}
