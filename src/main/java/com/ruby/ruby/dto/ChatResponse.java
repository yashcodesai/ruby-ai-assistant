package com.ruby.ruby.dto;

/**
 * @param response      text RUBY says back
 * @param url           website the browser should open, or null
 * @param stopListening true if the browser should switch voice mode off
 * @param userName      the user's name for this session, or null if not known yet
 */
public record ChatResponse(String response, String url, boolean stopListening, String userName) {

    public static ChatResponse error(String response) {
        return new ChatResponse(response, null, false, null);
    }
}
