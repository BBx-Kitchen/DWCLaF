package com.dwc.laf.css;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link CssVariableResolver} -- Pass 2 of the two-pass
 * CSS token architecture. Resolves all var() references in the raw
 * token map from {@link CssTokenParser}.
 */
class CssVariableResolverTest {

    // ================================================================
    // Passthrough (no var() references)
    // ================================================================

    @Nested
    @DisplayName("Passthrough values (no var() references)")
    class PassthroughTests {

        @Test
        void emptyMap() {
            var result = CssVariableResolver.resolve(Map.of());
            assertTrue(result.isEmpty());
        }

        @Test
        void singleLiteralValue() {
            var result = CssVariableResolver.resolve(Map.of("--a", "red"));
            assertEquals(Map.of("--a", "red"), result);
        }

        @Test
        void multipleLiteralValues() {
            var raw = Map.of("--a", "red", "--b", "10px", "--c", "bold");
            var result = CssVariableResolver.resolve(raw);
            assertEquals(3, result.size());
            assertEquals("red", result.get("--a"));
            assertEquals("10px", result.get("--b"));
            assertEquals("bold", result.get("--c"));
        }
    }

    // ================================================================
    // Simple var() references
    // ================================================================

    @Nested
    @DisplayName("Simple var() references")
    class SimpleReferenceTests {

        @Test
        void simpleReference() {
            var raw = Map.of("--a", "red", "--b", "var(--a)");
            var result = CssVariableResolver.resolve(raw);
            assertEquals("red", result.get("--b"));
        }

        @Test
        void chainedReference() {
            // --c -> --b -> --a = "1"
            var raw = Map.of("--a", "1", "--b", "var(--a)", "--c", "var(--b)");
            var result = CssVariableResolver.resolve(raw);
            assertEquals("1", result.get("--a"));
            assertEquals("1", result.get("--b"));
            assertEquals("1", result.get("--c"));
        }

        @Test
        void referencePreservesTargetValue() {
            var raw = Map.of("--color", "hsl(211, 100%, 50%)", "--primary", "var(--color)");
            var result = CssVariableResolver.resolve(raw);
            assertEquals("hsl(211, 100%, 50%)", result.get("--primary"));
        }
    }

    // ================================================================
    // Fallback values
    // ================================================================

    @Nested
    @DisplayName("Fallback values")
    class FallbackTests {

        @Test
        void fallbackWhenMissing() {
            var raw = Map.of("--a", "var(--missing, blue)");
            var result = CssVariableResolver.resolve(raw);
            assertEquals("blue", result.get("--a"));
        }

        @Test
        void fallbackIgnoredWhenReferenceExists() {
            var raw = Map.of("--x", "red", "--a", "var(--x, blue)");
            var result = CssVariableResolver.resolve(raw);
            assertEquals("red", result.get("--a"));
        }

        @Test
        void nestedFallback() {
            // var(--missing, var(--also-missing, green))
            var raw = Map.of("--a", "var(--missing, var(--also-missing, green))");
            var result = CssVariableResolver.resolve(raw);
            assertEquals("green", result.get("--a"));
        }

        @Test
        void fallbackToExistingVariable() {
            // var(--missing, var(--x)) where --x exists
            var raw = Map.of("--x", "red", "--a", "var(--missing, var(--x))");
            var result = CssVariableResolver.resolve(raw);
            assertEquals("red", result.get("--a"));
        }

        @Test
        void deeplyNestedFallback() {
            // var(--a1, var(--a2, var(--a3, found)))
            var raw = Map.of("--z", "var(--a1, var(--a2, var(--a3, found)))");
            var result = CssVariableResolver.resolve(raw);
            assertEquals("found", result.get("--z"));
        }
    }

    // ================================================================
    // Circular references
    // ================================================================

    @Nested
    @DisplayName("Circular reference detection")
    class CircularTests {

        @Test
        void selfReference() {
            // --a -> --a (cycle)
            var raw = Map.of("--a", "var(--a)");
            var result = CssVariableResolver.resolve(raw);
            assertFalse(result.containsKey("--a"), "--a should be excluded (self-reference)");
        }

        @Test
        void mutualCircularReference() {
            // --a -> --b -> --a (cycle)
            var raw = Map.of("--a", "var(--b)", "--b", "var(--a)");
            var result = CssVariableResolver.resolve(raw);
            assertFalse(result.containsKey("--a"), "--a should be excluded (circular)");
            assertFalse(result.containsKey("--b"), "--b should be excluded (circular)");
        }

        @Test
        void circularWithFallback() {
            // --a has fallback so it resolves; --b has no fallback
            var raw = Map.of("--a", "var(--b, safe)", "--b", "var(--a)");
            var result = CssVariableResolver.resolve(raw);
            assertEquals("safe", result.get("--a"));
            // --b tries --a, which tries --b (cycle), --a falls back to "safe"
            // but --b itself has no fallback for its cycle, so excluded
            assertFalse(result.containsKey("--b"), "--b should be excluded (circular, no fallback)");
        }

        @Test
        void threeWayCycle() {
            // --a -> --b -> --c -> --a
            var raw = Map.of("--a", "var(--b)", "--b", "var(--c)", "--c", "var(--a)");
            var result = CssVariableResolver.resolve(raw);
            assertFalse(result.containsKey("--a"));
            assertFalse(result.containsKey("--b"));
            assertFalse(result.containsKey("--c"));
        }

        @Test
        void circularDoesNotAffectOthers() {
            // --a -> --b -> --a (cycle), but --c is independent
            var raw = Map.of("--a", "var(--b)", "--b", "var(--a)", "--c", "red");
            var result = CssVariableResolver.resolve(raw);
            assertFalse(result.containsKey("--a"));
            assertFalse(result.containsKey("--b"));
            assertEquals("red", result.get("--c"));
        }
    }

