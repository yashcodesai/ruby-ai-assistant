package com.ruby.ruby.integration;

import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

import java.time.Clock;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;

/** Wikipedia search + page summary APIs: free, no API key. */
@Service
public class WikipediaKnowledgeService implements KnowledgeService {

    private final RestClient http;
    private final TtlCache<String, Optional<Article>> cache;

    public WikipediaKnowledgeService(RestClient http, Clock clock) {
        this.http = http;
        this.cache = new TtlCache<>(Duration.ofMinutes(30), clock);
    }

    @Override
    public Optional<Article> lookup(String topic) {
        return cache.get(topic.trim().toLowerCase(Locale.ROOT), this::fetch);
    }

    private Optional<Article> fetch(String topic) {
        try {
            JsonNode search = http.get()
                    .uri("https://en.wikipedia.org/w/api.php?action=query&list=search"
                            + "&srlimit=1&format=json&srsearch={q}", topic)
                    .retrieve()
                    .body(JsonNode.class);
            JsonNode hit = search == null ? null : search.path("query").path("search").path(0);
            if (hit == null || hit.isMissingNode()) {
                return Optional.empty();
            }
            String title = hit.path("title").asString();

            JsonNode page = http.get()
                    .uri("https://en.wikipedia.org/api/rest_v1/page/summary/{title}", title.replace(' ', '_'))
                    .retrieve()
                    .body(JsonNode.class);
            if (page == null || "disambiguation".equals(page.path("type").asString(""))) {
                return Optional.empty();
            }
            String summary = page.path("extract").asString("");
            if (summary.isBlank()) {
                return Optional.empty();
            }

            JsonNode thumbnail = page.path("thumbnail").path("source");
            return Optional.of(new Article(
                    page.path("title").asString(title),
                    summary,
                    thumbnail.isMissingNode() ? null : thumbnail.asString(),
                    page.path("content_urls").path("desktop").path("page")
                            .asString("https://en.wikipedia.org/wiki/" + title.replace(' ', '_'))));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (RestClientException e) {
            throw new ServiceUnavailableException("Wikipedia unavailable", e);
        }
    }
}
