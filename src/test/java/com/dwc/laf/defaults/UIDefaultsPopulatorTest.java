package com.dwc.laf.defaults;

import com.dwc.laf.css.CssThemeLoader;
import com.dwc.laf.css.CssTokenMap;
import com.dwc.laf.css.CssValue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.swing.UIDefaults;
import javax.swing.plaf.ColorUIResource;
import java.awt.Color;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UIDefaultsPopulator")
class UIDefaultsPopulatorTest {

    private UIDefaults table;

    @BeforeEach
    void setUp() {
        table = new UIDefaults();
    }

    // ---- Helper: build a CssTokenMap from CSS text ----
    private CssTokenMap tokenMap(String css) {
        return CssThemeLoader.loadFromString(css);
    }

    // ---- Helper: build a TokenMappingConfig from properties ----
    private TokenMappingConfig mapping(String cssTokenName, String targetSpec) {
        Properties props = new Properties();
        props.setProperty(cssTokenName, targetSpec);
        return TokenMappingConfig.loadFromProperties(props);
    }

    // ---- 1. Color mapping ----

    @Nested
    @DisplayName("Color mapping")
    class ColorMapping {

        @Test
        @DisplayName("ColorValue -> ColorUIResource in UIDefaults")
        void colorValueToColorUIResource() {
            CssTokenMap tokens = tokenMap(":root { --c: #ff0000; }");
            TokenMappingConfig config = mapping("--c", "color:Button.background");

            UIDefaultsPopulator.populate(table, tokens, config);

            Object val = table.get("Button.background");
            assertNotNull(val);
            assertInstanceOf(ColorUIResource.class, val);
            ColorUIResource cr = (ColorUIResource) val;
            assertEquals(255, cr.getRed());
            assertEquals(0, cr.getGreen());
            assertEquals(0, cr.getBlue());
        }

        @Test
        @DisplayName("Non-color value with COLOR type returns null (not placed)")
        void nonColorValueSkipped() {
            CssTokenMap tokens = tokenMap(":root { --c: 42; }");
            TokenMappingConfig config = mapping("--c", "color:Button.background");

            UIDefaultsPopulator.populate(table, tokens, config);

            assertNull(table.get("Button.background"));
        }
    }

    // ---- 2. Integer mapping ----

    @Nested
    @DisplayName("Integer mapping")
    class IntegerMapping {

        @Test
        @DisplayName("IntegerValue preserved as Integer")
        void integerValuePreserved() {
            CssTokenMap tokens = tokenMap(":root { --w: 400; }");
            TokenMappingConfig config = mapping("--w", "int:Button.font.style");

            UIDefaultsPopulator.populate(table, tokens, config);

            Object val = table.get("Button.font.style");
            assertNotNull(val);
            assertInstanceOf(Integer.class, val);
            assertEquals(400, val);
        }

        @Test
        @DisplayName("DimensionValue with px -> integer pixels")
        void dimensionPxToInt() {
            CssTokenMap tokens = tokenMap(":root { --r: 4px; }");
            TokenMappingConfig config = mapping("--r", "int:Button.arc");

            UIDefaultsPopulator.populate(table, tokens, config);

            Object val = table.get("Button.arc");
            assertNotNull(val);
            assertInstanceOf(Integer.class, val);
            assertEquals(4, val);
        }

        @Test
        @DisplayName("DimensionValue with rem -> integer pixels (0.25rem = 4px)")
        void dimensionRemToInt() {
            CssTokenMap tokens = tokenMap(":root { --r: 0.25rem; }");
            TokenMappingConfig config = mapping("--r", "int:Button.arc");

            UIDefaultsPopulator.populate(table, tokens, config);

            Object val = table.get("Button.arc");
            assertNotNull(val);
            assertInstanceOf(Integer.class, val);
            assertEquals(4, val); // 0.25 * 16 = 4
        }

        @Test
        @DisplayName("DimensionValue with em -> integer pixels (1.5em = 24px)")
        void dimensionEmToInt() {
            CssTokenMap tokens = tokenMap(":root { --r: 1.5em; }");
            TokenMappingConfig config = mapping("--r", "int:Component.focusWidth");

            UIDefaultsPopulator.populate(table, tokens, config);

            Object val = table.get("Component.focusWidth");
            assertNotNull(val);
            assertInstanceOf(Integer.class, val);
            assertEquals(24, val); // 1.5 * 16 = 24
        }

        @Test
        @DisplayName("FloatValue with INT type rounds to integer")
        void floatRoundedToInt() {
            CssTokenMap tokens = tokenMap(":root { --f: 2.7; }");
            TokenMappingConfig config = mapping("--f", "int:Component.borderWidth");

            UIDefaultsPopulator.populate(table, tokens, config);

            Object val = table.get("Component.borderWidth");
            assertNotNull(val);
            assertInstanceOf(Integer.class, val);
            assertEquals(3, val); // Math.round(2.7) = 3
        }
    }