    // ================================================================
    // Missing references
    // ================================================================

    @Nested
    @DisplayName("Missing references (no fallback)")
    class MissingTests {

        @Test
        void missingReferenceNoFallback() {
            var raw = Map.of("--a", "var(--missing)");
            var result = CssVariableResolver.resolve(raw);
            assertFalse(result.containsKey("--a"), "--a should be excluded (missing, no fallback)");
        }

        @Test
        void missingReferenceInChain() {
            // --b refers to --a which refers to --missing
            var raw = Map.of("--a", "var(--missing)", "--b", "var(--a)");
            var result = CssVariableResolver.resolve(raw);
            assertFalse(result.containsKey("--a"));
            assertFalse(result.containsKey("--b"));
        }
    }

    // ================================================================
    // Embedded var() in larger values
    // ================================================================

    @Nested
    @DisplayName("var() embedded in larger strings")
    class EmbeddedVarTests {

        @Test
        void partialVarInValue() {
            var raw = Map.of("--a", "red", "--shadow", "0 2px var(--a)");
            var result = CssVariableResolver.resolve(raw);
            assertEquals("0 2px red", result.get("--shadow"));
        }

        @Test
        void multipleVarInOneValue() {
            var raw = Map.of("--a", "10", "--b", "20", "--c", "var(--a) var(--b)");
            var result = CssVariableResolver.resolve(raw);
            assertEquals("10 20", result.get("--c"));
        }

        @Test
        void hslWithVarReferences() {
            var raw = Map.of("--h", "211", "--s", "100%", "--color", "hsl(var(--h), var(--s), 50%)");
            var result = CssVariableResolver.resolve(raw);
            assertEquals("hsl(211, 100%, 50%)", result.get("--color"));
        }

        @Test
        void multipleVarWithLiterals() {
            var raw = Map.of("--x", "4px", "--y", "8px",
                    "--border", "var(--x) solid var(--y)");
            var result = CssVariableResolver.resolve(raw);
            assertEquals("4px solid 8px", result.get("--border"));
        }

        @Test
        void embeddedVarWithMissingExcludesEntry() {
            // If an embedded var() has no fallback and reference is missing,
            // the entire value cannot be resolved
            var raw = Map.of("--shadow", "0 2px var(--missing)");
            var result = CssVariableResolver.resolve(raw);
            assertFalse(result.containsKey("--shadow"),
                    "Entire value excluded when embedded var() cannot resolve");
        }

        @Test
        void embeddedVarWithFallback() {
            var raw = Map.of("--shadow", "0 2px var(--missing, 4px)");
            var result = CssVariableResolver.resolve(raw);
            assertEquals("0 2px 4px", result.get("--shadow"));
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
            var raw = Map.of("--a", "red");
            var result = CssVariableResolver.resolve(raw);
            assertThrows(UnsupportedOperationException.class,
                    () -> result.put("--b", "blue"));
        }
    }

    // ================================================================
    // Malformed var()
    // ================================================================

    @Nested
    @DisplayName("Malformed var() expressions")
    class MalformedTests {

        @Test
        void unclosedVarKeptAsIs() {
            // var( with no closing paren -- keep raw string, log warning
            var raw = Map.of("--a", "var(--oops");
            var result = CssVariableResolver.resolve(raw);
            assertEquals("var(--oops", result.get("--a"));
        }
    }

    // ================================================================
    // Null handling
    // ================================================================

    @Nested
    @DisplayName("Null and edge cases")
    class EdgeCaseTests {

        @Test
        void nullInputReturnsEmptyMap() {
            var result = CssVariableResolver.resolve(null);
            assertTrue(result.isEmpty());
        }

        @Test
        void emptyStringValuesPreserved() {
            var raw = Map.of("--a", "");
            var result = CssVariableResolver.resolve(raw);
            assertEquals("", result.get("--a"));
        }
    }

    // ================================================================
    // Integration: realistic token maps
    // ================================================================

    @Nested
    @DisplayName("Realistic token maps")
    class RealisticTests {

        @Test
        void dwcColorPalettePattern() {
            // DWC pattern: base hue defined, then derived colors via var()
            var raw = Map.of(
                    "--dwc-color-primary-h", "211",
                    "--dwc-color-primary-s", "100%",
                    "--dwc-color-primary-l", "50%",
                    "--dwc-color-primary", "hsl(var(--dwc-color-primary-h), var(--dwc-color-primary-s), var(--dwc-color-primary-l))"
            );
            var result = CssVariableResolver.resolve(raw);
            assertEquals("211", result.get("--dwc-color-primary-h"));
            assertEquals("100%", result.get("--dwc-color-primary-s"));
            assertEquals("50%", result.get("--dwc-color-primary-l"));
            assertEquals("hsl(211, 100%, 50%)", result.get("--dwc-color-primary"));
        }

        @Test
        void shadowWithMultipleReferences() {
            var raw = Map.of(
                    "--dwc-shadow-color", "rgba(0, 0, 0, 0.2)",
                    "--dwc-shadow-offset-x", "0",
                    "--dwc-shadow-offset-y", "2px",
                    "--dwc-shadow-blur", "4px",
                    "--dwc-shadow", "var(--dwc-shadow-offset-x) var(--dwc-shadow-offset-y) var(--dwc-shadow-blur) var(--dwc-shadow-color)"
            );
            var result = CssVariableResolver.resolve(raw);
            assertEquals("0 2px 4px rgba(0, 0, 0, 0.2)", result.get("--dwc-shadow"));
        }
    }
}
