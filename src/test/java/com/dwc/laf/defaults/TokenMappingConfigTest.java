package com.dwc.laf.defaults;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TokenMappingConfig")
class TokenMappingConfigTest {

    @AfterEach
    void clearSystemProperty() {
        System.clearProperty("dwc.mapping");
    }

    // ---- 1. Basic parsing ----

    @Nested
    @DisplayName("Basic parsing")
    class BasicParsing {

        @Test
        @DisplayName("Single one-to-one mapping")
        void singleOneToOne() {
            Properties props = new Properties();
            props.setProperty("--my-color", "color:Button.background");

            TokenMappingConfig config = TokenMappingConfig.loadFromProperties(props);

            assertEquals(1, config.entries().size());
            MappingEntry entry = config.entries().get(0);
            assertEquals("--my-color", entry.cssTokenName());
            assertEquals(1, entry.targets().size());
            assertEquals("Button.background", entry.targets().get(0).key());
            assertEquals(MappingType.COLOR, entry.targets().get(0).type());
        }

        @Test
        @DisplayName("One-to-many mapping with comma-separated targets")
        void oneToMany() {
            Properties props = new Properties();
            props.setProperty("--radius", "int:Button.arc, int:Component.arc, int:CheckBox.arc");

            TokenMappingConfig config = TokenMappingConfig.loadFromProperties(props);

            assertEquals(1, config.entries().size());
            MappingEntry entry = config.entries().get(0);
            assertEquals("--radius", entry.cssTokenName());
            assertEquals(3, entry.targets().size());
            assertEquals("Button.arc", entry.targets().get(0).key());
            assertEquals("Component.arc", entry.targets().get(1).key());
            assertEquals("CheckBox.arc", entry.targets().get(2).key());
            // All should be INT
            for (MappingTarget t : entry.targets()) {
                assertEquals(MappingType.INT, t.type());
            }
        }
    }

    // ---- 2. Type prefix parsing ----

    @Nested
    @DisplayName("Type prefix parsing")
    class TypePrefixParsing {

        @Test
        @DisplayName("color: prefix parses to COLOR")
        void colorPrefix() {
            Properties props = new Properties();
            props.setProperty("--c", "color:Panel.bg");
            TokenMappingConfig config = TokenMappingConfig.loadFromProperties(props);
            assertEquals(MappingType.COLOR, config.entries().get(0).targets().get(0).type());
        }

        @Test
        @DisplayName("int: prefix parses to INT")
        void intPrefix() {
            Properties props = new Properties();
            props.setProperty("--i", "int:Button.arc");
            TokenMappingConfig config = TokenMappingConfig.loadFromProperties(props);
            assertEquals(MappingType.INT, config.entries().get(0).targets().get(0).type());
        }

        @Test
        @DisplayName("float: prefix parses to FLOAT")
        void floatPrefix() {
            Properties props = new Properties();
            props.setProperty("--f", "float:Component.opacity");
            TokenMappingConfig config = TokenMappingConfig.loadFromProperties(props);
            assertEquals(MappingType.FLOAT, config.entries().get(0).targets().get(0).type());
        }

        @Test
        @DisplayName("string: prefix parses to STRING")
        void stringPrefix() {
            Properties props = new Properties();
            props.setProperty("--s", "string:defaultFont.family");
            TokenMappingConfig config = TokenMappingConfig.loadFromProperties(props);
            assertEquals(MappingType.STRING, config.entries().get(0).targets().get(0).type());
        }

        @Test
        @DisplayName("insets: prefix parses to INSETS")
        void insetsPrefix() {
            Properties props = new Properties();
            props.setProperty("--ins", "insets:Button.margin");
            TokenMappingConfig config = TokenMappingConfig.loadFromProperties(props);
            assertEquals(MappingType.INSETS, config.entries().get(0).targets().get(0).type());
        }

        @Test
        @DisplayName("No type prefix defaults to AUTO")
        void noTypePrefix() {
            Properties props = new Properties();
            props.setProperty("--auto", "Button.background");
            TokenMappingConfig config = TokenMappingConfig.loadFromProperties(props);
            assertEquals(MappingType.AUTO, config.entries().get(0).targets().get(0).type());
            assertEquals("Button.background", config.entries().get(0).targets().get(0).key());
        }
    }

    // ---- 3. Edge cases ----

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("Empty/blank target segments are skipped")
        void emptySegmentsSkipped() {
            Properties props = new Properties();
            props.setProperty("--c", "color:A, , color:B, ");

            TokenMappingConfig config = TokenMappingConfig.loadFromProperties(props);

            assertEquals(1, config.entries().size());
            List<MappingTarget> targets = config.entries().get(0).targets();
            assertEquals(2, targets.size());
            assertEquals("A", targets.get(0).key());
            assertEquals("B", targets.get(1).key());
        }

