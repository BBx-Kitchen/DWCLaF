package com.dwc.laf.css;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link CssCalcEvaluator} -- evaluates calc() expressions in
 * resolved CSS token values before they are typed by {@link CssValueTyper}.
 */
class CssCalcEvaluatorTest {

    // ================================================================
    // Passthrough (no calc() expressions)
    // ================================================================

    @Nested
    @DisplayName("Passthrough values (no calc() expressions)")
    class PassthroughTests {

        @Test
        void emptyMap() {
            var result = CssCalcEvaluator.evaluate(Map.of());
            assertTrue(result.isEmpty());
        }

        @Test
        void nullMapReturnsEmpty() {
            var result = CssCalcEvaluator.evaluate(null);
            assertTrue(result.isEmpty());
        }

        @Test
        void plainValueUnchanged() {
            var result = CssCalcEvaluator.evaluate(Map.of("--a", "red"));
            assertEquals("red", result.get("--a"));
        }

        @Test
        void hslWithoutCalcUnchanged() {
            var result = CssCalcEvaluator.evaluate(Map.of("--c", "hsl(211, 100%, 50%)"));
            assertEquals("hsl(211, 100%, 50%)", result.get("--c"));
        }

        @Test
        void dimensionWithoutCalcUnchanged() {
            var result = CssCalcEvaluator.evaluate(Map.of("--size", "2.25rem"));
            assertEquals("2.25rem", result.get("--size"));
        }
    }

    // ================================================================
    // Simple arithmetic
    // ================================================================

    @Nested
    @DisplayName("Simple arithmetic")
    class SimpleArithmeticTests {

        @Test
        void addition() {
            var result = CssCalcEvaluator.evaluate(Map.of("--a", "calc(10 + 5)"));
            assertEquals("15", result.get("--a"));
        }

        @Test
        void subtraction() {
            var result = CssCalcEvaluator.evaluate(Map.of("--a", "calc(10 - 5)"));
            assertEquals("5", result.get("--a"));
        }

        @Test
        void negativeResult() {
            var result = CssCalcEvaluator.evaluate(Map.of("--a", "calc(5 - 10)"));
            assertEquals("-5", result.get("--a"));
        }
    }

    // ================================================================
    // Multiplication and division
    // ================================================================

    @Nested
    @DisplayName("Multiplication and division")
    class MultiplicationDivisionTests {

        @Test
        void multiplication() {
            var result = CssCalcEvaluator.evaluate(Map.of("--a", "calc(10 * 2)"));
            assertEquals("20", result.get("--a"));
        }

        @Test
        void division() {
            var result = CssCalcEvaluator.evaluate(Map.of("--a", "calc(10 / 4)"));
            assertEquals("2.5", result.get("--a"));
        }

        @Test
        void divisionWholeNumber() {
            var result = CssCalcEvaluator.evaluate(Map.of("--a", "calc(10 / 2)"));
            assertEquals("5", result.get("--a"));
        }
    }

    // ================================================================
    // Parentheses and operator precedence
    // ================================================================

    @Nested
    @DisplayName("Parentheses and operator precedence")
    class ParenthesesTests {

        @Test
        void parenthesesGrouping() {
            // (40 - 50) * -100 = (-10) * (-100) = 1000
            var result = CssCalcEvaluator.evaluate(Map.of("--a", "calc((40 - 50) * -100)"));
            assertEquals("1000", result.get("--a"));
        }

        @Test
        void operatorPrecedence() {
            // 2 + 3 * 4 = 2 + 12 = 14
            var result = CssCalcEvaluator.evaluate(Map.of("--a", "calc(2 + 3 * 4)"));
            assertEquals("14", result.get("--a"));
        }

        @Test
        void nestedParentheses() {
            // ((2 + 3) * (4 - 1)) = 5 * 3 = 15
            var result = CssCalcEvaluator.evaluate(Map.of("--a", "calc((2 + 3) * (4 - 1))"));
            assertEquals("15", result.get("--a"));
        }
    }

    // ================================================================
    // Percentage values
    // ================================================================

    @Nested
    @DisplayName("Percentage values")
    class PercentageTests {

        @Test
        void percentageClampsHigh() {
            // (40 - 50) * -100% = 1000%, clamped to 100%
            var result = CssCalcEvaluator.evaluate(Map.of("--a", "calc((40 - 50) * -100%)"));
            assertEquals("100%", result.get("--a"));
        }

