package com.dwc.laf.css;

import java.util.Optional;

/**
 * Parses CSS dimension strings into typed {@link CssValue} instances.
 *
 * <p>Handles dimensions with units (px, rem, em, %), pure integers,
 * and unitless floats. Returns {@link Optional#empty()} for non-numeric
 * strings, calc() expressions, and other non-dimension values.</p>
 *
 * <p>The parser scans the input character by character to find the boundary
 * between the numeric part and the unit part. It returns:</p>
 * <ul>
 *   <li>{@link CssValue.DimensionValue} if a unit is present (px, rem, em, %, etc.)</li>
 *   <li>{@link CssValue.IntegerValue} if the value is a pure integer with no unit</li>
 *   <li>{@link CssValue.FloatValue} if the value is a unitless decimal number</li>
 *   <li>{@link Optional#empty()} for non-numeric strings, calc() expressions, etc.</li>
 * </ul>
 *
 * <p>Returns {@link Optional#empty()} for unrecognized input. Never throws.</p>
 */
public final class CssDimensionParser {

    private CssDimensionParser() {
        // utility class
    }

    /**
     * Parse a CSS dimension/number string into a typed {@link CssValue}.
     *
     * @param value the CSS value string (e.g., "16px", "0.875rem", "400", "1.25")
     * @return an Optional containing the parsed CssValue, or empty if not a dimension/number
     */
    public static Optional<CssValue> parse(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        String trimmed = value.trim();

        // Reject values that start with non-numeric, non-sign characters
        // (e.g., "solid", "inherit", "#ff0000", "calc(...)")
        char first = trimmed.charAt(0);
        if (!isNumericStart(first)) {
            return Optional.empty();
        }

        // Find the boundary between the numeric part and the unit part
        int unitStart = findUnitStart(trimmed);

        String numericPart = trimmed.substring(0, unitStart);
        String unitPart = trimmed.substring(unitStart);

        // Validate the numeric part can be parsed
        if (numericPart.isEmpty()) {
            return Optional.empty();
        }

        try {
            if (!unitPart.isEmpty()) {
                // Has a unit -> DimensionValue
                float numericValue = Float.parseFloat(numericPart);
                return Optional.of(new CssValue.DimensionValue(numericValue, unitPart));
            } else {
                // No unit -- determine if integer or float
                if (numericPart.contains(".")) {
                    float floatValue = Float.parseFloat(numericPart);
                    return Optional.of(new CssValue.FloatValue(floatValue));
                } else {
                    int intValue = Integer.parseInt(numericPart);
                    return Optional.of(new CssValue.IntegerValue(intValue));
                }
            }
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * Check if a character can start a numeric value (digit, minus, dot).
     */
    private static boolean isNumericStart(char c) {
        return c == '-' || c == '+' || c == '.' || (c >= '0' && c <= '9');
    }

    /**
     * Find the index where the unit part begins.
     * The numeric part may contain digits, a dot, minus/plus sign.
     * Everything after that is the unit.
     */
    private static int findUnitStart(String s) {
        int i = 0;
        int len = s.length();

        // Skip optional leading sign
        if (i < len && (s.charAt(i) == '-' || s.charAt(i) == '+')) {
            i++;
        }

        // Skip digits
        while (i < len && s.charAt(i) >= '0' && s.charAt(i) <= '9') {
            i++;
        }

        // Skip optional decimal point and following digits
        if (i < len && s.charAt(i) == '.') {
            i++;
            while (i < len && s.charAt(i) >= '0' && s.charAt(i) <= '9') {
                i++;
            }
        }

        return i;
    }
}
