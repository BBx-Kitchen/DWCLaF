package com.dwc.laf.css;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CssDimensionParser")
class CssDimensionParserTest {

    // ---- Dimension values (number + unit) ----

    @Nested
    @DisplayName("Dimension values")
    class DimensionValues {

        @Test
        @DisplayName("16px -> DimensionValue(16.0, 'px')")
        void pixels() {
            Optional<CssValue> result = CssDimensionParser.parse("16px");
            assertTrue(result.isPresent());
            assertInstanceOf(CssValue.DimensionValue.class, result.get());
            CssValue.DimensionValue dim = (CssValue.DimensionValue) result.get();
            assertEquals(16.0f, dim.value(), 0.001f);
            assertEquals("px", dim.unit());
        }

        @Test
        @DisplayName("0.875rem -> DimensionValue(0.875, 'rem')")
        void rem() {
            Optional<CssValue> result = CssDimensionParser.parse("0.875rem");
            assertTrue(result.isPresent());
            assertInstanceOf(CssValue.DimensionValue.class, result.get());
            CssValue.DimensionValue dim = (CssValue.DimensionValue) result.get();
            assertEquals(0.875f, dim.value(), 0.001f);
            assertEquals("rem", dim.unit());
        }

        @Test
        @DisplayName("1.5em -> DimensionValue(1.5, 'em')")
        void em() {
            Optional<CssValue> result = CssDimensionParser.parse("1.5em");
            assertTrue(result.isPresent());
            assertInstanceOf(CssValue.DimensionValue.class, result.get());
            CssValue.DimensionValue dim = (CssValue.DimensionValue) result.get();
            assertEquals(1.5f, dim.value(), 0.001f);
            assertEquals("em", dim.unit());
        }

        @Test
        @DisplayName("50% -> DimensionValue(50.0, '%')")
        void percentage() {
            Optional<CssValue> result = CssDimensionParser.parse("50%");
            assertTrue(result.isPresent());
            assertInstanceOf(CssValue.DimensionValue.class, result.get());
            CssValue.DimensionValue dim = (CssValue.DimensionValue) result.get();
            assertEquals(50.0f, dim.value(), 0.001f);
            assertEquals("%", dim.unit());
        }

        @Test
        @DisplayName("1px -> DimensionValue(1.0, 'px')")
        void onePixel() {
            Optional<CssValue> result = CssDimensionParser.parse("1px");
            assertTrue(result.isPresent());
            assertInstanceOf(CssValue.DimensionValue.class, result.get());
            CssValue.DimensionValue dim = (CssValue.DimensionValue) result.get();
            assertEquals(1.0f, dim.value(), 0.001f);
            assertEquals("px", dim.unit());
        }

        @Test
        @DisplayName("100vh -> DimensionValue(100.0, 'vh')")
        void viewportHeight() {
            Optional<CssValue> result = CssDimensionParser.parse("100vh");
            assertTrue(result.isPresent());
            assertInstanceOf(CssValue.DimensionValue.class, result.get());
            CssValue.DimensionValue dim = (CssValue.DimensionValue) result.get();
            assertEquals(100.0f, dim.value(), 0.001f);
            assertEquals("vh", dim.unit());
        }

        @Test
        @DisplayName("300ms -> DimensionValue(300.0, 'ms')")
        void milliseconds() {
            Optional<CssValue> result = CssDimensionParser.parse("300ms");
            assertTrue(result.isPresent());
            assertInstanceOf(CssValue.DimensionValue.class, result.get());
            CssValue.DimensionValue dim = (CssValue.DimensionValue) result.get();
            assertEquals(300.0f, dim.value(), 0.001f);
            assertEquals("ms", dim.unit());
        }

        @Test
        @DisplayName("0.2s -> DimensionValue(0.2, 's')")
        void seconds() {
            Optional<CssValue> result = CssDimensionParser.parse("0.2s");
            assertTrue(result.isPresent());
            assertInstanceOf(CssValue.DimensionValue.class, result.get());
            CssValue.DimensionValue dim = (CssValue.DimensionValue) result.get();
            assertEquals(0.2f, dim.value(), 0.001f);
            assertEquals("s", dim.unit());
        }

        @Test
        @DisplayName("negative: -2px -> DimensionValue(-2.0, 'px')")
        void negative() {
            Optional<CssValue> result = CssDimensionParser.parse("-2px");
            assertTrue(result.isPresent());
            assertInstanceOf(CssValue.DimensionValue.class, result.get());
            CssValue.DimensionValue dim = (CssValue.DimensionValue) result.get();
            assertEquals(-2.0f, dim.value(), 0.001f);
            assertEquals("px", dim.unit());
        }
    }

    // ---- Pure integer (unitless) ----

    @Nested
    @DisplayName("Integer values")
    class IntegerValues {

