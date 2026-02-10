package com.dwc.laf.css;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CssThemeLoader")
class CssThemeLoaderTest {

    @AfterEach
    void clearSystemProperty() {
        System.clearProperty("dwc.theme");
    }

    // ---- 1. Classpath loading ----

    @Nested
    @DisplayName("Classpath loading")
    class ClasspathLoading {

        @Test
        @DisplayName("loadFromClasspath loads default-light.css with 200+ tokens")
        void loadsDefaultLight() {
            CssTokenMap map = CssThemeLoader.loadFromClasspath(
                    "com/dwc/laf/themes/default-light.css");
            assertNotNull(map);
            assertTrue(map.size() > 200,
                    "Expected 200+ tokens, got: " + map.size());
        }
    }

    // ---- 2. String loading ----

    @Nested
    @DisplayName("String loading")
    class StringLoading {

        @Test
        @DisplayName("loadFromString parses simple CSS and types correctly")
        void loadsFromString() {
            CssTokenMap map = CssThemeLoader.loadFromString(":root { --a: red; }");
            assertNotNull(map);
            assertEquals(1, map.size());
            assertTrue(map.contains("--a"));
            Optional<Color> color = map.getColor("--a");
            assertTrue(color.isPresent());
            assertEquals(new Color(255, 0, 0), color.get());
        }
    }

    // ---- 3. Full pipeline test ----

    @Nested
    @DisplayName("Full pipeline")
    class FullPipeline {

        @Test
        @DisplayName("Color tokens resolve to correct Color objects")
        void colorTokensResolve() {
            CssTokenMap map = CssThemeLoader.loadFromString(
                    ":root { --c: #ff0000; }");
            Optional<Color> c = map.getColor("--c");
            assertTrue(c.isPresent());
            assertEquals(new Color(255, 0, 0), c.get());
        }

        @Test
        @DisplayName("var() references are resolved, not raw strings")
        void varReferencesResolved() {
            CssTokenMap map = CssThemeLoader.loadFromString(
                    ":root { --base: #00ff00; --alias: var(--base); }");
            Optional<Color> c = map.getColor("--alias");
            assertTrue(c.isPresent());
            assertEquals(new Color(0, 255, 0), c.get());
            // Raw value should be the resolved hex, not "var(--base)"
            assertEquals("#00ff00", map.getRaw("--alias"));
        }

        @Test
        @DisplayName("HSL colors produce correct Color")
        void hslColorProducesCorrectColor() {
            CssTokenMap map = CssThemeLoader.loadFromString(
                    ":root { --hsl: hsl(0, 100%, 50%); }");
            Optional<Color> c = map.getColor("--hsl");
            assertTrue(c.isPresent());
            assertEquals(255, c.get().getRed());
            assertEquals(0, c.get().getGreen(), 1);
            assertEquals(0, c.get().getBlue(), 1);
        }

        @Test
        @DisplayName("Dimension tokens produce DimensionValue")
        void dimensionTokensTyped() {
            CssTokenMap map = CssThemeLoader.loadFromString(
                    ":root { --size: 16px; }");
            Optional<CssValue> val = map.get("--size");
            assertTrue(val.isPresent());
            assertInstanceOf(CssValue.DimensionValue.class, val.get());
            assertEquals(16.0f, ((CssValue.DimensionValue) val.get()).value(), 0.01f);
            assertEquals("px", ((CssValue.DimensionValue) val.get()).unit());
        }

        @Test
        @DisplayName("Integer tokens produce IntegerValue")
        void integerTokensTyped() {
            CssTokenMap map = CssThemeLoader.loadFromString(
                    ":root { --weight: 400; }");
            OptionalInt val = map.getInt("--weight");
            assertTrue(val.isPresent());
            assertEquals(400, val.getAsInt());
        }

