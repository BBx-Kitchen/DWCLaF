package com.dwc.laf.defaults;

/**
 * Enum of mapping target types for converting CSS values to UIDefaults values.
 *
 * <p>Each type determines how a {@link com.dwc.laf.css.CssValue} is converted
 * to a Java object suitable for placement in {@link javax.swing.UIDefaults}.</p>
 */
public enum MappingType {

    /** Map to {@link javax.swing.plaf.ColorUIResource}. */
    COLOR,

    /** Map to {@link Integer}. Dimension values are converted to pixels. */
    INT,

    /** Map to {@link Float}. */
    FLOAT,

    /** Map to {@link String}. */
    STRING,

    /** Map to {@link javax.swing.plaf.InsetsUIResource}. Reserved for future use. */
    INSETS,

    /** Auto-detect target type from the CssValue subtype. */
    AUTO;

    /**
     * Parses a type prefix string to a MappingType, case-insensitively.
     *
     * <p>Recognized prefixes: "color", "int", "float", "string", "insets", "auto".
     * Unknown strings default to {@link #AUTO}.</p>
     *
     * @param typeString the type prefix string (e.g., "color", "int")
     * @return the corresponding MappingType, or AUTO for unknown strings
     */
    public static MappingType fromString(String typeString) {
        if (typeString == null || typeString.isBlank()) {
            return AUTO;
        }
        return switch (typeString.strip().toLowerCase()) {
            case "color" -> COLOR;
            case "int" -> INT;
            case "float" -> FLOAT;
            case "string" -> STRING;
            case "insets" -> INSETS;
            case "auto" -> AUTO;
            default -> AUTO;
        };
    }
}
