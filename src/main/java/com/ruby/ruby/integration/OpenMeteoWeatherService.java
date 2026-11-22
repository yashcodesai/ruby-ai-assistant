package com.ruby.ruby.integration;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

import java.time.Clock;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;

/** Open-Meteo: free, no API key. https://open-meteo.com */
@Service
public class OpenMeteoWeatherService implements WeatherService {

    private final RestClient http;
    private final TtlCache<String, Optional<WeatherReport>> cache;

    public OpenMeteoWeatherService(RestClient http, Clock clock) {
        this.http = http;
        this.cache = new TtlCache<>(Duration.ofMinutes(10), clock);
    }

    @Override
    public Optional<WeatherReport> current(String place) {
        return cache.get(place.trim().toLowerCase(Locale.ROOT), this::fetch);
    }

    private Optional<WeatherReport> fetch(String place) {
        try {
            JsonNode places = http.get()
                    .uri("https://geocoding-api.open-meteo.com/v1/search?count=1&language=en&name={name}", place)
                    .retrieve()
                    .body(JsonNode.class);
            JsonNode match = places == null ? null : places.path("results").path(0);
            if (match == null || match.isMissingNode()) {
                return Optional.empty();
            }

            JsonNode weather = http.get()
                    .uri("https://api.open-meteo.com/v1/forecast?latitude={lat}&longitude={lon}"
                                    + "&current=temperature_2m,relative_humidity_2m,apparent_temperature,"
                                    + "weather_code,wind_speed_10m,is_day",
                            match.path("latitude").asDouble(), match.path("longitude").asDouble())
                    .retrieve()
                    .body(JsonNode.class);
            JsonNode now = weather == null ? null : weather.path("current");
            if (now == null || now.isMissingNode()) {
                throw new ServiceUnavailableException("No current weather in response", null);
            }

            return Optional.of(new WeatherReport(
                    match.path("name").asString(place),
                    match.path("country").asString(""),
                    now.path("temperature_2m").asDouble(),
                    now.path("apparent_temperature").asDouble(),
                    now.path("relative_humidity_2m").asInt(),
                    now.path("wind_speed_10m").asDouble(),
                    now.path("weather_code").asInt(-1),
                    now.path("is_day").asInt(1) == 1));
        } catch (RestClientException e) {
            throw new ServiceUnavailableException("Weather service unavailable", e);
        }
    }
}