        @Test
        @DisplayName("Blank property value is skipped")
        void blankValueSkipped() {
            Properties props = new Properties();
            props.setProperty("--empty", "   ");

            TokenMappingConfig config = TokenMappingConfig.loadFromProperties(props);
            assertEquals(0, config.entries().size());
        }

        @Test
        @DisplayName("Entries list is unmodifiable")
        void entriesUnmodifiable() {
            Properties props = new Properties();
            props.setProperty("--c", "color:A");

            TokenMappingConfig config = TokenMappingConfig.loadFromProperties(props);
            assertThrows(UnsupportedOperationException.class,
                    () -> config.entries().add(
                            new MappingEntry("--x", List.of(new MappingTarget("X", MappingType.AUTO)))));
        }

        @Test
        @DisplayName("MappingEntry targets list is unmodifiable")
        void targetsUnmodifiable() {
            Properties props = new Properties();
            props.setProperty("--c", "color:A");

            TokenMappingConfig config = TokenMappingConfig.loadFromProperties(props);
            assertThrows(UnsupportedOperationException.class,
                    () -> config.entries().get(0).targets().add(
                            new MappingTarget("X", MappingType.AUTO)));
        }
    }

    // ---- 4. Classpath loading ----

    @Nested
    @DisplayName("Classpath loading")
    class ClasspathLoading {

        @Test
        @DisplayName("loadFromClasspath loads bundled token-mapping.properties")
        void loadsBundledProperties() {
            TokenMappingConfig config = TokenMappingConfig.loadFromClasspath(
                    "com/dwc/laf/token-mapping.properties");

            assertNotNull(config);
            assertFalse(config.entries().isEmpty(),
                    "Bundled token-mapping.properties should have entries");
        }

        @Test
        @DisplayName("Bundled properties file has non-zero entries")
        void bundledHasEntries() {
            TokenMappingConfig config = TokenMappingConfig.loadFromClasspath(
                    "com/dwc/laf/token-mapping.properties");

            assertTrue(config.entries().size() > 10,
                    "Expected 10+ mapping entries, got: " + config.entries().size());
        }

        @Test
        @DisplayName("loadDefault returns non-empty config")
        void loadDefaultReturnsNonEmptyConfig() {
            TokenMappingConfig config = TokenMappingConfig.loadDefault();

            assertNotNull(config);
            assertTrue(config.entries().size() > 10,
                    "Expected 10+ entries from loadDefault, got: " + config.entries().size());
        }

        @Test
        @DisplayName("loadFromClasspath with nonexistent resource returns empty config")
        void missingResourceReturnsEmpty() {
            TokenMappingConfig config = TokenMappingConfig.loadFromClasspath(
                    "nonexistent/mapping.properties");

            assertNotNull(config);
            assertEquals(0, config.entries().size());
        }
    }

    // ---- 5. External override ----

    @Nested
    @DisplayName("External override")
    class ExternalOverride {

        @Test
        @DisplayName("dwc.mapping system property merges external override")
        void systemPropertyOverrideMerges(@TempDir Path tempDir) throws IOException {
            // Write override mapping
            Path overrideFile = tempDir.resolve("override-mapping.properties");
            Files.writeString(overrideFile,
                    "--dwc-color-primary = color:OverriddenKey.background\n" +
                    "--dwc-custom-new = int:Custom.value\n");

            System.setProperty("dwc.mapping", overrideFile.toString());

            TokenMappingConfig config = TokenMappingConfig.loadDefault();

            // Find the overridden entry
            MappingEntry primaryEntry = config.entries().stream()
                    .filter(e -> e.cssTokenName().equals("--dwc-color-primary"))
                    .findFirst()
                    .orElse(null);

            assertNotNull(primaryEntry, "--dwc-color-primary should exist");
            // Should be overridden to OverriddenKey.background (not original multi-target)
            assertEquals(1, primaryEntry.targets().size());
            assertEquals("OverriddenKey.background", primaryEntry.targets().get(0).key());

            // New custom entry should also be present
            MappingEntry customEntry = config.entries().stream()
                    .filter(e -> e.cssTokenName().equals("--dwc-custom-new"))
                    .findFirst()
                    .orElse(null);

            assertNotNull(customEntry, "--dwc-custom-new should exist from override");
            assertEquals(MappingType.INT, customEntry.targets().get(0).type());
        }

        @Test
        @DisplayName("Missing external override file logs warning but does not throw")
        void missingOverrideNonFatal() {
            System.setProperty("dwc.mapping", "/nonexistent/path/mapping.properties");

            // Should NOT throw
            TokenMappingConfig config = assertDoesNotThrow(
                    TokenMappingConfig::loadDefault);

            // Bundled defaults should still be present
            assertNotNull(config);
            assertTrue(config.entries().size() > 10,
                    "Expected bundled entries despite missing override, got: "
                            + config.entries().size());
        }
    }
}
