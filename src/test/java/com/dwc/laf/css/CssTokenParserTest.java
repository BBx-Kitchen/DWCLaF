package com.dwc.laf.css;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link CssTokenParser} -- CSS custom property extraction (Pass 1).
 *
 * <p>Verifies that parse() extracts {@code --custom-property} declarations from
 * CSS text into a {@code Map<String, String>} of property name to raw value string.
 * var() references are NOT resolved here -- just extracted as-is.</p>
 */
class CssTokenParserTest {

    // ----------------------------------------------------------------
    // Basic extraction from :root
    // ----------------------------------------------------------------

    @Nested
    @DisplayName(":root extraction")
    class RootExtraction {

        @Test
        @DisplayName("extracts single custom property from :root")
        void singleProperty() {
            var result = CssTokenParser.parse(":root { --color: red; }");
            assertEquals(Map.of("--color", "red"), result);
        }

        @Test
        @DisplayName("extracts multiple custom properties from :root")
        void multipleProperties() {
            var css = """
                    :root {
                      --a: 1;
                      --b: 2;
                      --c: 3;
                    }
                    """;
            var result = CssTokenParser.parse(css);
            assertEquals(3, result.size());
            assertEquals("1", result.get("--a"));
            assertEquals("2", result.get("--b"));
            assertEquals("3", result.get("--c"));
        }

        @Test
        @DisplayName("ignores non-custom properties")
        void ignoresNonCustomProperties() {
            var css = ":root { color: red; --custom: blue; font-size: 14px; }";
            var result = CssTokenParser.parse(css);
            assertEquals(1, result.size());
            assertEquals("blue", result.get("--custom"));
        }

        @Test
        @DisplayName("extracts var() references as raw strings")
        void varReferencesRaw() {
            var css = ":root { --a: 1; --b: var(--a); }";
            var result = CssTokenParser.parse(css);
            assertEquals("1", result.get("--a"));
            assertEquals("var(--a)", result.get("--b"));
        }
    }

    // ----------------------------------------------------------------
    // Component selector extraction
    // ----------------------------------------------------------------

    @Nested
    @DisplayName("component selector extraction")
    class ComponentSelectorExtraction {

        @Test
        @DisplayName("extracts from component selectors like .dwc-button")
        void componentSelector() {
            var css = ".dwc-button { --btn-bg: blue; }";
            var result = CssTokenParser.parse(css);
            assertEquals("blue", result.get("--btn-bg"));
        }

        @Test
        @DisplayName("extracts from :host selector")
        void hostSelector() {
            var css = ":host { --host-val: 42; }";
            var result = CssTokenParser.parse(css);
            assertEquals("42", result.get("--host-val"));
        }

        @Test
        @DisplayName("extracts from multiple selector blocks")
        void multipleSelectorBlocks() {
            var css = """
                    :root { --a: 1; }
                    :host { --b: 2; }
                    .dwc-button { --c: 3; }
                    """;
            var result = CssTokenParser.parse(css);
            assertEquals("1", result.get("--a"));
            assertEquals("2", result.get("--b"));
            assertEquals("3", result.get("--c"));
        }

        @Test
        @DisplayName("extracts from attribute selectors")
        void attributeSelector() {
            var css = ".dwc-button[theme='primary'] { --btn-bg: var(--primary); }";
            var result = CssTokenParser.parse(css);
            assertEquals("var(--primary)", result.get("--btn-bg"));
        }
    }

    // ----------------------------------------------------------------
    // Flattening / override behavior
    // ----------------------------------------------------------------

    @Nested
    @DisplayName("flattening and overrides")
    class FlatteningOverrides {

        @Test
        @DisplayName("component-level overrides :root for same property name")
        void componentOverridesRoot() {
            var css = """
                    :root { --a: 1; }
                    .comp { --a: 2; }
                    """;
            var result = CssTokenParser.parse(css);
            assertEquals("2", result.get("--a"));
        }

