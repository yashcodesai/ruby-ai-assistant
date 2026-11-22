package com.ruby.ruby.integration;

import com.ruby.ruby.integration.Places.Place;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlacesTest {

    // Real Open-Meteo results for "Manali", in the order the API returns them.
    private static final Place MANALI_TN = new Place("Manali", "Tamil Nadu", "Chennai district", "India", 35248, 13.17, 80.27);
    private static final Place MANALI_HP = new Place("Manali", "Himachal Pradesh", "Kullu", "India", 8096, 32.26, 77.17);
    private static final Place MANALI_VILLAGE = new Place("Manali", "Kerala", "Malappuram", "India", 0, 11.33, 76.29);

    @Test
    void notesNotableNamesake() {
        Places.Choice choice = Places.choose(List.of(MANALI_TN, MANALI_HP, MANALI_VILLAGE)).orElseThrow();
        assertEquals(MANALI_TN, choice.place());
        assertEquals("Himachal Pradesh", choice.alternativeRegion());
    }

    @Test
    void qualifierPicksTheRightPlace() {
        List<Place> filtered = Places.filter(List.of(MANALI_TN, MANALI_HP, MANALI_VILLAGE), "himachal pradesh");
        assertEquals(List.of(MANALI_HP), filtered);
        assertNull(Places.choose(filtered).orElseThrow().alternativeRegion());
    }

    @Test
    void tinyNamesakesAreNotAmbiguous() {
        Place london = new Place("London", "England", "Greater London", "United Kingdom", 8_961_989, 51.5, -0.13);
        Place ontario = new Place("London", "Ontario", "Middlesex", "Canada", 383_822, 42.98, -81.25);
        assertNull(Places.choose(List.of(london, ontario)).orElseThrow().alternativeRegion());
    }

    @Test
    void nothingFound() {
        assertTrue(Places.choose(List.of()).isEmpty());
    }
}
