package com.ruby.ruby.integration;

import java.util.Optional;

public interface WeatherService {

    /**
     * @return empty if the place is not found
     * @throws ServiceUnavailableException if the weather service cannot be reached
     */
    Optional<WeatherReport> current(String place);
}