        @Test
        @DisplayName("later :root block overrides earlier :root block")
        void laterRootOverridesEarlier() {
            var css = """
                    :root { --x: first; }
                    :root { --x: second; }
                    """;
            var result = CssTokenParser.parse(css);
            assertEquals("second", result.get("--x"));
        }

        @Test
        @DisplayName("later component block overrides earlier component block for same property")
        void laterComponentOverridesEarlier() {
            var css = """
                    .comp-a { --x: first; }
                    .comp-b { --x: second; }
                    """;
            var result = CssTokenParser.parse(css);
            assertEquals("second", result.get("--x"));
        }

        @Test
        @DisplayName(":root + component flatten into single map preserving both unique keys")
        void flattenedMapHasBothKeys() {
            var css = """
                    :root { --global: 10; }
                    .comp { --local: 20; }
                    """;
            var result = CssTokenParser.parse(css);
            assertEquals(2, result.size());
            assertEquals("10", result.get("--global"));
            assertEquals("20", result.get("--local"));
        }
    }

    // ----------------------------------------------------------------
    // Value preservation
    // ----------------------------------------------------------------

    @Nested
    @DisplayName("value preservation")
    class ValuePreservation {

        @Test
        @DisplayName("preserves commas in value")
        void commasInValue() {
            var css = ":root { --font: -apple-system, sans-serif; }";
            var result = CssTokenParser.parse(css);
            assertEquals("-apple-system, sans-serif", result.get("--font"));
        }

        @Test
        @DisplayName("preserves multi-part shadow values with rgba")
        void shadowValues() {
            var css = ":root { --shadow: 0 2px 4px rgba(0,0,0,0.1), 0 4px 8px rgba(0,0,0,0.05); }";
            var result = CssTokenParser.parse(css);
            assertEquals("0 2px 4px rgba(0,0,0,0.1), 0 4px 8px rgba(0,0,0,0.05)", result.get("--shadow"));
        }

        @Test
        @DisplayName("captures multi-line values until semicolon")
        void multiLineValues() {
            var css = """
                    :root {
                      --shadow: 0 2px 4px rgba(0,0,0,0.1),
                                0 4px 8px rgba(0,0,0,0.05);
                    }
                    """;
            var result = CssTokenParser.parse(css);
            // Multi-line value should be captured and whitespace normalized
            String val = result.get("--shadow");
            assertNotNull(val);
            assertTrue(val.contains("rgba(0,0,0,0.1)"), "should contain first shadow part");
            assertTrue(val.contains("rgba(0,0,0,0.05)"), "should contain second shadow part");
        }

        @Test
        @DisplayName("trims whitespace from values")
        void trimmedValues() {
            var css = ":root { --a:   spaced   ; }";
            var result = CssTokenParser.parse(css);
            assertEquals("spaced", result.get("--a"));
        }

        @Test
        @DisplayName("preserves var() with fallback")
        void varWithFallback() {
            var css = ":root { --x: var(--missing, blue); }";
            var result = CssTokenParser.parse(css);
            assertEquals("var(--missing, blue)", result.get("--x"));
        }

        @Test
        @DisplayName("preserves nested var() fallbacks")
        void nestedVarFallback() {
            var css = ":root { --x: var(--missing, var(--color-red, blue)); }";
            var result = CssTokenParser.parse(css);
            assertEquals("var(--missing, var(--color-red, blue))", result.get("--x"));
        }

        @Test
        @DisplayName("preserves calc() expressions")
        void calcExpressions() {
            var css = ":root { --calc: calc(16px * 1.5); }";
            var result = CssTokenParser.parse(css);
            assertEquals("calc(16px * 1.5)", result.get("--calc"));
        }

        @Test
        @DisplayName("preserves hsl() function values")
        void hslFunctions() {
            var css = ":root { --color: hsl(211, 100%, 50%); }";
            var result = CssTokenParser.parse(css);
            assertEquals("hsl(211, 100%, 50%)", result.get("--color"));
        }

