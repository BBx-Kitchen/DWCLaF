package com.dwc.laf.css;

import java.util.Optional;

/**
 * Parses CSS dimension strings into typed {@link CssValue} instances.
 *
 * <p>Handles dimensions with units (px, rem, em, %), pure integers,
 * and unitless floats. Returns {@link Optional#empty()} for non-numeric
 * strings, calc() expressions, and other non-dimension values.</p>
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
        // Stub -- tests should fail
        return Optional.empty();
    }
}
