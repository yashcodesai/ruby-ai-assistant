package com.ruby.ruby.command;

import com.ruby.ruby.integration.ServiceUnavailableException;
import com.ruby.ruby.integration.WeatherReport;
import com.ruby.ruby.integration.WeatherService;
import com.ruby.ruby.memory.ConversationMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** "what's the weather", "weather in Mumbai", "is it raining in London". */
@Component
@Order(58)
public class WeatherCommand implements Command {

    private static final Pattern PLACE = Pattern.compile(
            "\\b(?:in|at|for|of)\\s+([a-z][a-z .'-]*?)"
                    + "(?:\\s+(?:today|now|right now|currently|tonight|please))?\\s*[?.!]*$");

    private final WeatherService weather;
    private final String defaultCity;

    public WeatherCommand(WeatherService weather,
                          @Value("${ruby.weather.default-city:New Delhi}") String defaultCity) {
        this.weather = weather;
        this.defaultCity = defaultCity;
    }

    @Override
    public boolean matches(Message message) {
        return message.has("weather", "temperature", "forecast", "raining", "is it hot", "is it cold",
                "humidity", "how hot", "how cold");
    }

    @Override
    public Reply execute(Message message, ConversationMemory memory) {
        String place = placeIn(message).orElse(defaultCity);
        try {
            return weather.current(place)
                    .map(WeatherCommand::describe)
                    .orElse(Reply.say("I could not find a place called " + titleCase(place) + "."));
        } catch (ServiceUnavailableException e) {
            return Reply.say("I could not reach the weather service right now. "
                    + "Please check the internet connection.");
        }
    }

    private static String titleCase(String text) {
        StringBuilder result = new StringBuilder();
        for (String word : text.split("\\s+")) {
            if (!result.isEmpty()) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }

    static Optional<String> placeIn(Message message) {
        Matcher matcher = PLACE.matcher(message.normalized());
        if (!matcher.find()) {
            return Optional.empty();
        }
        String place = matcher.group(1).trim();
        return place.isEmpty() || place.equals("the day") || place.equals("today")
                ? Optional.empty() : Optional.of(place);
    }

    private static Reply describe(WeatherReport report) {
        long temperature = Math.round(report.temperature());
        long feelsLike = Math.round(report.feelsLike());
        String spoken = "In " + report.city() + " it is " + temperature + " degrees, "
                + report.description().toLowerCase() + "."
                + (feelsLike != temperature ? " It feels like " + feelsLike + " degrees." : "")
                + " Humidity is " + report.humidity() + " percent.";

        Card card = new Card("weather",
                report.country().isEmpty() ? report.city() : report.city() + ", " + report.country(),
                report.description(),
                null, null, null,
                List.of(new Card.Fact("Temperature", temperature + "°C"),
                        new Card.Fact("Feels like", feelsLike + "°C"),
                        new Card.Fact("Humidity", report.humidity() + "%"),
                        new Card.Fact("Wind", Math.round(report.windKmh()) + " km/h")));
        return Reply.withCard(spoken, card);
    }
}
