package com.dwc.laf.css;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CssTokenMap")
class CssTokenMapTest {

    private CssTokenMap tokenMap;

    @BeforeEach
    void setUp() {
        var typed = new LinkedHashMap<String, CssValue>();
        typed.put("--color-red", new CssValue.ColorValue(new Color(255, 0, 0)));
        typed.put("--color-blue", new CssValue.ColorValue(new Color(0, 0, 255)));
        typed.put("--weight", new CssValue.IntegerValue(400));
        typed.put("--opacity", new CssValue.FloatValue(0.6f));
        typed.put("--font", new CssValue.StringValue("-apple-system, sans-serif"));
        typed.put("--calc", new CssValue.RawValue("calc(100% - 20px)"));
        typed.put("--size", new CssValue.DimensionValue(16.0f, "px"));

        var raw = new LinkedHashMap<String, String>();
        raw.put("--color-red", "#ff0000");
        raw.put("--color-blue", "#0000ff");
        raw.put("--weight", "400");
        raw.put("--opacity", "0.6");
        raw.put("--font", "-apple-system, sans-serif");
        raw.put("--calc", "calc(100% - 20px)");
        raw.put("--size", "16px");

        tokenMap = new CssTokenMap(typed, raw);
    }

    // ---- getColor ----

    @Nested
    @DisplayName("getColor()")
    class GetColor {

        @Test
        @DisplayName("Returns Color for color tokens")
        void returnsColorForColorToken() {
            Optional<Color> result = tokenMap.getColor("--color-red");
            assertTrue(result.isPresent());
            assertEquals(new Color(255, 0, 0), result.get());
        }

        @Test
        @DisplayName("Returns empty for non-color token")
        void returnsEmptyForNonColor() {
            Optional<Color> result = tokenMap.getColor("--weight");
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Returns empty for absent token")
        void returnsEmptyForAbsent() {
            Optional<Color> result = tokenMap.getColor("--nonexistent");
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("getColor with default returns color when present")
        void withDefaultReturnsColor() {
            Color result = tokenMap.getColor("--color-red", Color.BLACK);
            assertEquals(new Color(255, 0, 0), result);
        }

        @Test
        @DisplayName("getColor with default returns default when missing")
        void withDefaultReturnsDefault() {
            Color result = tokenMap.getColor("--nonexistent", Color.BLACK);
            assertEquals(Color.BLACK, result);
        }

        @Test
        @DisplayName("getColor with default returns default for non-color token")
        void withDefaultReturnsDefaultForNonColor() {
            Color result = tokenMap.getColor("--weight", Color.BLACK);
            assertEquals(Color.BLACK, result);
        }
    }

    // ---- getInt ----

    @Nested
    @DisplayName("getInt()")
    class GetInt {

        @Test
        @DisplayName("Returns int for integer tokens")
        void returnsIntForIntToken() {
            OptionalInt result = tokenMap.getInt("--weight");
            assertTrue(result.isPresent());
            assertEquals(400, result.getAsInt());
        }

        @Test
        @DisplayName("Returns empty for non-integer token")
        void returnsEmptyForNonInt() {
            OptionalInt result = tokenMap.getInt("--color-red");
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Returns empty for absent token")
        void returnsEmptyForAbsent() {
            OptionalInt result = tokenMap.getInt("--nonexistent");
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("getInt with default returns value when present")
        void withDefaultReturnsValue() {
            int result = tokenMap.getInt("--weight", 0);
            assertEquals(400, result);
        }

        @Test
        @DisplayName("getInt with default returns default when missing")
        void withDefaultReturnsDefault() {
            int result = tokenMap.getInt("--nonexistent", 42);
            assertEquals(42, result);
        }
    }

    // ---- getFloat ----

    @Nested
    @DisplayName("getFloat()")
    class GetFloat {

        @Test
        @DisplayName("Returns float for float tokens")
        void returnsFloatForFloatToken() {
            Optional<Float> result = tokenMap.getFloat("--opacity");
            assertTrue(result.isPresent());
            assertEquals(0.6f, result.get(), 0.001f);
        }

        @Test
        @DisplayName("Returns empty for non-float token")
        void returnsEmptyForNonFloat() {
            Optional<Float> result = tokenMap.getFloat("--weight");
            assertTrue(result.isEmpty());
        }
    }

    // ---- getString ----

    @Nested
    @DisplayName("getString()")
    class GetString {

        @Test
        @DisplayName("Returns string for string tokens")
        void returnsStringForStringToken() {
            Optional<String> result = tokenMap.getString("--font");
            assertTrue(result.isPresent());
            assertEquals("-apple-system, sans-serif", result.get());
        }

        @Test
        @DisplayName("Returns empty for non-string token")
        void returnsEmptyForNonString() {
            Optional<String> result = tokenMap.getString("--weight");
            assertTrue(result.isEmpty());
        }
    }

    // ---- get ----

    @Nested
    @DisplayName("get()")
    class Get {

        @Test
        @DisplayName("Returns CssValue for any token type")
        void returnsCssValueForAny() {
            assertTrue(tokenMap.get("--color-red").isPresent());
            assertTrue(tokenMap.get("--weight").isPresent());
            assertTrue(tokenMap.get("--font").isPresent());
            assertTrue(tokenMap.get("--calc").isPresent());
        }

        @Test
        @DisplayName("Returns empty for absent token")
        void returnsEmptyForAbsent() {
            assertTrue(tokenMap.get("--nonexistent").isEmpty());
        }
    }

    // ---- getRaw ----

    @Nested
    @DisplayName("getRaw()")
    class GetRaw {

        @Test
        @DisplayName("Returns resolved string for debugging")
        void returnsRawString() {
            assertEquals("#ff0000", tokenMap.getRaw("--color-red"));
            assertEquals("400", tokenMap.getRaw("--weight"));
        }

        @Test
        @DisplayName("Returns null for absent token")
        void returnsNullForAbsent() {
            assertNull(tokenMap.getRaw("--nonexistent"));
        }
    }

    // ---- Bulk access ----

    @Nested
    @DisplayName("Bulk access")
    class BulkAccess {

        @Test
        @DisplayName("propertyNames() returns all names")
        void propertyNamesReturnsAll() {
            var names = tokenMap.propertyNames();
            assertEquals(7, names.size());
            assertTrue(names.contains("--color-red"));
            assertTrue(names.contains("--weight"));
            assertTrue(names.contains("--font"));
        }

        @Test
        @DisplayName("size() returns correct count")
        void sizeReturnsCount() {
            assertEquals(7, tokenMap.size());
        }

        @Test
        @DisplayName("contains() returns true for present")
        void containsReturnsTrueForPresent() {
            assertTrue(tokenMap.contains("--color-red"));
            assertTrue(tokenMap.contains("--weight"));
        }

        @Test
        @DisplayName("contains() returns false for absent")
        void containsReturnsFalseForAbsent() {
            assertFalse(tokenMap.contains("--nonexistent"));
        }
    }

    // ---- Immutability ----

    @Nested
    @DisplayName("Immutability")
    class Immutability {

        @Test
        @DisplayName("Token map is unmodifiable")
        void tokenMapIsUnmodifiable() {
            assertThrows(UnsupportedOperationException.class, () ->
                    tokenMap.propertyNames().add("--new"));
        }
    }
}
