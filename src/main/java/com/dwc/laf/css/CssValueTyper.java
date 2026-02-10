package com.dwc.laf.css;

import java.awt.Color;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Pass 3 of the CSS token pipeline: converts resolved string values into
 * typed {@link CssValue} records.
 *
 * <p>For each resolved token value, the typer attempts classification in order:</p>
 * <ol>
 *   <li>Color: via {@link CssColorParser#parse(String)}</li>
 *   <li>Dimension/number: via {@link CssDimensionParser#parse(String)}</li>
 *   <li>String: font-family stacks (comma-separated), CSS keywords (solid, none, etc.)</li>
 *   <li>Raw: calc() expressions and anything else that cannot be further typed</li>
 * </ol>
 *
 * <p>The returned map is immutable.</p>
 */
public final class CssValueTyper {

    private static final Logger LOG = Logger.getLogger(CssValueTyper.class.getName());

    /**
     * CSS keywords that should be typed as StringValue rather than RawValue.
     * These are common property values that downstream code may want to match on.
     */
    private static final Set<String> CSS_KEYWORDS = Set.of(
            "solid", "none", "inherit", "initial", "auto", "unset", "revert",
            "hidden", "dotted", "dashed", "double", "groove", "ridge", "inset", "outset",
            "normal", "italic", "oblique",
            "underline", "overline", "line-through",
            "pointer", "default", "crosshair", "text", "wait", "help", "move",
            "not-allowed", "grab", "grabbing", "col-resize", "row-resize",
            "n-resize", "s-resize", "e-resize", "w-resize",
            "bold", "bolder", "lighter",
            "block", "inline", "inline-block", "flex", "grid",
            "relative", "absolute", "fixed", "sticky", "static",
            "visible", "collapse", "scroll",
            "left", "right", "center", "justify",
            "top", "bottom", "middle",
            "nowrap", "wrap", "pre", "pre-wrap", "pre-line",
            "ease", "ease-in", "ease-out", "ease-in-out", "linear"
    );

    private CssValueTyper() {
        // utility class
    }

    /**
     * Converts a map of resolved string values into a map of typed {@link CssValue} records.
     *
     * @param resolvedTokens the resolved token map from {@link CssVariableResolver#resolve}
     * @return an unmodifiable map of property name to typed CssValue
     */
    public static Map<String, CssValue> type(Map<String, String> resolvedTokens) {
        if (resolvedTokens == null || resolvedTokens.isEmpty()) {
            return Map.of();
        }

        var typed = new LinkedHashMap<String, CssValue>(resolvedTokens.size());

        for (var entry : resolvedTokens.entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue();
            CssValue cssValue = classify(value);
            typed.put(name, cssValue);
        }

        return Collections.unmodifiableMap(typed);
    }

    /**
     * Classifies a single resolved CSS value string into the appropriate
     * {@link CssValue} variant.
     */
    private static CssValue classify(String value) {
        if (value == null || value.isEmpty()) {
            return new CssValue.RawValue(value == null ? "" : value);
        }

        // 1. Check for unresolvable functions first (calc, env, etc.)
        //    These must be caught before dimension/color parsing since they
        //    may start with numeric characters
        if (containsFunction(value)) {
            return new CssValue.RawValue(value);
        }

        // 2. Check for multi-value / string-type patterns early.
        //    Comma-separated values (font stacks, shadow lists) and CSS keywords
        //    must be caught before dimension parsing since values like
        //    "0 2px 4px rgba(...), ..." start with a digit.
        if (isStringValue(value)) {
            return new CssValue.StringValue(value);
        }

        // 3. Try color parsing
        Optional<Color> color = CssColorParser.parse(value);
        if (color.isPresent()) {
            return new CssValue.ColorValue(color.get());
        }

        // 4. Try dimension/number parsing (only for simple single-token values)
        Optional<CssValue> dimension = CssDimensionParser.parse(value);
        if (dimension.isPresent()) {
            return dimension.get();
        }

        // 5. Fallback: RawValue
        return new CssValue.RawValue(value);
    }

    /**
     * Determines if a value should be typed as a StringValue.
     *
     * <p>String values include:</p>
     * <ul>
     *   <li>Font-family stacks: comma-separated names at top level</li>
     *   <li>CSS keywords: solid, none, inherit, initial, auto, etc.</li>
     * </ul>
     */
    private static boolean isStringValue(String value) {
        // Font-family stacks and multi-value shorthands contain commas at the
        // top level (outside parentheses). We must NOT flag hsl(211, 100%, 50%)
        // or rgb(255, 0, 0) since those commas are inside function calls.
        if (hasTopLevelComma(value)) {
            return true;
        }

        // Check against known CSS keywords (case-insensitive)
        String lower = value.toLowerCase(Locale.ROOT).trim();
        return CSS_KEYWORDS.contains(lower);
    }

    /**
     * Checks whether the value contains a comma at the top level (outside
     * any parenthesized function calls). For example:
     * <ul>
     *   <li>{@code "-apple-system, sans-serif"} -> true (comma at top level)</li>
     *   <li>{@code "hsl(211, 100%, 50%)"} -> false (commas inside parens)</li>
     *   <li>{@code "0 2px rgba(0,0,0,0.1), 0 4px rgba(0,0,0,0.05)"} -> true</li>
     * </ul>
     */
    private static boolean hasTopLevelComma(String value) {
        int depth = 0;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth < 0) depth = 0;
            } else if (c == ',' && depth == 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the value contains a CSS function that cannot be further resolved,
     * such as calc(), env(), or min()/max()/clamp().
     */
    private static boolean containsFunction(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.contains("calc(")
                || lower.contains("env(")
                || lower.contains("min(")
                || lower.contains("max(")
                || lower.contains("clamp(");
    }
}