        @Test
        @DisplayName("test-tokens.css pipeline: colors, vars, dimensions all correct")
        void testTokensFullPipeline() {
            CssTokenMap map = CssThemeLoader.loadFromClasspath("test-tokens.css");
            assertNotNull(map);

            // Color tokens
            assertTrue(map.getColor("--color-red").isPresent());
            assertEquals(new Color(255, 0, 0), map.getColor("--color-red").get());

            // var() resolved (not raw)
            assertTrue(map.getColor("--theme-color").isPresent());
            assertEquals(new Color(255, 0, 0), map.getColor("--theme-color").get());

            // Aliased var() chain
            assertTrue(map.getColor("--aliased-theme").isPresent());
            assertEquals(new Color(255, 0, 0), map.getColor("--aliased-theme").get());

            // Fallback resolved
            assertTrue(map.getColor("--fallback-simple").isPresent());
            // --fallback-simple: var(--missing, blue) -> blue
            assertEquals(new Color(0, 0, 255), map.getColor("--fallback-simple").get());

            // Dimension tokens
            Optional<CssValue> sizeRem = map.get("--size-rem");
            assertTrue(sizeRem.isPresent());
            assertInstanceOf(CssValue.DimensionValue.class, sizeRem.get());

            // Integer tokens
            OptionalInt weight = map.getInt("--weight");
            assertTrue(weight.isPresent());
            assertEquals(400, weight.getAsInt());

            // Circular references excluded
            assertFalse(map.contains("--circular-a"));
            assertFalse(map.contains("--circular-b"));
            assertFalse(map.contains("--self-ref"));
        }
    }

    // ---- 4. Override merge test ----

    @Nested
    @DisplayName("Override merge")
    class OverrideMerge {

        @Test
        @DisplayName("Override tokens replace matching keys, base provides defaults")
        void overrideMergesCorrectly() {
            // Simulate merge via loadFromString on base + override separately
            // then test with system property (test 5) for actual file-based merge.
            // This test validates the concept directly.
            String base = ":root { --a: red; --b: blue; }";
            String override = ":root { --a: green; }";

            // Parse base
            var baseTokens = CssTokenParser.parse(base);
            var overrideTokens = CssTokenParser.parse(override);

            // Merge
            var merged = new java.util.LinkedHashMap<>(baseTokens);
            merged.putAll(overrideTokens);

            var resolved = CssVariableResolver.resolve(merged);
            var typed = CssValueTyper.type(resolved);
            var map = new CssTokenMap(typed, resolved);

            // --a should be green (overridden)
            Optional<Color> colorA = map.getColor("--a");
            assertTrue(colorA.isPresent());
            assertEquals(new Color(0, 128, 0), colorA.get()); // CSS "green" = #008000

            // --b should be blue (preserved from base)
            Optional<Color> colorB = map.getColor("--b");
            assertTrue(colorB.isPresent());
            assertEquals(new Color(0, 0, 255), colorB.get());
        }
    }

    // ---- 5. System property override test ----

    @Nested
    @DisplayName("System property override")
    class SystemPropertyOverride {

        @Test
        @DisplayName("dwc.theme system property triggers file override merge")
        void systemPropertyOverride(@TempDir Path tempDir) throws IOException {
            // Write override CSS to temp file
            Path overrideFile = tempDir.resolve("override.css");
            Files.writeString(overrideFile,
                    ":root { --dwc-color-white: #000000; }");

            // Set system property
            System.setProperty("dwc.theme", overrideFile.toString());

            // Load (will merge bundled + override)
            CssTokenMap map = CssThemeLoader.load();

            // --dwc-color-white should be overridden to black
            Optional<Color> white = map.getColor("--dwc-color-white");
            assertTrue(white.isPresent());
            assertEquals(new Color(0, 0, 0), white.get());

            // Other bundled tokens should still be present
            assertTrue(map.size() > 200,
                    "Expected 200+ tokens with override, got: " + map.size());
        }
    }

    // ---- 6. File loading test ----

    @Nested
    @DisplayName("File loading")
    class FileLoading {

        @Test
        @DisplayName("loadFromFile reads CSS from file and produces tokens")
        void loadsFromFile(@TempDir Path tempDir) throws IOException {
            Path cssFile = tempDir.resolve("theme.css");
            Files.writeString(cssFile,
                    ":root { --my-color: #123456; --my-size: 24px; }");

            CssTokenMap map = CssThemeLoader.loadFromFile(cssFile);

            assertEquals(2, map.size());
            assertTrue(map.getColor("--my-color").isPresent());
            Optional<CssValue> size = map.get("--my-size");
            assertTrue(size.isPresent());
            assertInstanceOf(CssValue.DimensionValue.class, size.get());
        }
    }