        @Test
        @DisplayName("preserves quoted string values")
        void quotedStrings() {
            var css = ":root { --indicator: '\\2022'; }";
            var result = CssTokenParser.parse(css);
            assertEquals("'\\2022'", result.get("--indicator"));
        }
    }

    // ----------------------------------------------------------------
    // CSS comment handling
    // ----------------------------------------------------------------

    @Nested
    @DisplayName("comment handling")
    class CommentHandling {

        @Test
        @DisplayName("strips block comments")
        void blockComments() {
            var css = "/* comment */ :root { --a: 1; }";
            var result = CssTokenParser.parse(css);
            assertEquals("1", result.get("--a"));
        }

        @Test
        @DisplayName("strips inline comments between declarations")
        void inlineComments() {
            var css = """
                    :root {
                      --a: 1; /* first */
                      --b: 2; /* second */
                    }
                    """;
            var result = CssTokenParser.parse(css);
            assertEquals("1", result.get("--a"));
            assertEquals("2", result.get("--b"));
        }

        @Test
        @DisplayName("strips multi-line block comments")
        void multiLineComments() {
            var css = """
                    /* This is
                       a multi-line
                       comment */
                    :root { --a: 1; }
                    """;
            var result = CssTokenParser.parse(css);
            assertEquals("1", result.get("--a"));
        }

        @Test
        @DisplayName("handles comment inside value gracefully")
        void commentInValue() {
            // Edge case: /* comment */ before semicolon within a value
            var css = ":root { --a: red /* inline */; }";
            var result = CssTokenParser.parse(css);
            // After comment stripping, value should be "red" (trimmed)
            assertEquals("red", result.get("--a"));
        }
    }

    // ----------------------------------------------------------------
    // Edge cases and error handling
    // ----------------------------------------------------------------

    @Nested
    @DisplayName("edge cases and error handling")
    class EdgeCases {

