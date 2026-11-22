package com.ruby.ruby.integration;

import com.ruby.ruby.integration.Places.Place;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Open-Meteo: free, no API key. https://open-meteo.com */
@Service
public class OpenMeteoWeatherService implements WeatherService {

    private static final int RESULTS = 10;

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

    private Optional<WeatherReport> fetch(String query) {
        try {
            return findPlace(query).map(this::weatherAt);
        } catch (RestClientException e) {
            throw new ServiceUnavailableException("Weather service unavailable", e);
        }
    }

    /**
     * "manali" -> best match; "manali, himachal pradesh" or "manali himachal pradesh"
     * -> the Manali whose region matches the extra words.
     */
    private Optional<Places.Choice> findPlace(String query) {
        String[] parts = query.split(",", 2);
        String name = parts[0].trim();
        String qualifier = parts.length > 1 ? parts[1].trim() : "";

        List<Place> results = search(name);
        if (!qualifier.isEmpty()) {
            results = Places.filter(results, qualifier);
        } else if (results.isEmpty()) {
            // The geocoder only matches names, so try shorter names with the rest as a qualifier.
            String[] words = name.split("\\s+");
            for (int k = words.length - 1; k >= 1 && results.isEmpty(); k--) {
                String shorter = String.join(" ", Arrays.copyOfRange(words, 0, k));
                String rest = String.join(" ", Arrays.copyOfRange(words, k, words.length));
                results = Places.filter(search(shorter), rest);
            }
        }
        return Places.choose(results);
    }

    private List<Place> search(String name) {
        JsonNode body = http.get()
                .uri("https://geocoding-api.open-meteo.com/v1/search?count={count}&language=en&name={name}",
                        RESULTS, name)
                .retrieve()
                .body(JsonNode.class);
        List<Place> places = new ArrayList<>();
        if (body != null) {
            for (JsonNode r : body.path("results")) {
                places.add(new Place(
                        r.path("name").asString(name),
                        r.path("admin1").asString(""),
                        r.path("admin2").asString(""),
                        r.path("country").asString(""),
                        r.path("population").asLong(0),
                        r.path("latitude").asDouble(),
                        r.path("longitude").asDouble()));
            }
        }
        return places;
    }

    private WeatherReport weatherAt(Places.Choice choice) {
        Place place = choice.place();
        JsonNode weather = http.get()
                .uri("https://api.open-meteo.com/v1/forecast?latitude={lat}&longitude={lon}"
                                + "&current=temperature_2m,relative_humidity_2m,apparent_temperature,"
                                + "weather_code,wind_speed_10m,is_day",
                        place.latitude(), place.longitude())
                .retrieve()
                .body(JsonNode.class);
        JsonNode now = weather == null ? null : weather.path("current");
        if (now == null || now.isMissingNode()) {
            throw new ServiceUnavailableException("No current weather in response", null);
        }
        return new WeatherReport(
                place.name(), place.region(), place.country(), choice.alternativeRegion(),
                place.latitude(), place.longitude(),
                now.path("temperature_2m").asDouble(),
                now.path("apparent_temperature").asDouble(),
                now.path("relative_humidity_2m").asInt(),
                now.path("wind_speed_10m").asDouble(),
                now.path("weather_code").asInt(-1),
                now.path("is_day").asInt(1) == 1);
    }
}