    // ---- 3. Float mapping ----

    @Nested
    @DisplayName("Float mapping")
    class FloatMapping {

        @Test
        @DisplayName("FloatValue preserved as Float")
        void floatValuePreserved() {
            CssTokenMap tokens = tokenMap(":root { --o: 0.6; }");
            TokenMappingConfig config = mapping("--o", "float:Component.disabledOpacity");

            UIDefaultsPopulator.populate(table, tokens, config);

            Object val = table.get("Component.disabledOpacity");
            assertNotNull(val);
            assertInstanceOf(Float.class, val);
            assertEquals(0.6f, (float) val, 0.01f);
        }

        @Test
        @DisplayName("IntegerValue with FLOAT type converts to Float")
        void integerToFloat() {
            CssTokenMap tokens = tokenMap(":root { --i: 2; }");
            TokenMappingConfig config = mapping("--i", "float:Component.opacity");

            UIDefaultsPopulator.populate(table, tokens, config);

            Object val = table.get("Component.opacity");
            assertNotNull(val);
            assertInstanceOf(Float.class, val);
            assertEquals(2.0f, (float) val, 0.01f);
        }
    }

    // ---- 4. String mapping ----

    @Nested
    @DisplayName("String mapping")
    class StringMapping {

        @Test
        @DisplayName("StringValue preserved as String")
        void stringValuePreserved() {
            // Font family stacks with commas are classified as StringValue by CssValueTyper
            CssTokenMap tokens = tokenMap(":root { --font: Helvetica, Arial, sans-serif; }");
            TokenMappingConfig config = mapping("--font", "string:defaultFont.family");

            UIDefaultsPopulator.populate(table, tokens, config);

            Object val = table.get("defaultFont.family");
            assertNotNull(val);
            assertInstanceOf(String.class, val);
        }
    }

    // ---- 5. AUTO type detection ----

    @Nested
    @DisplayName("AUTO type detection")
    class AutoTypeDetection {

        @Test
        @DisplayName("ColorValue auto-detected as ColorUIResource")
        void autoColor() {
            CssTokenMap tokens = tokenMap(":root { --c: blue; }");
            TokenMappingConfig config = mapping("--c", "auto:Label.foreground");

            UIDefaultsPopulator.populate(table, tokens, config);

            Object val = table.get("Label.foreground");
            assertNotNull(val);
            assertInstanceOf(ColorUIResource.class, val);
        }

        @Test
        @DisplayName("IntegerValue auto-detected as Integer")
        void autoInteger() {
            CssTokenMap tokens = tokenMap(":root { --w: 700; }");
            TokenMappingConfig config = mapping("--w", "auto:defaultFont.style");

            UIDefaultsPopulator.populate(table, tokens, config);

            Object val = table.get("defaultFont.style");
            assertNotNull(val);
            assertInstanceOf(Integer.class, val);
            assertEquals(700, val);
        }

        @Test
        @DisplayName("FloatValue auto-detected as Float")
        void autoFloat() {
            CssTokenMap tokens = tokenMap(":root { --o: 0.5; }");
            TokenMappingConfig config = mapping("--o", "auto:Component.opacity");

            UIDefaultsPopulator.populate(table, tokens, config);

            Object val = table.get("Component.opacity");
            assertNotNull(val);
            assertInstanceOf(Float.class, val);
        }

        @Test
        @DisplayName("DimensionValue auto-detected as integer pixels")
        void autoDimension() {
            CssTokenMap tokens = tokenMap(":root { --s: 16px; }");
            TokenMappingConfig config = mapping("--s", "auto:defaultFont.size");

            UIDefaultsPopulator.populate(table, tokens, config);

            Object val = table.get("defaultFont.size");
            assertNotNull(val);
            assertInstanceOf(Integer.class, val);
            assertEquals(16, val);
        }

        @Test
        @DisplayName("StringValue auto-detected as String")
        void autoString() {
            // Comma-separated font stacks are classified as StringValue by CssValueTyper
            CssTokenMap tokens = tokenMap(":root { --f: Helvetica, Arial, sans-serif; }");
            TokenMappingConfig config = mapping("--f", "auto:defaultFont.family");

            UIDefaultsPopulator.populate(table, tokens, config);

            Object val = table.get("defaultFont.family");
            assertNotNull(val);
            assertInstanceOf(String.class, val);
        }

        @Test
        @DisplayName("RawValue skipped in AUTO mode (not placed in UIDefaults)")
        void autoRawSkipped() {
            // env() expressions result in RawValue (calc() is now evaluated before typing)
            CssTokenMap tokens = tokenMap(":root { --r: env(safe-area-inset-top); }");
            TokenMappingConfig config = mapping("--r", "auto:Label.width");

            UIDefaultsPopulator.populate(table, tokens, config);

            assertNull(table.get("Label.width"));
        }
    }