        @Test
        @DisplayName("400 -> IntegerValue(400)")
        void fontWeight() {
            Optional<CssValue> result = CssDimensionParser.parse("400");
            assertTrue(result.isPresent());
            assertInstanceOf(CssValue.IntegerValue.class, result.get());
            assertEquals(400, ((CssValue.IntegerValue) result.get()).value());
        }

        @Test
        @DisplayName("0 -> IntegerValue(0)")
        void zero() {
            Optional<CssValue> result = CssDimensionParser.parse("0");
            assertTrue(result.isPresent());
            assertInstanceOf(CssValue.IntegerValue.class, result.get());
            assertEquals(0, ((CssValue.IntegerValue) result.get()).value());
        }

        @Test
        @DisplayName("-1 -> IntegerValue(-1)")
        void negativeInt() {
            Optional<CssValue> result = CssDimensionParser.parse("-1");
            assertTrue(result.isPresent());
            assertInstanceOf(CssValue.IntegerValue.class, result.get());
            assertEquals(-1, ((CssValue.IntegerValue) result.get()).value());
        }

        @Test
        @DisplayName("1000 -> IntegerValue(1000)")
        void largeInt() {
            Optional<CssValue> result = CssDimensionParser.parse("1000");
            assertTrue(result.isPresent());
            assertInstanceOf(CssValue.IntegerValue.class, result.get());
            assertEquals(1000, ((CssValue.IntegerValue) result.get()).value());
        }
    }

    // ---- Unitless float ----

    @Nested
    @DisplayName("Float values")
    class FloatValues {

        @Test
        @DisplayName("1.25 -> FloatValue(1.25)")
        void lineHeight() {
            Optional<CssValue> result = CssDimensionParser.parse("1.25");
            assertTrue(result.isPresent());
            assertInstanceOf(CssValue.FloatValue.class, result.get());
            assertEquals(1.25f, ((CssValue.FloatValue) result.get()).value(), 0.001f);
        }

        @Test
        @DisplayName("0.6 -> FloatValue(0.6)")
        void opacity() {
            Optional<CssValue> result = CssDimensionParser.parse("0.6");
            assertTrue(result.isPresent());
            assertInstanceOf(CssValue.FloatValue.class, result.get());
            assertEquals(0.6f, ((CssValue.FloatValue) result.get()).value(), 0.001f);
        }

        @Test
        @DisplayName("-0.5 -> FloatValue(-0.5)")
        void negativeFloat() {
            Optional<CssValue> result = CssDimensionParser.parse("-0.5");
            assertTrue(result.isPresent());
            assertInstanceOf(CssValue.FloatValue.class, result.get());
            assertEquals(-0.5f, ((CssValue.FloatValue) result.get()).value(), 0.001f);
        }
    }

    // ---- Non-dimension values (return empty) ----

    @Nested
    @DisplayName("Non-dimension values")
    class NonDimension {

        @Test
        @DisplayName("'solid' -> empty")
        void solidKeyword() {
            Optional<CssValue> result = CssDimensionParser.parse("solid");
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("empty string -> empty")
        void emptyString() {
            Optional<CssValue> result = CssDimensionParser.parse("");
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("null -> empty")
        void nullInput() {
            Optional<CssValue> result = CssDimensionParser.parse(null);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("'calc(var(--size-m) / 2)' -> empty")
        void calcExpression() {
            Optional<CssValue> result = CssDimensionParser.parse("calc(var(--size-m) / 2)");
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("'inherit' -> empty")
        void inheritKeyword() {
            Optional<CssValue> result = CssDimensionParser.parse("inherit");
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("'#ff0000' -> empty (color, not dimension)")
        void colorHex() {
            Optional<CssValue> result = CssDimensionParser.parse("#ff0000");
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("whitespace only -> empty")
        void whitespaceOnly() {
            Optional<CssValue> result = CssDimensionParser.parse("   ");
            assertTrue(result.isEmpty());
        }
    }

    // ---- Whitespace handling ----

    @Nested
    @DisplayName("Whitespace handling")
    class WhitespaceHandling {

        @Test
        @DisplayName("' 16px ' -> trims and parses")
        void trims() {
            Optional<CssValue> result = CssDimensionParser.parse(" 16px ");
            assertTrue(result.isPresent());
            assertInstanceOf(CssValue.DimensionValue.class, result.get());
            assertEquals(16.0f, ((CssValue.DimensionValue) result.get()).value(), 0.001f);
        }

        @Test
        @DisplayName("' 400 ' -> trims integer")
        void trimsInteger() {
            Optional<CssValue> result = CssDimensionParser.parse(" 400 ");
            assertTrue(result.isPresent());
            assertInstanceOf(CssValue.IntegerValue.class, result.get());
            assertEquals(400, ((CssValue.IntegerValue) result.get()).value());
        }
    }
}
