package com.dwc.laf.css;

/**
 * Sealed interface representing typed CSS values extracted from custom properties.
 *
 * <p>Each record variant wraps a specific Java type suitable for Swing consumption.
 * Exhaustive pattern matching via {@code switch} is supported since this is sealed.</p>
 */
public sealed interface CssValue {

    /**
     * A resolved CSS color value, wrapping {@link java.awt.Color}.
     * Produced from hex, rgb/rgba, hsl/hsla, and named color strings.
     */
    record ColorValue(java.awt.Color color) implements CssValue {}

    /**
     * A CSS dimension value with a numeric component and unit string.
     * For example, {@code 0.875rem} becomes {@code DimensionValue(0.875f, "rem")}.
     */
    record DimensionValue(float value, String unit) implements CssValue {
        /**
         * Returns the value rounded to the nearest integer.
         * Useful for converting to pixel values expected by Swing UIDefaults.
         */
        public int intValue() {
            return Math.round(value);
        }
    }

    /**
     * A pure integer CSS value. Used for font weights (400, 700),
     * z-indexes, and other whole-number properties.
     */
    record IntegerValue(int value) implements CssValue {}

    /**
     * A unitless floating-point CSS value. Used for line-height (1.25),
     * opacity (0.6), and similar unitless numbers.
     */
    record FloatValue(float value) implements CssValue {}

    /**
     * A string CSS value. Used for font-family stacks, border-style names,
     * cursor values, and other non-numeric, non-color strings.
     */
    record StringValue(String value) implements CssValue {}

    /**
     * A raw CSS value that cannot be further typed. Used for calc() expressions,
     * complex shorthand values, and anything the type system cannot resolve.
     */
    record RawValue(String raw) implements CssValue {}
}
