package com.dwc.laf.css;

import java.awt.Color;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

/**
 * Immutable typed token map -- the public API for downstream consumers.
 *
 * <p>Wraps the typed token map produced by {@link CssValueTyper} and the
 * raw resolved string map for debugging. Provides typed accessors for
 * colors, integers, floats, and strings.</p>
 *
 * <p>Instances are created by {@link CssThemeLoader} (package-private constructor).
 * All methods are thread-safe (the underlying maps are immutable).</p>
 */
public final class CssTokenMap {

    private final Map<String, CssValue> tokens;
    private final Map<String, String> rawTokens;

    /**
     * Creates a new CssTokenMap. Package-private: only {@link CssThemeLoader} creates these.
     *
     * @param tokens    the typed token map (will be wrapped as unmodifiable if not already)
     * @param rawTokens the resolved string map for debugging (will be wrapped as unmodifiable if not already)
     */
    CssTokenMap(Map<String, CssValue> tokens, Map<String, String> rawTokens) {
        this.tokens = Collections.unmodifiableMap(tokens);
        this.rawTokens = Collections.unmodifiableMap(rawTokens);
    }

    // ---- Typed accessors ----

    /**
     * Returns the color value for the given property name, if it exists and is a {@link CssValue.ColorValue}.
     *
     * @param propertyName the CSS custom property name (e.g., "--dwc-color-primary-50")
     * @return the Color, or empty if the property is absent or not a color
     */
    public Optional<Color> getColor(String propertyName) {
        CssValue value = tokens.get(propertyName);
        if (value instanceof CssValue.ColorValue cv) {
            return Optional.of(cv.color());
        }
        return Optional.empty();
    }

    /**
     * Returns the color value for the given property name, or a default if absent/not-a-color.
     *
     * @param propertyName the CSS custom property name
     * @param defaultValue the default color to return if the property is missing or not a color
     * @return the Color value, or defaultValue
     */
    public Color getColor(String propertyName, Color defaultValue) {
        return getColor(propertyName).orElse(defaultValue);
    }

    /**
     * Returns the integer value for the given property name, if it exists and is an {@link CssValue.IntegerValue}.
     *
     * @param propertyName the CSS custom property name (e.g., "--dwc-font-weight-normal")
     * @return the int value, or empty if the property is absent or not an integer
     */
    public OptionalInt getInt(String propertyName) {
        CssValue value = tokens.get(propertyName);
        if (value instanceof CssValue.IntegerValue iv) {
            return OptionalInt.of(iv.value());
        }
        return OptionalInt.empty();
    }

    /**
     * Returns the integer value for the given property name, or a default if absent/not-an-integer.
     *
     * @param propertyName the CSS custom property name
     * @param defaultValue the default int to return if the property is missing or not an integer
     * @return the int value, or defaultValue
     */
    public int getInt(String propertyName, int defaultValue) {
        return getInt(propertyName).orElse(defaultValue);
    }

    /**
     * Returns the float value for the given property name, if it exists and is a {@link CssValue.FloatValue}.
     *
     * @param propertyName the CSS custom property name (e.g., "--dwc-opacity")
     * @return the Float value, or empty if the property is absent or not a float
     */
    public Optional<Float> getFloat(String propertyName) {
        CssValue value = tokens.get(propertyName);
        if (value instanceof CssValue.FloatValue fv) {
            return Optional.of(fv.value());
        }
        return Optional.empty();
    }

    /**
     * Returns the string value for the given property name, if it exists and is a {@link CssValue.StringValue}.
     *
     * @param propertyName the CSS custom property name (e.g., "--dwc-font-family-sans")
     * @return the String value, or empty if the property is absent or not a string
     */
    public Optional<String> getString(String propertyName) {
        CssValue value = tokens.get(propertyName);
        if (value instanceof CssValue.StringValue sv) {
            return Optional.of(sv.value());
        }
        return Optional.empty();
    }

    /**
     * Returns the typed CssValue for the given property name, regardless of type.
     *
     * @param propertyName the CSS custom property name
     * @return the CssValue, or empty if the property is absent
     */
    public Optional<CssValue> get(String propertyName) {
        return Optional.ofNullable(tokens.get(propertyName));
    }

    /**
     * Returns the resolved raw string value for the given property, for debugging.
     *
     * @param propertyName the CSS custom property name
     * @return the raw resolved string, or null if absent
     */
    public String getRaw(String propertyName) {
        return rawTokens.get(propertyName);
    }

    // ---- Bulk access ----

    /**
     * Returns the set of all property names in this token map.
     *
     * @return an unmodifiable set of property names
     */
    public Set<String> propertyNames() {
        return tokens.keySet();
    }

    /**
     * Returns the number of tokens in this map.
     *
     * @return the token count
     */
    public int size() {
        return tokens.size();
    }

    /**
     * Returns true if this map contains a token with the given property name.
     *
     * @param propertyName the CSS custom property name
     * @return true if present, false otherwise
     */
    public boolean contains(String propertyName) {
        return tokens.containsKey(propertyName);
    }

    @Override
    public String toString() {
        return "CssTokenMap{size=" + tokens.size() + "}";
    }
}