    // ---- 7. Missing classpath resource ----

    @Nested
    @DisplayName("Missing resources")
    class MissingResources {

        @Test
        @DisplayName("loadFromClasspath with nonexistent resource returns empty map")
        void missingClasspathReturnsEmpty() {
            CssTokenMap map = CssThemeLoader.loadFromClasspath("nonexistent.css");
            assertNotNull(map);
            assertEquals(0, map.size());
        }
    }

    // ---- 8. Missing override file ----

    @Nested
    @DisplayName("Missing override file")
    class MissingOverrideFile {

        @Test
        @DisplayName("Missing override file is non-fatal, bundled defaults still work")
        void missingOverrideNonFatal() {
            System.setProperty("dwc.theme", "/nonexistent/path/override.css");

            CssTokenMap map = CssThemeLoader.load();

            // Should still have bundled tokens (warning logged, not fatal)
            assertNotNull(map);
            assertTrue(map.size() > 200,
                    "Expected 200+ bundled tokens despite missing override, got: " + map.size());
        }
    }

    // ---- 9. End-to-end with real DWC tokens ----

    @Nested
    @DisplayName("End-to-end with real DWC tokens")
    class EndToEndRealTokens {

        @Test
        @DisplayName("Primary color 50 is a blue-ish Color (hue ~211)")
        void primaryColor50() {
            CssTokenMap map = CssThemeLoader.loadFromClasspath(
                    "com/dwc/laf/themes/default-light.css");

            // --dwc-color-primary-50: hsl(var(--dwc-color-primary-h), var(--dwc-color-primary-s), 50%)
            // After resolution: hsl(211, 100%, 50%) -> R=0, G=123, B=255
            Optional<Color> color = map.getColor("--dwc-color-primary-50");
            assertTrue(color.isPresent(), "Expected --dwc-color-primary-50 to be a color");
            Color c = color.get();
            // Blue-ish: low red, moderate green, high blue
            assertTrue(c.getRed() < 10, "Red channel should be near 0, got: " + c.getRed());
            assertTrue(c.getBlue() > 250, "Blue channel should be near 255, got: " + c.getBlue());
            assertTrue(c.getGreen() > 100 && c.getGreen() < 140,
                    "Green channel should be ~123, got: " + c.getGreen());
        }

        @Test
        @DisplayName("Font weight normal is 400")
        void fontWeightNormal() {
            CssTokenMap map = CssThemeLoader.loadFromClasspath(
                    "com/dwc/laf/themes/default-light.css");

            OptionalInt weight = map.getInt("--dwc-font-weight-normal");
            assertTrue(weight.isPresent());
            assertEquals(400, weight.getAsInt());
        }

        @Test
        @DisplayName("Font family sans contains expected system fonts")
        void fontFamilySans() {
            CssTokenMap map = CssThemeLoader.loadFromClasspath(
                    "com/dwc/laf/themes/default-light.css");

            Optional<String> font = map.getString("--dwc-font-family-sans");
            assertTrue(font.isPresent(), "Expected --dwc-font-family-sans to be a string");
            String fontValue = font.get();
            assertTrue(fontValue.contains("-apple-system") || fontValue.contains("Roboto"),
                    "Expected font family to contain apple-system or Roboto, got: " + fontValue);
        }

        @Test
        @DisplayName("Color white is #fff -> Color(255, 255, 255)")
        void colorWhite() {
            CssTokenMap map = CssThemeLoader.loadFromClasspath(
                    "com/dwc/laf/themes/default-light.css");

            Optional<Color> white = map.getColor("--dwc-color-white");
            assertTrue(white.isPresent());
            assertEquals(new Color(255, 255, 255), white.get());
        }

        @Test
        @DisplayName("Token count is 200+ (realistic theme)")
        void realisticTokenCount() {
            CssTokenMap map = CssThemeLoader.loadFromClasspath(
                    "com/dwc/laf/themes/default-light.css");

            assertTrue(map.size() > 200,
                    "Expected 200+ tokens from real theme, got: " + map.size());
        }
    }
}
