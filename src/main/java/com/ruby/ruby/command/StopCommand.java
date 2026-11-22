package com.ruby.ruby.command;

import com.ruby.ruby.memory.ConversationMemory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;

/**
 * "stop" switches voice mode off. Speech recognition often repeats words
 * ("stop stop", "stop it stop"), so any phrase made only of stop words counts,
 * while "stop the timer" (other words) is left to the timer command.
 */
@Component
@Order(5)
public class StopCommand implements Command {

    private static final Set<String> STOP_WORDS = Set.of(
            "stop", "cancel", "quiet", "listening", "it", "please", "now", "be", "ruby", "ok", "okay");

    private static final Set<String> TRIGGERS = Set.of("stop", "cancel", "quiet");

    @Override
    public boolean matches(Message message) {
        return message.is("shut up", "never mind", "nevermind", "that's all", "that is all")
                || onlyStopWords(message);
    }

    @Override
    public Reply execute(Message message, ConversationMemory memory) {
        return Reply.sayAndStop("Okay. Voice mode off.");
    }

    private static boolean onlyStopWords(Message message) {
        String[] words = message.normalized().replaceAll("[^a-z' ]", " ").trim().split("\\s+");
        return Arrays.stream(words).allMatch(STOP_WORDS::contains)
                && Arrays.stream(words).anyMatch(TRIGGERS::contains);
    }
}
