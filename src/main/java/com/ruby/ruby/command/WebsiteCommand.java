package com.ruby.ruby.command;

import com.ruby.ruby.memory.ConversationMemory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Order(10)
public class WebsiteCommand implements Command {

    /**
     * Add a site here and it works by voice; the frontend needs no change.
     * {@code spoken} lists how speech recognition may write the name.
     */
    record Site(String name, String url, String spoken) {
    }

    static final List<Site> SITES = List.of(
            new Site("Google", "https://www.google.com", "google"),
            new Site("YouTube", "https://www.youtube.com", "you\\s*tube"),
            new Site("GitHub", "https://github.com", "git\\s*hub"),
            new Site("ChatGPT", "https://chatgpt.com", "chat\\s*g\\.?\\s*p\\.?\\s*t\\.?"));

    private static final Pattern OPEN_VERB = Pattern.compile("\\b(?:open|launch|go to|start)\\s+");

    @Override
    public boolean matches(Message message) {
        return find(message) != null;
    }

    @Override
    public Reply execute(Message message, ConversationMemory memory) {
        Site site = find(message);
        return Reply.open("Opening " + site.name() + ".", site.url());
    }

    private static Site find(Message message) {
        Matcher verb = OPEN_VERB.matcher(message.normalized());
        if (!verb.find()) {
            return null;
        }
        String rest = message.normalized().substring(verb.end());
        for (Site site : SITES) {
            if (Pattern.compile("^(?:the\\s+)?" + site.spoken() + "(?:\\.com)?\\b").matcher(rest).find()) {
                return site;
            }
        }
        return null;
    }
}
