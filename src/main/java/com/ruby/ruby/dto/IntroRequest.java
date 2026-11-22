package com.ruby.ruby.dto;

import java.util.List;

/**
 * Sent when the dashboard loads.
 *
 * @param userName name saved on the device, or null for a new user
 * @param notes    notes saved on the device
 * @param timeZone the browser's IANA timezone, e.g. "Asia/Kolkata"
 */
public record IntroRequest(String userName, List<String> notes, String timeZone) {
}
