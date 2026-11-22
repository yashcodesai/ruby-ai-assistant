package com.ruby.ruby.integration;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Picks the right place from geocoding results, e.g. "Manali" (two well-known
 * towns in India) or "Manali Himachal Pradesh" (the qualifier chooses one).
 */
final class Places {

    /** One geocoding result. */
    record Place(String name, String region, String district, String country,
                 long population, double latitude, double longitude) {
    }

    /** The chosen place, plus the region of a well-known namesake if there is one. */
    record Choice(Place place, String alternativeRegion) {
    }

    /** Another place with the same name counts if it has at least this share of the population. */
    private static final double NOTABLE_SHARE = 0.10;

    private Places() {
    }

    /** Keeps results whose region/district/country contain every word of the qualifier. */
    static List<Place> filter(List<Place> results, String qualifier) {
        String[] words = qualifier.toLowerCase(Locale.ROOT).split("[\\s,]+");
        return results.stream()
                .filter(p -> {
                    String where = (p.region() + " " + p.district() + " " + p.country()).toLowerCase(Locale.ROOT);
                    return Arrays.stream(words).filter(w -> !w.isBlank()).allMatch(where::contains);
                })
                .toList();
    }

    /** First (most relevant) result, noting a notable namesake in another region. */
    static Optional<Choice> choose(List<Place> results) {
        if (results.isEmpty()) {
            return Optional.empty();
        }
        Place first = results.get(0);
        String alternative = results.stream()
                .skip(1)
                .filter(p -> p.name().equalsIgnoreCase(first.name()))
                .filter(p -> !p.region().equalsIgnoreCase(first.region()))
                .filter(p -> first.population() == 0 || p.population() >= first.population() * NOTABLE_SHARE)
                .map(Place::region)
                .filter(region -> !region.isBlank())
                .findFirst()
                .orElse(null);
        return Optional.of(new Choice(first, alternative));
    }
}
