package com.ruby.ruby.command;

import com.ruby.ruby.integration.Article;
import com.ruby.ruby.integration.KnowledgeService;
import com.ruby.ruby.integration.ServiceUnavailableException;
import com.ruby.ruby.memory.ConversationMemory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * "who is APJ Abdul Kalam", "tell me about the Taj Mahal", "what is photosynthesis".
 * Runs after every other command, so questions they understand never reach Wikipedia.
 */
@Component
@Order(85)
public class KnowledgeCommand implements Command {

    private static final Pattern QUESTION = Pattern.compile(
            "^(?:tell me (?:something )?about|who (?:is|was|are|were)|what (?:is|are|was|were)"
                    + "|what's|whats|define|explain|search wikipedia for|wikipedia)\\s+(.+?)[?.!]*$",
            Pattern.CASE_INSENSITIVE);

    /** Too vague to look up. */
    private static final Set<String> VAGUE = Set.of(
            "this", "that", "it", "up", "new", "going on", "happening", "wrong", "you", "me",
            "he", "she", "they", "there", "here", "a", "an", "the", "your name", "my name");

    /** Spoken answers stay short; the card shows more. */
    private static final int SPOKEN_SENTENCES = 2;
    private static final int CARD_LENGTH = 600;

    private final KnowledgeService knowledge;

    public KnowledgeCommand(KnowledgeService knowledge) {
        this.knowledge = knowledge;
    }

    @Override
    public boolean matches(Message message) {
        String topic = topicOf(message);
        return topic != null;
    }

    @Override
    public Reply execute(Message message, ConversationMemory memory) {
        String topic = topicOf(message);
        try {
            return knowledge.lookup(topic)
                    .map(KnowledgeCommand::describe)
                    .orElse(Reply.say("I could not find anything about " + topic
                            + ". Try saying it a different way."));
        } catch (ServiceUnavailableException e) {
            return Reply.say("I could not reach Wikipedia right now. Please check the internet connection.");
        }
    }

    static String topicOf(Message message) {
        Matcher matcher = QUESTION.matcher(message.original());
        if (!matcher.matches()) {
            return null;
        }
        String topic = matcher.group(1).trim();
        String lower = topic.toLowerCase();
        if (topic.length() < 2 || topic.length() > 80 || VAGUE.contains(lower) || lower.matches("[\\d\\s.]+")) {
            return null;
        }
        return topic;
    }

    private static Reply describe(Article article) {
        String spoken = firstSentences(article.summary(), SPOKEN_SENTENCES);
        String cardText = article.summary().length() <= CARD_LENGTH
                ? article.summary()
                : article.summary().substring(0, CARD_LENGTH).replaceFirst("\\s+\\S*$", "") + "…";
        Card card = new Card("knowledge", article.title(), "Wikipedia", cardText,
                article.imageUrl(), article.pageUrl(), List.of());
        return Reply.withCard(spoken, card);
    }

    /** Splits on sentence ends, but not on initials like "A. P. J." or "Dr." */
    static String firstSentences(String text, int count) {
        String[] parts = text.split("(?<=[a-z0-9)\\]][.!?])\\s+(?=[A-Z])");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < Math.min(count, parts.length); i++) {
            if (i > 0) {
                result.append(' ');
            }
            result.append(parts[i].trim());
        }
        return result.toString();
    }
}
