package com.ruby.ruby.command;

import com.ruby.ruby.memory.ConversationMemory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** "search YouTube for lofi music", "play Arijit Singh songs", "google spring boot". */
@Component
@Order(12)
public class SearchCommand implements Command {

    private record Rule(Pattern pattern, String site) {
    }

    private static final String YOUTUBE = "YouTube";
    private static final String GOOGLE = "Google";

    private static final List<Rule> RULES = List.of(
            rule("^(?:search|look up|find)\\s+(?:on\\s+)?you\\s*tube\\s+(?:for\\s+)?(.+)$", YOUTUBE),
            rule("^(?:search|look up|find)\\s+(?:for\\s+)?(.+?)\\s+on\\s+you\\s*tube$", YOUTUBE),
            rule("^play\\s+(.+?)(?:\\s+on\\s+you\\s*tube)?$", YOUTUBE),
            rule("^(?:search|look up)\\s+(?:on\\s+)?google\\s+(?:for\\s+)?(.+)$", GOOGLE),
            rule("^google\\s+(.+)$", GOOGLE),
            rule("^(?:search|look up)\\s+(?:for\\s+)?(.+?)(?:\\s+on\\s+google)?$", GOOGLE));

    private static Rule rule(String regex, String site) {
        return new Rule(Pattern.compile(regex), site);
    }

    @Override
    public boolean matches(Message message) {
        return !message.has("wikipedia") && find(message) != null;
    }

    @Override
    public Reply execute(Message message, ConversationMemory memory) {
        String[] found = find(message);
        String site = found[0];
        String query = found[1];
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = site.equals(YOUTUBE)
                ? "https://www.youtube.com/results?search_query=" + encoded
                : "https://www.google.com/search?q=" + encoded;
        return Reply.open("Searching " + site + " for " + query + ".", url);
    }

    /** @return {site, query} or null */
    private static String[] find(Message message) {
        String text = message.normalized().replaceAll("[.!?]+$", "").trim();
        for (Rule rule : RULES) {
            Matcher matcher = rule.pattern().matcher(text);
            if (matcher.matches()) {
                String query = matcher.group(1).trim();
                if (!query.isEmpty()) {
                    return new String[]{rule.site(), query};
                }
            }
        }
        return null;
    }
}
