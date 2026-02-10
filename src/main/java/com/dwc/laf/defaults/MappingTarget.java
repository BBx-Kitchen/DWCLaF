package com.dwc.laf.defaults;

/**
 * A single UIDefaults target: a key name and the type to convert the value to.
 *
 * <p>For example, {@code new MappingTarget("Button.background", MappingType.COLOR)}
 * means the CSS value should be converted to a {@link javax.swing.plaf.ColorUIResource}
 * and stored under the key "Button.background" in UIDefaults.</p>
 *
 * @param key  the UIDefaults key name (e.g., "Button.background")
 * @param type the mapping type that determines value conversion
 */
public record MappingTarget(String key, MappingType type) {

    /**
     * Creates a MappingTarget, validating that key is non-null and non-blank.
     */
    public MappingTarget {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("MappingTarget key must not be null or blank");
        }
        if (type == null) {
            throw new IllegalArgumentException("MappingTarget type must not be null");
        }
    }
}
