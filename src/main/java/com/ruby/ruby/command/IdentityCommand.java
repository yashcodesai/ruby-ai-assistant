package com.ruby.ruby.command;

import com.ruby.ruby.memory.ConversationMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(50)
public class IdentityCommand implements Command {

    static final String CAPABILITIES = "I can check the weather, answer questions like who is "
            + "or tell me about, set timers and reminders, search YouTube and Google, open websites, "
            + "do calculations, tell the time and date, and remember notes for you. "
            + "I can also tell jokes, roll dice and flip coins. Say stop to turn voice mode off.";

    private final String creatorName;

    /** Optional: set ruby.creator-name in application.properties to credit the developer. */
    public IdentityCommand(@Value("${ruby.creator-name:}") String creatorName) {
        this.creatorName = creatorName.trim();
    }

    @Override
    public boolean matches(Message message) {
        return isCreator(message) || isUserName(message) || isHelp(message)
                || message.has("who are you", "what are you", "your name", "how are you",
                "how's it going", "how is it going", "system status");
    }

    @Override
    public Reply execute(Message message, ConversationMemory memory) {
        if (isCreator(message)) {
            return creatorName.isEmpty()
                    ? Reply.say("I was built as a personal voice assistant project.")
                    : Reply.say("I was created by " + creatorName + ".");
        }
        if (isUserName(message)) {
            return memory.userName()
                    .map(name -> Reply.say("Your name is " + name + "."))
                    .orElseGet(() -> {
                        memory.askForName();
                        return Reply.say("I do not know your name yet. What should I call you?");
                    });
        }
        if (isHelp(message)) {
            return Reply.say(CAPABILITIES);
        }
        if (message.has("your name")) {
            return Reply.say("My name is RUBY.");
        }
        if (message.has("who are you", "what are you")) {
            return Reply.say("I am RUBY, your intelligent voice assistant.");
        }
        if (message.has("system status")) {
            return Reply.say("All systems operational. Voice engine active. AI core online.");
        }
        return Reply.say("I am functioning perfectly. Thanks for asking.");
    }

    private static boolean isCreator(Message message) {
        return message.has("who created you", "who made you", "who built you", "who developed you",
                "who programmed you", "who designed you", "your creator", "your maker", "your developer");
    }

    private static boolean isUserName(Message message) {
        return message.has("my name", "who am i");
    }

    private static boolean isHelp(Message message) {
        return message.has("what can you do", "what commands", "your features", "your abilities")
                || message.is("help", "help me", "commands");
    }
}
