package com.ruby.ruby.command;

import com.ruby.ruby.memory.ConversationMemory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Before DateTimeCommand, so "tell me a joke one more time" is a joke, not the time. */
@Component
@Order(55)
public class FunCommand implements Command {

    private static final List<String> JOKES = List.of(
            "Why do programmers prefer dark mode? Because light attracts bugs.",
            "There are 10 kinds of people: those who understand binary and those who don't.",
            "A SQL query walks into a bar, walks up to two tables and asks: can I join you?");

    private static final List<String> QUOTES = List.of(
            "Success is the sum of small efforts repeated day after day.",
            "The secret of getting ahead is getting started.",
            "It always seems impossible until it is done.");

    @Override
    public boolean matches(Message message) {
        return isJoke(message) || isMotivation(message) || isDice(message)
                || isCoin(message) || isRandomNumber(message);
    }

    @Override
    public Reply execute(Message message, ConversationMemory memory) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (isJoke(message)) {
            return Reply.say(JOKES.get(random.nextInt(JOKES.size())));
        }
        if (isMotivation(message)) {
            return Reply.say(QUOTES.get(random.nextInt(QUOTES.size())));
        }
        if (isDice(message)) {
            return Reply.say("You rolled " + random.nextInt(1, 7) + ".");
        }
        if (isRandomNumber(message)) {
            return Reply.say("Your random number is " + random.nextInt(1, 101) + ".");
        }
        return Reply.say(random.nextBoolean() ? "Heads." : "Tails.");
    }

    private static boolean isJoke(Message m) {
        return m.has("joke", "make me laugh", "something funny");
    }

    private static boolean isMotivation(Message m) {
        return m.has("motivate me", "motivation", "motivational", "inspire me", "inspiration", "quote");
    }

    private static boolean isDice(Message m) {
        return m.has("roll", "throw") && m.has("dice", "die");
    }

    private static boolean isCoin(Message m) {
        return (m.has("flip", "toss") && m.has("coin")) || m.has("heads or tails");
    }

    private static boolean isRandomNumber(Message m) {
        return m.has("random number", "pick a number");
    }
}
