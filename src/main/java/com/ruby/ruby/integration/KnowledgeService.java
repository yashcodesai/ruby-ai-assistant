package com.ruby.ruby.integration;

import java.util.Optional;

public interface KnowledgeService {

    /**
     * @return empty if nothing suitable was found
     * @throws ServiceUnavailableException if the service cannot be reached
     */
    Optional<Article> lookup(String topic);
}
