package com.dwc.laf.css;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CssValueTyper")
class CssValueTyperTest {

    // ---- Color typing ----

    @Nested
    @DisplayName("Color values")
    class ColorValues {

        @Test
        @DisplayName("HSL color string types as ColorValue")
        void hslTypesAsColor() {
            var resolved = Map.of("--c", "hsl(211, 100%, 50%)");
            var typed = CssValueTyper.type(resolved);
            assertInstanceOf(CssValue.ColorValue.class, typed.get("--c"));
            Color c = ((CssValue.ColorValue) typed.get("--c")).color();
            assertEquals(0, c.getRed(), 1);
            assertEquals(255, c.getBlue(), 1);
        }

        @Test
        @DisplayName("Hex #ff0000 types as ColorValue")
        void hexTypesAsColor() {
            var resolved = Map.of("--c", "#ff0000");
            var typed = CssValueTyper.type(resolved);
            assertInstanceOf(CssValue.ColorValue.class, typed.get("--c"));
            Color c = ((CssValue.ColorValue) typed.get("--c")).color();
            assertEquals(255, c.getRed());
            assertEquals(0, c.getGreen());
            assertEquals(0, c.getBlue());
        }

        @Test
        @DisplayName("Named 'red' types as ColorValue")
        void namedTypesAsColor() {
            var resolved = Map.of("--c", "red");
            var typed = CssValueTyper.type(resolved);
            assertInstanceOf(CssValue.ColorValue.class, typed.get("--c"));
            Color c = ((CssValue.ColorValue) typed.get("--c")).color();
            assertEquals(255, c.getRed());
            assertEquals(0, c.getGreen());
            assertEquals(0, c.getBlue());
        }
    }

    // ---- Dimension / Number typing ----

    @Nested
    @DisplayName("Dimension and number values")
    class DimensionValues {

        @Test
        @DisplayName("'16px' types as DimensionValue(16, 'px')")
        void dimensionWithUnit() {
            var resolved = Map.of("--d", "16px");
            var typed = CssValueTyper.type(resolved);
            assertInstanceOf(CssValue.DimensionValue.class, typed.get("--d"));
            var dim = (CssValue.DimensionValue) typed.get("--d");
            assertEquals(16.0f, dim.value(), 0.01f);
            assertEquals("px", dim.unit());
        }

        @Test
        @DisplayName("'400' types as IntegerValue(400)")
        void integerValue() {
            var resolved = Map.of("--w", "400");
            var typed = CssValueTyper.type(resolved);
            assertInstanceOf(CssValue.IntegerValue.class, typed.get("--w"));
            assertEquals(400, ((CssValue.IntegerValue) typed.get("--w")).value());
        }

        @Test
        @DisplayName("'1.25' types as FloatValue(1.25)")
        void floatValue() {
            var resolved = Map.of("--lh", "1.25");
            var typed = CssValueTyper.type(resolved);
            assertInstanceOf(CssValue.FloatValue.class, typed.get("--lh"));
            assertEquals(1.25f, ((CssValue.FloatValue) typed.get("--lh")).value(), 0.001f);
        }
    }

    // ---- String typing ----

    @Nested
    @DisplayName("String values")
    class StringValues {

        @Test
        @DisplayName("Font family stack types as StringValue")
        void fontFamilyStack() {
            var resolved = Map.of("--f", "-apple-system, sans-serif");
            var typed = CssValueTyper.type(resolved);
            assertInstanceOf(CssValue.StringValue.class, typed.get("--f"));
            assertEquals("-apple-system, sans-serif",
                    ((CssValue.StringValue) typed.get("--f")).value());
        }

        @Test
        @DisplayName("'solid' types as StringValue")
        void cssKeyword() {
            var resolved = Map.of("--bs", "solid");
            var typed = CssValueTyper.type(resolved);
            assertInstanceOf(CssValue.StringValue.class, typed.get("--bs"));
            assertEquals("solid", ((CssValue.StringValue) typed.get("--bs")).value());
        }

        @Test
        @DisplayName("'none' types as StringValue")
        void noneKeyword() {
            var resolved = Map.of("--n", "none");
            var typed = CssValueTyper.type(resolved);
            assertInstanceOf(CssValue.StringValue.class, typed.get("--n"));
        }

        @Test
        @DisplayName("'transparent' types as ColorValue (named color, not keyword)")
        void transparentIsColor() {
            var resolved = Map.of("--t", "transparent");
            var typed = CssValueTyper.type(resolved);
            // 'transparent' is a named CSS color (rgba(0,0,0,0)), parsed by CssColorParser
            assertInstanceOf(CssValue.ColorValue.class, typed.get("--t"));
        }
    }

    // ---- Raw typing ----

    @Nested
    @DisplayName("Raw values")
    class RawValues {

        @Test
        @DisplayName("calc() types as RawValue")
        void calcTypesAsRaw() {
            var resolved = Map.of("--calc", "calc(100% - 20px)");
            var typed = CssValueTyper.type(resolved);
            assertInstanceOf(CssValue.RawValue.class, typed.get("--calc"));
            assertEquals("calc(100% - 20px)",
                    ((CssValue.RawValue) typed.get("--calc")).raw());
        }

        @Test
        @DisplayName("Complex shadow value types as StringValue (contains commas)")
        void shadowTypesAsString() {
            var resolved = Map.of("--shadow",
                    "0 2px 4px rgba(0, 0, 0, 0.1), 0 4px 8px rgba(0, 0, 0, 0.05)");
            var typed = CssValueTyper.type(resolved);
            // Contains commas -> StringValue
            assertInstanceOf(CssValue.StringValue.class, typed.get("--shadow"));
        }

        @Test
        @DisplayName("Unknown string without comma or keyword types as RawValue")
        void unknownStringTypesAsRaw() {
            var resolved = Map.of("--x", "some-custom-value");
            var typed = CssValueTyper.type(resolved);
            assertInstanceOf(CssValue.RawValue.class, typed.get("--x"));
        }
    }

    // ---- Edge cases ----

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("Null input returns empty map")
        void nullInput() {
            var typed = CssValueTyper.type(null);
            assertTrue(typed.isEmpty());
        }

        @Test
        @DisplayName("Empty map returns empty map")
        void emptyInput() {
            var typed = CssValueTyper.type(Map.of());
            assertTrue(typed.isEmpty());
        }

        @Test
        @DisplayName("Result map is unmodifiable")
        void resultIsImmutable() {
            var resolved = Map.of("--a", "red");
            var typed = CssValueTyper.type(resolved);
            assertThrows(UnsupportedOperationException.class, () ->
                    typed.put("--b", new CssValue.RawValue("x")));
        }

        @Test
        @DisplayName("Multiple values typed correctly in one pass")
        void multipleValues() {
            var resolved = new LinkedHashMap<String, String>();
            resolved.put("--color", "#ff0000");
            resolved.put("--size", "16px");
            resolved.put("--weight", "400");
            resolved.put("--font", "-apple-system, sans-serif");
            resolved.put("--calc", "calc(100% - 20px)");

            var typed = CssValueTyper.type(resolved);

            assertEquals(5, typed.size());
            assertInstanceOf(CssValue.ColorValue.class, typed.get("--color"));
            assertInstanceOf(CssValue.DimensionValue.class, typed.get("--size"));
            assertInstanceOf(CssValue.IntegerValue.class, typed.get("--weight"));
            assertInstanceOf(CssValue.StringValue.class, typed.get("--font"));
            assertInstanceOf(CssValue.RawValue.class, typed.get("--calc"));
        }
    }
}
