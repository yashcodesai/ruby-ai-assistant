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
 * @param location place the card is about, shown on the holographic globe; may be null
 */
public record Card(String type, String title, String subtitle, String text,
                   String imageUrl, String link, List<Fact> facts, Location location) {

    public record Fact(String label, String value) {
    }

    /** @param label short name for the globe marker, e.g. "London" */
    public record Location(double latitude, double longitude, String label) {
    }
}