        @Test
        void percentageClampsLow() {
            // (90 - 50) * -100% = -4000%, clamped to 0%
            var result = CssCalcEvaluator.evaluate(Map.of("--a", "calc((90 - 50) * -100%)"));
            assertEquals("0%", result.get("--a"));
        }

        @Test
        void percentageInRange() {
            // 50%
            var result = CssCalcEvaluator.evaluate(Map.of("--a", "calc(25 + 25%)"));
            assertEquals("50%", result.get("--a"));
        }

        @Test
        void simplePercentage() {
            var result = CssCalcEvaluator.evaluate(Map.of("--a", "calc(50%)"));
            assertEquals("50%", result.get("--a"));
        }
    }

    // ================================================================
    // Embedded in hsl()
    // ================================================================

    @Nested
    @DisplayName("calc() embedded in hsl()")
    class EmbeddedHslTests {

        @Test
        void hslWithCalcLightness() {
            var result = CssCalcEvaluator.evaluate(
                    Map.of("--c", "hsl(0, 0%, calc((40 - 50) * -100%))"));
            assertEquals("hsl(0, 0%, 100%)", result.get("--c"));
        }

        @Test
        void hslWithCalcProducingZero() {
            var result = CssCalcEvaluator.evaluate(
                    Map.of("--c", "hsl(0, 0%, calc((90 - 50) * -100%))"));
            assertEquals("hsl(0, 0%, 0%)", result.get("--c"));
        }

        @Test
        void hslWithCalcInRange() {
            // (50 - 50) * -100% = 0%, which is in range
            var result = CssCalcEvaluator.evaluate(
                    Map.of("--c", "hsl(0, 0%, calc((50 - 50) * -100%))"));
            assertEquals("hsl(0, 0%, 0%)", result.get("--c"));
        }
    }

    // ================================================================
    // Unit preservation
    // ================================================================

    @Nested
    @DisplayName("Unit preservation")
    class UnitTests {

        @Test
        void remUnitPreserved() {
            var result = CssCalcEvaluator.evaluate(Map.of("--a", "calc(2.25rem / 2)"));
            assertEquals("1.125rem", result.get("--a"));
        }

        @Test
        void pxUnitPreserved() {
            var result = CssCalcEvaluator.evaluate(Map.of("--a", "calc(10px + 5px)"));
            assertEquals("15px", result.get("--a"));
        }

        @Test
        void pxMultipliedByUnitless() {
            var result = CssCalcEvaluator.evaluate(Map.of("--a", "calc(10px * 2)"));
            assertEquals("20px", result.get("--a"));
        }

        @Test
        void unitlessDividedByUnitless() {
            var result = CssCalcEvaluator.evaluate(Map.of("--a", "calc(10 / 2)"));
            assertEquals("5", result.get("--a"));
        }
    }

    // ================================================================
    // Multiple calc() in one value
    // ================================================================

    @Nested
    @DisplayName("Multiple calc() in one value")
    class MultipleCalcTests {

        @Test
        void twoCalcExpressionsInOneValue() {
            var result = CssCalcEvaluator.evaluate(
                    Map.of("--a", "calc(10 + 5) calc(20 - 3)"));
            assertEquals("15 17", result.get("--a"));
        }

        @Test
        void calcInMultipleHslArgs() {
            var result = CssCalcEvaluator.evaluate(
                    Map.of("--c", "hsl(calc(180 + 30), calc(50 + 50%), calc(25 * 2%))"));
            assertEquals("hsl(210, 100%, 50%)", result.get("--c"));
        }
    }

    // ================================================================
    // Malformed expressions
    // ================================================================

    @Nested
    @DisplayName("Malformed calc() expressions")
    class MalformedTests {

        @Test
        void unparseableCalcLeftAsIs() {
            var result = CssCalcEvaluator.evaluate(Map.of("--a", "calc(foo + bar)"));
            assertEquals("calc(foo + bar)", result.get("--a"));
        }

        @Test
        void unclosedCalcLeftAsIs() {
            var result = CssCalcEvaluator.evaluate(Map.of("--a", "calc(10 + 5"));
            assertEquals("calc(10 + 5", result.get("--a"));
        }

