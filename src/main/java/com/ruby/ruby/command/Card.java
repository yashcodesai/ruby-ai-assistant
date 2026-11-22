package com.ruby.ruby.command;

import java.util.List;

/**
 * A rich result the dashboard shows as a holographic card
 * (weather, Wikipedia article, ...).
 *
 * @param type     "weather" or "knowledge"; the frontend picks the layout from it
 * @param title    heading, e.g. "New Delhi, India"
 * @param subtitle short line under the title, e.g. "Clear sky"; may be null
 * @param text     body text; may be null
 * @param imageUrl picture to show; may be null
 * @param link     "read more" link; may be null
 * @param facts    label/value pairs, e.g. Humidity: 47%
 */
public record Card(String type, String title, String subtitle, String text,
                   String imageUrl, String link, List<Fact> facts) {

    public record Fact(String label, String value) {
    }
}
