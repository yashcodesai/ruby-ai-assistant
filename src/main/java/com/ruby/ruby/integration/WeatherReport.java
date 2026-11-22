package com.ruby.ruby.integration;

/**
 * Current weather for a place.
 *
 * @param city        e.g. "New Delhi"
 * @param country     e.g. "India"
 * @param temperature degrees Celsius
 * @param feelsLike   degrees Celsius
 * @param humidity    percent
 * @param windKmh     km/h
 * @param code        WMO weather code
 * @param day         true in daytime
 */
public record WeatherReport(String city, String country, double temperature, double feelsLike,
                            int humidity, double windKmh, int code, boolean day) {

    /** Plain-English description of the WMO weather code. */
    public String description() {
        return switch (code) {
            case 0 -> day ? "Clear sky" : "Clear night";
            case 1 -> "Mainly clear";
            case 2 -> "Partly cloudy";
            case 3 -> "Overcast";
            case 45, 48 -> "Foggy";
            case 51, 53, 55 -> "Drizzle";
            case 56, 57 -> "Freezing drizzle";
            case 61 -> "Light rain";
            case 63 -> "Rain";
            case 65 -> "Heavy rain";
            case 66, 67 -> "Freezing rain";
            case 71 -> "Light snow";
            case 73 -> "Snow";
            case 75, 77 -> "Heavy snow";
            case 80, 81 -> "Rain showers";
            case 82 -> "Violent rain showers";
            case 85, 86 -> "Snow showers";
            case 95 -> "Thunderstorm";
            case 96, 99 -> "Thunderstorm with hail";
            default -> "Unknown conditions";
        };
    }
}