        @Test
        void emptyCalcLeftAsIs() {
            var result = CssCalcEvaluator.evaluate(Map.of("--a", "calc()"));
            assertEquals("calc()", result.get("--a"));
        }
    }

    // ================================================================
    // Immutability
    // ================================================================

    @Nested
    @DisplayName("Result immutability")
    class ImmutabilityTests {

        @Test
        void resultIsImmutable() {
            var result = CssCalcEvaluator.evaluate(Map.of("--a", "calc(1 + 2)"));
            assertThrows(UnsupportedOperationException.class,
                    () -> result.put("--b", "blue"));
        }

        @Test
        void inputNotMutated() {
            var input = new LinkedHashMap<String, String>();
            input.put("--a", "calc(1 + 2)");
            CssCalcEvaluator.evaluate(input);
            assertEquals("calc(1 + 2)", input.get("--a"), "Original map should not be modified");
        }
    }

    // ================================================================
    // Real DWC tokens
    // ================================================================

    @Nested
    @DisplayName("Real DWC contrast color tokens")
    class RealTokenTests {

        @Test
        void dangerTextContrastWhite() {
            // When contrast value (50) > threshold (40): (40 - 50) * -100% = 1000% → clamped 100%
            var result = CssCalcEvaluator.evaluate(
                    Map.of("--dwc-color-danger-text-40",
                            "hsl(0, 0%, calc((40 - 50) * -100%))"));
            assertEquals("hsl(0, 0%, 100%)", result.get("--dwc-color-danger-text-40"));
        }

        @Test
        void dangerTextContrastBlack() {
            // When contrast value (30) < threshold (40): (40 - 30) * -100% = -1000% → clamped 0%
            var result = CssCalcEvaluator.evaluate(
                    Map.of("--dwc-color-danger-text-40",
                            "hsl(0, 0%, calc((40 - 30) * -100%))"));
            assertEquals("hsl(0, 0%, 0%)", result.get("--dwc-color-danger-text-40"));
        }

        @Test
        void multipleContrastTokens() {
            var input = new LinkedHashMap<String, String>();
            input.put("--dwc-color-danger-text-40", "hsl(0, 0%, calc((40 - 50) * -100%))");
            input.put("--dwc-color-primary-text-40", "hsl(211, 0%, calc((40 - 50) * -100%))");
            input.put("--dwc-color-success-text-40", "hsl(120, 0%, calc((40 - 50) * -100%))");
            input.put("--dwc-button-color", "#ffffff"); // non-calc passthrough

            var result = CssCalcEvaluator.evaluate(input);

            assertEquals("hsl(0, 0%, 100%)", result.get("--dwc-color-danger-text-40"));
            assertEquals("hsl(211, 0%, 100%)", result.get("--dwc-color-primary-text-40"));
            assertEquals("hsl(120, 0%, 100%)", result.get("--dwc-color-success-text-40"));
            assertEquals("#ffffff", result.get("--dwc-button-color"));
        }
    }

    // ================================================================
    // Edge cases
    // ================================================================

    @Nested
    @DisplayName("Edge cases")
    class EdgeCaseTests {

        @Test
        void decimalNumbers() {
            var result = CssCalcEvaluator.evaluate(Map.of("--a", "calc(1.5 + 2.5)"));
            assertEquals("4", result.get("--a"));
        }

        @Test
        void unaryMinus() {
            var result = CssCalcEvaluator.evaluate(Map.of("--a", "calc(-5 + 10)"));
            assertEquals("5", result.get("--a"));
        }

        @Test
        void unaryPlus() {
            var result = CssCalcEvaluator.evaluate(Map.of("--a", "calc(+5 + 10)"));
            assertEquals("15", result.get("--a"));
        }

        @Test
        void zeroResult() {
            var result = CssCalcEvaluator.evaluate(Map.of("--a", "calc(5 - 5)"));
            assertEquals("0", result.get("--a"));
        }

        @Test
        void preservesAllEntries() {
            var input = new LinkedHashMap<String, String>();
            input.put("--a", "calc(1 + 2)");
            input.put("--b", "red");
            input.put("--c", "calc(3 * 4)");

            var result = CssCalcEvaluator.evaluate(input);

            assertEquals(3, result.size());
            assertEquals("3", result.get("--a"));
            assertEquals("red", result.get("--b"));
            assertEquals("12", result.get("--c"));
        }
    }
}