    // ---- 6. Missing tokens and edge cases ----

    @Nested
    @DisplayName("Missing tokens and edge cases")
    class MissingTokens {

        @Test
        @DisplayName("Missing CSS token is skipped without error")
        void missingTokenSkipped() {
            CssTokenMap tokens = tokenMap(":root { --other: red; }");
            TokenMappingConfig config = mapping("--missing", "color:Button.background");

            // Should NOT throw
            assertDoesNotThrow(() ->
                    UIDefaultsPopulator.populate(table, tokens, config));

            assertNull(table.get("Button.background"));
        }

        @Test
        @DisplayName("Null convertValue result is not placed in UIDefaults")
        void nullConversionNotPlaced() {
            // IntegerValue cannot convert to COLOR
            CssTokenMap tokens = tokenMap(":root { --i: 42; }");
            TokenMappingConfig config = mapping("--i", "color:Button.background");

            UIDefaultsPopulator.populate(table, tokens, config);

            assertNull(table.get("Button.background"));
        }
    }

    // ---- 7. One-to-many mapping ----

    @Nested
    @DisplayName("One-to-many mapping")
    class OneToMany {

        @Test
        @DisplayName("One CSS token populates multiple UIDefaults keys")
        void oneTokenMultipleKeys() {
            CssTokenMap tokens = tokenMap(":root { --radius: 8px; }");
            Properties props = new Properties();
            props.setProperty("--radius", "int:Button.arc, int:Component.arc, int:CheckBox.arc");
            TokenMappingConfig config = TokenMappingConfig.loadFromProperties(props);

            UIDefaultsPopulator.populate(table, tokens, config);

            assertEquals(8, table.get("Button.arc"));
            assertEquals(8, table.get("Component.arc"));
            assertEquals(8, table.get("CheckBox.arc"));
        }
    }

    // ---- 8. Integration test ----

    @Nested
    @DisplayName("Integration")
    class Integration {

        @Test
        @DisplayName("End-to-end: CSS text -> CssTokenMap -> TokenMappingConfig -> UIDefaults")
        void endToEnd() {
            // Create a small CSS theme
            String css = """
                :root {
                    --bg-color: #336699;
                    --text-color: #ffffff;
                    --border-radius: 6px;
                    --opacity: 0.8;
                    --font-size: 14;
                    --font-family: Segoe UI, Tahoma, sans-serif;
                }
                """;
            CssTokenMap tokens = CssThemeLoader.loadFromString(css);

            // Create a mapping config
            Properties props = new Properties();
            props.setProperty("--bg-color", "color:Panel.background, color:control");
            props.setProperty("--text-color", "color:Label.foreground");
            props.setProperty("--border-radius", "int:Button.arc, int:Component.arc");
            props.setProperty("--opacity", "float:Component.disabledOpacity");
            props.setProperty("--font-size", "int:defaultFont.size");
            props.setProperty("--font-family", "string:defaultFont.family");
            TokenMappingConfig config = TokenMappingConfig.loadFromProperties(props);

            // Populate
            UIDefaultsPopulator.populate(table, tokens, config);

            // Verify colors are ColorUIResource
            Object bgColor = table.get("Panel.background");
            assertNotNull(bgColor, "Panel.background should be set");
            assertInstanceOf(ColorUIResource.class, bgColor);
            ColorUIResource bgCR = (ColorUIResource) bgColor;
            assertEquals(0x33, bgCR.getRed());
            assertEquals(0x66, bgCR.getGreen());
            assertEquals(0x99, bgCR.getBlue());

            // Verify one-to-many: same color in both keys
            Object control = table.get("control");
            assertNotNull(control);
            assertInstanceOf(ColorUIResource.class, control);

            // Verify text color
            Object textColor = table.get("Label.foreground");
            assertNotNull(textColor);
            assertInstanceOf(ColorUIResource.class, textColor);
            assertEquals(255, ((ColorUIResource) textColor).getRed());

            // Verify integer conversion
            Object arc = table.get("Button.arc");
            assertNotNull(arc);
            assertEquals(6, arc);

            Object compArc = table.get("Component.arc");
            assertNotNull(compArc);
            assertEquals(6, compArc);

            // Verify float
            Object opacity = table.get("Component.disabledOpacity");
            assertNotNull(opacity);
            assertInstanceOf(Float.class, opacity);
            assertEquals(0.8f, (float) opacity, 0.01f);

            // Verify integer (from IntegerValue)
            Object fontSize = table.get("defaultFont.size");
            assertNotNull(fontSize);
            assertEquals(14, fontSize);

            // Verify string
            Object fontFamily = table.get("defaultFont.family");
            assertNotNull(fontFamily);
            assertInstanceOf(String.class, fontFamily);
        }
    }
}
