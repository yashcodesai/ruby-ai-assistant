package com.ruby.ruby.service;

import com.ruby.ruby.command.Command;
import com.ruby.ruby.command.Message;
import com.ruby.ruby.command.NameCommand;
import com.ruby.ruby.command.Names;
import com.ruby.ruby.command.Reply;
import com.ruby.ruby.memory.ConversationMemory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RubyService {

    static final String FALLBACK =
            "I am still learning. I do not know how to answer that yet.";

    /** Injected in @Order order. */
    private final List<Command> commands;

    public RubyService(List<Command> commands) {
        this.commands = commands;
    }

    public Reply process(String text, ConversationMemory memory) {
        Message message = Message.of(text);
        Reply reply = commands.stream()
                .filter(command -> command.matches(message, memory))
                .findFirst()
                .map(command -> command.execute(message, memory))
                .orElseGet(() -> answerToNameQuestion(message, memory)
                        .orElse(Reply.say(FALLBACK)));
        // Recorded after handling so "what did i ask before" sees the previous command.
        // Store what the user actually said, not the filler-stripped form.
        memory.recordCommand(text.trim());
        return reply;
    }

    /** Opening line when the page loads: asks for the name if RUBY does not know it yet. */
    public Reply introduce(ConversationMemory memory) {
        return memory.userName()
                .map(name -> Reply.say("Welcome back, " + name + ". How can I help you?"))
                .orElseGet(() -> {
                    memory.askForName();
                    return Reply.say("Hello, I am RUBY, your voice assistant. What is your name?");
                });
    }

    /**
     * After RUBY asks for a name, a reply no command understood (e.g. "Priya")
     * is taken as the name. Commands still work while the question is pending.
     */
    private static java.util.Optional<Reply> answerToNameQuestion(Message message, ConversationMemory memory) {
        if (!memory.awaitingName()) {
            return java.util.Optional.empty();
        }
        return Names.fromBareAnswer(message).map(name -> NameCommand.greetNewName(name, memory));
    }
}
