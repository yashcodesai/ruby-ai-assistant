package com.ruby.ruby.dto;

/**
 * @param message  what the user said or typed
 * @param timeZone the browser's IANA timezone, e.g. "Asia/Kolkata"; optional
 */
public record ChatRequest(String message, String timeZone) {
}