        @Test
        @DisplayName("empty input returns empty map")
        void emptyInput() {
            var result = CssTokenParser.parse("");
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("null input returns empty map")
        void nullInput() {
            var result = CssTokenParser.parse(null);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("whitespace-only input returns empty map")
        void whitespaceOnly() {
            var result = CssTokenParser.parse("   \n\t  ");
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("malformed declaration without colon is skipped")
        void malformedNoColon() {
            var css = ":root { --broken; --ok: 1; }";
            var result = CssTokenParser.parse(css);
            assertEquals(1, result.size());
            assertEquals("1", result.get("--ok"));
        }

        @Test
        @DisplayName("declaration with colon but no value before semicolon is skipped")
        void emptyValueSkipped() {
            var css = ":root { --empty: ; --ok: 1; }";
            var result = CssTokenParser.parse(css);
            // Empty value should either be stored as empty string or skipped
            // For robustness, we store empty string
            assertEquals("1", result.get("--ok"));
        }

        @Test
        @DisplayName("CSS with no custom properties returns empty map")
        void noCustomProperties() {
            var css = ":root { color: red; font-size: 14px; }";
            var result = CssTokenParser.parse(css);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("CSS with no selector blocks returns empty map")
        void noBlocks() {
            var css = "/* just a comment */";
            var result = CssTokenParser.parse(css);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("returned map is unmodifiable")
        void unmodifiableMap() {
            var result = CssTokenParser.parse(":root { --a: 1; }");
            assertThrows(UnsupportedOperationException.class, () -> result.put("--x", "y"));
        }

        @Test
        @DisplayName("value terminated by closing brace when semicolon missing")
        void valueTerminatedByBrace() {
            var css = ":root { --a: hello }";
            var result = CssTokenParser.parse(css);
            assertEquals("hello", result.get("--a"));
        }
    }

    // ----------------------------------------------------------------
    // Realistic CSS files
    // ----------------------------------------------------------------

    @Nested
    @DisplayName("realistic CSS file parsing")
    class RealisticFiles {

        @Test
        @DisplayName("parses test-tokens.css with all expected entries")
        void testTokensCss() throws IOException {
            var css = loadResource("/test-tokens.css");
            var result = CssTokenParser.parse(css);

            // :root block entries
            assertEquals("#ff0000", result.get("--color-red"));
            assertEquals("#00ff00", result.get("--color-green"));
            assertEquals("#0000ff", result.get("--color-blue"));
            assertEquals("#f00", result.get("--hex3"));
            assertEquals("rgb(255, 0, 0)", result.get("--rgb-color"));
            assertEquals("hsl(211, 100%, 50%)", result.get("--hsl-color"));
            assertEquals("var(--color-red)", result.get("--theme-color"));
            assertEquals("var(--missing, blue)", result.get("--fallback-simple"));
            assertEquals("0.875rem", result.get("--size-rem"));
            assertEquals("400", result.get("--weight"));
            assertEquals("1.25", result.get("--line-height"));
            assertEquals("-apple-system, BlinkMacSystemFont, sans-serif", result.get("--font-family"));

            // Component selector entries
            assertEquals("var(--color-red)", result.get("--comp-bg"));
            assertEquals("var(--color-green)", result.get("--panel-bg"));

            // Override: component overrides :root
            assertEquals("#00ff00", result.get("--comp-override"));
        }

        @Test
        @DisplayName("parses bundled default-light.css with 200+ entries")
        void defaultLightCss() throws IOException {
            var css = loadResource("/com/dwc/laf/themes/default-light.css");
            var result = CssTokenParser.parse(css);

            // Should have many entries
            assertTrue(result.size() >= 200,
                    "Expected 200+ entries, got " + result.size());

            // Spot-check some known properties
            assertNotNull(result.get("--dwc-font-family-sans"), "should have font family");
            assertNotNull(result.get("--dwc-font-weight-normal"), "should have font weight");
            assertNotNull(result.get("--dwc-border-radius"), "should have border radius");
            assertNotNull(result.get("--dwc-shadow-1"), "should have shadow");
        }

        @Test
        @DisplayName("default-light.css component selectors override :root for same properties")
        void defaultLightCssComponentOverrides() throws IOException {
            var css = loadResource("/com/dwc/laf/themes/default-light.css");
            var result = CssTokenParser.parse(css);

            // Component-level properties should be present
            assertNotNull(result.get("--dwc-button-background"),
                    "should have button background from .dwc-button selector");
            assertNotNull(result.get("--dwc-input-background"),
                    "should have input background from .dwc-input selector");
        }
    }

    // ----------------------------------------------------------------
    // Whitespace and formatting variations
    // ----------------------------------------------------------------

    @Nested
    @DisplayName("whitespace and formatting")
    class WhitespaceFormatting {

        @Test
        @DisplayName("handles single-line CSS")
        void singleLine() {
            var css = ":root{--a:1;--b:2;}";
            var result = CssTokenParser.parse(css);
            assertEquals("1", result.get("--a"));
            assertEquals("2", result.get("--b"));
        }

        @Test
        @DisplayName("handles extra whitespace around colon and semicolon")
        void extraWhitespace() {
            var css = ":root { --a :  hello world  ; }";
            var result = CssTokenParser.parse(css);
            assertEquals("hello world", result.get("--a"));
        }

        @Test
        @DisplayName("handles tab-indented CSS")
        void tabIndented() {
            var css = ":root {\n\t--a: 1;\n\t--b: 2;\n}";
            var result = CssTokenParser.parse(css);
            assertEquals("1", result.get("--a"));
            assertEquals("2", result.get("--b"));
        }
    }

    // ----------------------------------------------------------------
    // Helper
    // ----------------------------------------------------------------

    private String loadResource(String path) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) {
                throw new IOException("Resource not found: " + path);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
