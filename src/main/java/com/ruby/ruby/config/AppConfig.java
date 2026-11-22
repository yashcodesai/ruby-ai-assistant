package com.ruby.ruby.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;

@Configuration
public class AppConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    /**
     * Shared HTTP client for online services. Short timeouts so a slow
     * network makes RUBY say "I could not reach..." instead of hanging.
     */
    @Bean
    public RestClient restClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(4));
        factory.setReadTimeout(Duration.ofSeconds(6));
        return RestClient.builder()
                .requestFactory(factory)
                // Wikipedia asks API clients to identify themselves.
                .defaultHeader("User-Agent", "RUBY-Assistant/2.0 (personal voice assistant project)")
                .build();
    }
}
