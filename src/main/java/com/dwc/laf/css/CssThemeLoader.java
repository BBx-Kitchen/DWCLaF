package com.dwc.laf.css;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Main entry point for the CSS token engine.
 *
 * <p>Orchestrates the full pipeline: load CSS text -> parse raw tokens ->
 * merge override layer -> resolve var() references -> type values ->
 * wrap in {@link CssTokenMap}.</p>
 *
 * <p>Supports two layers: bundled defaults (from classpath) and one optional
 * external override (from file path specified via system property
 * {@code dwc.theme}). External override tokens replace matching keys;
 * bundled tokens provide defaults.</p>
 *
 * <p>Theme loading is startup-only. The returned {@link CssTokenMap} is
 * immutable and can be shared across threads.</p>
 */
public final class CssThemeLoader {

    private static final Logger LOG = Logger.getLogger(CssThemeLoader.class.getName());

    /**
     * Classpath resource path for the bundled default light theme.
     */
    private static final String DEFAULT_THEME_RESOURCE = "com/dwc/laf/themes/default-light.css";

    /**
     * System property name for specifying external CSS override file path.
     */
    private static final String OVERRIDE_SYSTEM_PROPERTY = "dwc.theme";

    private CssThemeLoader() {
        // utility class
    }

    /**
     * Load the complete theme: bundled defaults + optional external override.
     *
     * <p>External override path is read from the system property
     * {@code dwc.theme}. If set, the file at that path is parsed and its
     * tokens are merged on top of the bundled defaults (override wins for
     * matching keys). If the override file cannot be read, a warning is
     * logged and the bundled defaults are used alone.</p>
     *
     * @return an immutable {@link CssTokenMap} with all tokens resolved and typed
     */
    public static CssTokenMap load() {
        // 1. Load bundled default CSS from classpath
        String bundledCss = loadResource(DEFAULT_THEME_RESOURCE);
        if (bundledCss == null) {
            LOG.warning("Bundled default theme not found: " + DEFAULT_THEME_RESOURCE);
            bundledCss = "";
        }
        Map<String, String> rawTokens = CssTokenParser.parse(bundledCss);

        // 2. Check for external override via system property
        String overridePath = System.getProperty(OVERRIDE_SYSTEM_PROPERTY);
        if (overridePath != null && !overridePath.isBlank()) {
            try {
                String overrideCss = Files.readString(Path.of(overridePath), StandardCharsets.UTF_8);
                Map<String, String> overrideTokens = CssTokenParser.parse(overrideCss);
                // Merge: override tokens replace matching bundled tokens
                Map<String, String> merged = new LinkedHashMap<>(rawTokens);
                merged.putAll(overrideTokens);
                rawTokens = merged;
                LOG.info("Loaded override theme from: " + overridePath);
            } catch (IOException e) {
                LOG.warning("Failed to load override theme from: " + overridePath
                        + " - " + e.getMessage());
            }
        }

        // 3. Resolve var() references
        Map<String, String> resolved = CssVariableResolver.resolve(rawTokens);

        // 4. Type the resolved values
        Map<String, CssValue> typed = CssValueTyper.type(resolved);

        // 5. Wrap in CssTokenMap
        return new CssTokenMap(typed, resolved);
    }

    /**
     * Load theme from a specific classpath resource.
     *
     * <p>Useful for testing or loading alternative bundled themes.
     * No external override is applied.</p>
     *
     * @param resourcePath the classpath resource path (e.g., "com/dwc/laf/themes/default-light.css")
     * @return an immutable {@link CssTokenMap}, or an empty map if the resource is not found
     */
    public static CssTokenMap loadFromClasspath(String resourcePath) {
        String css = loadResource(resourcePath);
        if (css == null) {
            LOG.warning("Classpath resource not found: " + resourcePath);
            return new CssTokenMap(Map.of(), Map.of());
        }
        return buildTokenMap(css);
    }

    /**
     * Load theme from a specific file path.
     *
     * <p>No external override is applied -- the file is the complete theme.</p>
     *
     * @param filePath the path to the CSS file
     * @return an immutable {@link CssTokenMap}
     * @throws IOException if the file cannot be read
     */
    public static CssTokenMap loadFromFile(Path filePath) throws IOException {
        String css = Files.readString(filePath, StandardCharsets.UTF_8);
        return buildTokenMap(css);
    }

    /**
     * Load theme from CSS text directly.
     *
     * <p>Useful for testing. Runs the full parse-resolve-type pipeline.</p>
     *
     * @param cssText the CSS text to parse
     * @return an immutable {@link CssTokenMap}
     */
    public static CssTokenMap loadFromString(String cssText) {
        return buildTokenMap(cssText);
    }

    /**
     * Internal pipeline: parse -> resolve -> type -> wrap.
     */
    private static CssTokenMap buildTokenMap(String cssText) {
        Map<String, String> rawTokens = CssTokenParser.parse(cssText);
        Map<String, String> resolved = CssVariableResolver.resolve(rawTokens);
        Map<String, CssValue> typed = CssValueTyper.type(resolved);
        return new CssTokenMap(typed, resolved);
    }

    /**
     * Load a classpath resource as a String.
     *
     * @param resourcePath the classpath resource path
     * @return the resource content as a string, or null if not found
     */
    private static String loadResource(String resourcePath) {
        // Try context classloader first (works in application server environments)
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = CssThemeLoader.class.getClassLoader();
        }

        try (InputStream is = cl.getResourceAsStream(resourcePath)) {
            if (is == null) {
                return null;
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.warning("Error reading classpath resource: " + resourcePath + " - " + e.getMessage());
            return null;
        }
    }
}
