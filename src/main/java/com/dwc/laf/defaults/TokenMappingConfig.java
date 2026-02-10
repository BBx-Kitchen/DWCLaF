package com.dwc.laf.defaults;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Parses token-mapping properties files that define how CSS tokens map to UIDefaults keys.
 *
 * <p>The properties file format is:</p>
 * <pre>
 *   --css-token-name = type:UIDefaults.key, type:UIDefaults.key, ...
 * </pre>
 *
 * <p>Where {@code type} is one of: color, int, float, string, insets, auto.
 * If the type prefix and colon are omitted, AUTO is assumed.</p>
 *
 * <p>Supports loading from classpath (bundled defaults) with an optional external
 * override via the system property {@code dwc.mapping}. External entries merge on
 * top of bundled entries (external wins for matching CSS token names).</p>
 *
 * <p>Instances are immutable after construction.</p>
 */
public final class TokenMappingConfig {

    private static final Logger LOG = Logger.getLogger(TokenMappingConfig.class.getName());

    /**
     * Classpath resource path for the bundled default token mapping.
     */
    private static final String DEFAULT_MAPPING_RESOURCE = "com/dwc/laf/token-mapping.properties";

    /**
     * System property for specifying an external token-mapping override file.
     */
    private static final String OVERRIDE_SYSTEM_PROPERTY = "dwc.mapping";

    private final List<MappingEntry> entries;

    private TokenMappingConfig(List<MappingEntry> entries) {
        this.entries = Collections.unmodifiableList(List.copyOf(entries));
    }

    /**
     * Returns the immutable list of mapping entries.
     *
     * @return the mapping entries
     */
    public List<MappingEntry> entries() {
        return entries;
    }

    /**
     * Loads the default token mapping from the bundled classpath resource,
     * then merges any external override specified via the {@code dwc.mapping}
     * system property.
     *
     * <p>If the external override file does not exist, a warning is logged
     * and the bundled defaults are used alone.</p>
     *
     * @return the token mapping configuration
     */
    public static TokenMappingConfig loadDefault() {
        // Load bundled defaults from classpath
        Properties bundled = loadPropertiesFromClasspath(DEFAULT_MAPPING_RESOURCE);
        if (bundled == null) {
            LOG.warning("Bundled token mapping not found: " + DEFAULT_MAPPING_RESOURCE);
            bundled = new Properties();
        }

        // Check for external override
        String overridePath = System.getProperty(OVERRIDE_SYSTEM_PROPERTY);
        if (overridePath != null && !overridePath.isBlank()) {
            Path path = Path.of(overridePath);
            if (Files.exists(path)) {
                try (InputStream is = new FileInputStream(path.toFile())) {
                    Properties override = new Properties();
                    override.load(is);
                    // Merge: external keys override bundled keys
                    for (String name : override.stringPropertyNames()) {
                        bundled.setProperty(name, override.getProperty(name));
                    }
                    LOG.info("Loaded token mapping override from: " + overridePath);
                } catch (IOException e) {
                    LOG.warning("Failed to load token mapping override from: " + overridePath
                            + " - " + e.getMessage());
                }
            } else {
                LOG.warning("External token mapping file not found: " + overridePath);
            }
        }

        return loadFromProperties(bundled);
    }

    /**
     * Loads a token mapping from a specific classpath resource.
     * No external override is applied.
     *
     * @param resourcePath the classpath resource path
     * @return the token mapping configuration, or empty config if resource not found
     */
    public static TokenMappingConfig loadFromClasspath(String resourcePath) {
        Properties props = loadPropertiesFromClasspath(resourcePath);
        if (props == null) {
            LOG.warning("Classpath resource not found: " + resourcePath);
            return new TokenMappingConfig(List.of());
        }
        return loadFromProperties(props);
    }

    /**
     * Parses a Properties object into a TokenMappingConfig.
     *
     * <p>Each property name is the CSS token name (starting with {@code --}).
     * Each property value is a comma-separated list of targets, where each
     * target is optionally prefixed with {@code type:}.</p>
     *
     * @param props the Properties to parse
     * @return the token mapping configuration
     */
    public static TokenMappingConfig loadFromProperties(Properties props) {
        return new TokenMappingConfig(parse(props));
    }

    /**
     * Parses properties into a list of MappingEntry objects.
     */
    private static List<MappingEntry> parse(Properties props) {
        // Use a LinkedHashMap to maintain order and handle merges (same CSS token name)
        Map<String, MappingEntry> entryMap = new LinkedHashMap<>();

        for (String cssTokenName : props.stringPropertyNames()) {
            String value = props.getProperty(cssTokenName);
            if (value == null || value.isBlank()) {
                continue;
            }

            String[] segments = value.split(",");
            List<MappingTarget> targets = new ArrayList<>();

            for (String segment : segments) {
                String trimmed = segment.strip();
                if (trimmed.isEmpty()) {
                    continue;
                }

                // Split on first colon to separate type prefix from key
                int colonIndex = trimmed.indexOf(':');
                MappingType type;
                String key;

                if (colonIndex >= 0) {
                    String typeStr = trimmed.substring(0, colonIndex).strip();
                    key = trimmed.substring(colonIndex + 1).strip();
                    type = MappingType.fromString(typeStr);
                } else {
                    // No colon -- entire segment is the key, type is AUTO
                    key = trimmed;
                    type = MappingType.AUTO;
                }

                if (!key.isEmpty()) {
                    targets.add(new MappingTarget(key, type));
                }
            }

            if (!targets.isEmpty()) {
                entryMap.put(cssTokenName, new MappingEntry(cssTokenName, targets));
            }
        }

        return new ArrayList<>(entryMap.values());
    }

    /**
     * Loads a Properties object from a classpath resource.
     * Uses the same classloader strategy as CssThemeLoader.
     *
     * @return the loaded Properties, or null if the resource is not found
     */
    private static Properties loadPropertiesFromClasspath(String resourcePath) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = TokenMappingConfig.class.getClassLoader();
        }

        try (InputStream is = cl.getResourceAsStream(resourcePath)) {
            if (is == null) {
                return null;
            }
            Properties props = new Properties();
            props.load(is);
            return props;
        } catch (IOException e) {
            LOG.warning("Error reading classpath resource: " + resourcePath + " - " + e.getMessage());
            return null;
        }
    }
}
